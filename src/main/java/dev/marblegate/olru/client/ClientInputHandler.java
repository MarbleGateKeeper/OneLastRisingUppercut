package dev.marblegate.olru.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.marblegate.olru.client.movement.ClientMovementInteractionState;
import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.movement.task.ClientMovementRuntimeData;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import dev.marblegate.olru.network.payload.ServerboundMovementTaskActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OneLastRisingUppercut.MODID, value = Dist.CLIENT)
public class ClientInputHandler {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "category"));
    public static final KeyMapping SKILL_TWO_KEY = new KeyMapping("key.olru.skill_two", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY);
    public static final KeyMapping SKILL_THREE_KEY = new KeyMapping("key.olru.skill_three", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ULTIMATE_KEY = new KeyMapping("key.olru.ultimate", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    private static boolean wasJumpDownForMovement = false;
    private static int continuousAttackTicks = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.screen != null) {
            continuousAttackTicks = 0;
            drainGauntletSkillClicks();
            return;
        }

        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem gauntlet)) {
            continuousAttackTicks = 0;
            drainGauntletSkillClicks();
            return;
        }

        if (consumeAnyClick(SKILL_TWO_KEY)) {
            ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.skillTwo());
        }

        if (consumeAnyClick(SKILL_THREE_KEY)) {
            ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.skillThree());
        }

        if (consumeAnyClick(ULTIMATE_KEY)) {
            ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.ultimate());
        }

        // Hold-to-channel normal attack (The Final Answer's Biotic Spray): re-fire while LMB is held.
        GauntletSkillGroup group = gauntlet.getSyncedSkillGroup(mc.player);
        if (gauntlet.isNormalAttackContinuous()
                && group != null
                && group.get(SkillType.NORMAL_ATTACK).isUsable()
                && mc.options.keyAttack.isDown()) {
            if (++continuousAttackTicks >= Math.max(1, OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY.pulseIntervalTicks.get())) {
                continuousAttackTicks = 0;
                ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.normalAttack());
            }
        } else {
            continuousAttackTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null
                || !(event.getEntity().getMainHandItem().getItem() instanceof AbstractGauntletItem)
                || !isGauntletSkillBoundToSneak(mc)) {
            return;
        }

        releaseSneakKey(mc);

        Input keyPresses = event.getInput().keyPresses;
        if (keyPresses.shift()) {
            event.getInput().keyPresses = new Input(keyPresses.forward(), keyPresses.backward(), keyPresses.left(), keyPresses.right(), keyPresses.jump(), false, keyPresses.sprint());
        }
    }

    public static void dispatchMovementRuntimeInput(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            wasJumpDownForMovement = false;
            return;
        }

        boolean jumpDown = mc.options.keyJump.isDown();
        if (ClientMovementManager.hasTask() && jumpDown && !wasJumpDownForMovement) {
            ClientMovementManager.submitRuntimeData(ClientMovementRuntimeData.jumpPressed());
        }
        wasJumpDownForMovement = jumpDown;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientMovementInteractionState.isMeteorStrikeHoverActive()) {
            event.setCanceled(true);
            ClientPacketDistributor.sendToServer(ServerboundMovementTaskActionPayload.primaryAttackPressed());
            return;
        }

        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem)) return;

        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.normalAttack());
    }

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(SKILL_TWO_KEY);
        event.register(SKILL_THREE_KEY);
        event.register(ULTIMATE_KEY);
    }

    private static void drainGauntletSkillClicks() {
        consumeAnyClick(SKILL_TWO_KEY);
        consumeAnyClick(SKILL_THREE_KEY);
        consumeAnyClick(ULTIMATE_KEY);
    }

    private static boolean isGauntletSkillBoundToSneak(Minecraft mc) {
        KeyMapping sneakKey = mc.options.keyShift;
        return hasSameBinding(mc.options.keyAttack, sneakKey)
                || hasSameBinding(mc.options.keyUse, sneakKey)
                || hasSameBinding(SKILL_TWO_KEY, sneakKey)
                || hasSameBinding(SKILL_THREE_KEY, sneakKey)
                || hasSameBinding(ULTIMATE_KEY, sneakKey);
    }

    private static boolean hasSameBinding(KeyMapping skillKey, KeyMapping sneakKey) {
        return !skillKey.isUnbound()
                && skillKey.getKey().equals(sneakKey.getKey())
                && skillKey.getKeyModifier() == sneakKey.getKeyModifier();
    }

    private static void releaseSneakKey(Minecraft mc) {
        KeyMapping sneakKey = mc.options.keyShift;
        if (mc.options.toggleCrouch().get()) {
            if (sneakKey.isDown()) {
                // ToggleKeyMapping ignores setDown(false) in toggle mode; another press flips it off.
                sneakKey.setDown(true);
            }
        } else {
            sneakKey.setDown(false);
        }
    }

    private static boolean consumeAnyClick(KeyMapping mapping) {
        boolean consumed = false;
        while (mapping.consumeClick()) {
            consumed = true;
        }
        return consumed;
    }
}
