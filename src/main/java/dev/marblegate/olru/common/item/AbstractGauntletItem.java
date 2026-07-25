package dev.marblegate.olru.common.item;

import dev.marblegate.olru.common.attachment.GauntletEntityState;
import dev.marblegate.olru.common.attachment.GauntletEntityState.GroupAccess;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.network.payload.ServerboundGauntletChargeReleasePayload;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractGauntletItem extends Item {
    public AbstractGauntletItem(Properties properties) {
        super(properties);
    }

    public abstract Identifier gauntletId();

    public abstract GauntletSkillGroup createDefaultSkillGroup();

    // Item hooks

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!isSkillReady(player, SkillType.SKILL_ONE)) return InteractionResult.PASS;
        if (!level.isClientSide() && player.isUsingItem()) {
            // Heal a stuck server-side charge left by a client/server readiness desync:
            // a new use packet means the client started a fresh charge, so restart cleanly.
            player.stopUsingItem();
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        entity.setNoGravity(false);
        if (level.isClientSide()) {
            int ticksHeld = getUseDuration(stack, entity) - timeCharged;
            float chargePercent = Math.min(1f, (float) ticksHeld / getMaxChargeTicks());
            ClientPacketDistributor.sendToServer(new ServerboundGauntletChargeReleasePayload(chargePercent));
            return false;
        }
        return true;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        entity.setNoGravity(false);
    }

    /**
     * Server-side entry for {@link ServerboundGauntletChargeReleasePayload}. When the server shares
     * the vanilla use state, the charge is recomputed from server-side timing; otherwise (the server
     * never started the charge due to a readiness desync) the client-reported charge is used.
     */
    public void handleChargeRelease(ServerPlayer player, float clientChargePercent) {
        float chargePercent = Math.clamp(clientChargePercent, 0f, 1f);
        if (player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItem().getItem() == this) {
            int heldTicks = getUseDuration(player.getUseItem(), player) - player.getUseItemRemainingTicks();
            chargePercent = Math.min(1f, (float) heldTicks / getMaxChargeTicks());
            player.stopUsingItem();
        }
        performSkillOne(player, chargePercent);
    }

    // Skill entry points

    public abstract void performSkillOne(ServerPlayer player, float chargePercent);

    public abstract void performSkillTwo(ServerPlayer player);

    public abstract void performSkillThree(ServerPlayer player);

    public abstract void performUltimate(ServerPlayer player);

    public abstract void performNormalAttack(ServerPlayer player);

    public abstract int getMaxChargeTicks();

    public GauntletSkillGroup getSkillGroup(Player player) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GroupAccess access = state.getOrCreate(gauntletId(), this::createDefaultSkillGroup);
        if (access.wasCreated() && player instanceof ServerPlayer sp) {
            sp.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
        }
        return access.group();
    }

    public @Nullable GauntletSkillGroup getSyncedSkillGroup(Player player) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        if (state.get(gauntletId()) == null) return null;
        return state.getOrCreate(gauntletId(), this::createDefaultSkillGroup).group();
    }

    protected boolean isSkillReady(Player player, SkillType type) {
        return getSkillGroup(player).get(type).isUsable();
    }

    protected void consumeSkill(ServerPlayer player, SkillType type) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        state.getOrCreate(gauntletId(), this::createDefaultSkillGroup).group().get(type).consume();
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
    }
}
