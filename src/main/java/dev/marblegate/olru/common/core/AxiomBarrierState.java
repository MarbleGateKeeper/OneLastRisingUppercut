package dev.marblegate.olru.common.core;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.attachment.GauntletEntityState;
import dev.marblegate.olru.common.attachment.GauntletEntityState.GroupAccess;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.ResourceCooldownState;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.common.registry.OLRUItems;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Accessor for The Axiom's Experimental Barrier state, stored as the SKILL_ONE
 * {@link ResourceCooldownState}: the progress is the durability fraction (0..1 of max) and the
 * cooldown is the recall/broken redeploy block. Reads never create state; writes go through
 * {@code setData} so the change syncs to the client.
 */
public final class AxiomBarrierState {
    private static final Identifier AXIOM = Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "the_axiom");

    private AxiomBarrierState() {}

    /** Durability fraction (0..1); a player who never touched The Axiom is treated as full. */
    public static float getDurabilityFraction(ServerPlayer player) {
        ResourceCooldownState barrier = getState(player);
        return barrier == null ? 1f : barrier.getProgress();
    }

    /** True while a recall or broken cooldown is still ticking down. */
    public static boolean isCooldownActive(ServerPlayer player) {
        ResourceCooldownState barrier = getState(player);
        return barrier != null && barrier.getCdRemaining() > 0;
    }

    /** Adds a signed fraction delta (clamped 0..1) and syncs; returns the new fraction. */
    public static float addDurability(ServerPlayer player, float delta) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GroupAccess access = state.getOrCreate(AXIOM, () -> OLRUItems.THE_AXIOM.get().createDefaultSkillGroup());
        if (!(access.group().get(SkillType.SKILL_ONE) instanceof ResourceCooldownState barrier)) return 0f;
        float before = barrier.getProgress();
        barrier.addProgress(delta);
        if (access.wasCreated() || barrier.getProgress() != before) {
            player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
        }
        return barrier.getProgress();
    }

    /** Drains {@code damage} points of durability (converted to a fraction of max) and syncs. */
    public static float drainDurability(ServerPlayer player, float damage) {
        float max = (float) OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER.maxDurability.getAsDouble();
        return max <= 0f ? 0f : addDurability(player, -damage / max);
    }

    /** Starts the shorter recall cooldown (capped at the broken cooldown) and syncs. */
    public static void startRecallCooldown(ServerPlayer player, int ticks) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GroupAccess access = state.getOrCreate(AXIOM, () -> OLRUItems.THE_AXIOM.get().createDefaultSkillGroup());
        if (!(access.group().get(SkillType.SKILL_ONE) instanceof ResourceCooldownState barrier)) return;
        barrier.consume(ticks);
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
    }

    /** Starts the full broken cooldown after a shatter and syncs. */
    public static void startBrokenCooldown(ServerPlayer player) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GroupAccess access = state.getOrCreate(AXIOM, () -> OLRUItems.THE_AXIOM.get().createDefaultSkillGroup());
        if (!(access.group().get(SkillType.SKILL_ONE) instanceof ResourceCooldownState barrier)) return;
        barrier.consume();
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
    }

    private static @Nullable ResourceCooldownState getState(ServerPlayer player) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GauntletSkillGroup group = state.get(AXIOM);
        if (group == null || !(group.get(SkillType.SKILL_ONE) instanceof ResourceCooldownState barrier)) return null;
        return barrier;
    }
}
