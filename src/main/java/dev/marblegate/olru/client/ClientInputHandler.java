package dev.marblegate.olru.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.marblegate.olru.client.movement.ClientMovementInteractionState;
import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.movement.task.ClientMovementRuntimeData;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload;
import dev.marblegate.olru.network.payload.ServerboundMovementTaskActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OneLastRisingUppercut.MODID, value = Dist.CLIENT)
public class ClientInputHandler {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "category"));
    public static final KeyMapping ULTIMATE_KEY = new KeyMapping("key.olru.ultimate", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    private static boolean wasShiftDown = false;
    private static boolean wasJumpDownForMovement = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        boolean shiftClick = mc.options.keyShift.consumeClick();
        boolean ultimateClick = ULTIMATE_KEY.consumeClick();

        if (mc.player == null || mc.level == null || mc.screen != null) {
            wasShiftDown = false;
            return;
        }

        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem)) {
            wasShiftDown = false;
            return;
        }

        if (shiftClick && !wasShiftDown) {
            ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.skillTwo());
        }
        wasShiftDown = shiftClick;

        if (ultimateClick) {
            ClientPacketDistributor.sendToServer(ServerboundGauntletSkillPayload.ultimate());
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
        event.register(ULTIMATE_KEY);
    }
}
