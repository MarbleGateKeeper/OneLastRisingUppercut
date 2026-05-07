package dev.marblegate.olru.common.core;

import dev.marblegate.olru.common.attachment.GauntletEntityState;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.ConditionalChargeState;
import dev.marblegate.olru.common.attachment.skill.SkillState;
import dev.marblegate.olru.common.item.LegacyOfHorusGauntletItem;
import dev.marblegate.olru.common.item.LegacyPrimeGauntletItem;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;

public class GauntletEventHandlers {
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (player.getMainHandItem().getItem() instanceof LegacyPrimeGauntletItem gauntlet) {
            addLegacyPrimeMeteorCharge(player, gauntlet, event.getNewDamage());
            return;
        }
        if (player.getMainHandItem().getItem() instanceof LegacyOfHorusGauntletItem) {
            if (event.getEntity() == player || GauntletHelper.isFriendly(player, event.getEntity())) return;
            addHorusNanoCharge(
                    player,
                    event.getNewDamage() * (float) (OLRUConfig.HORUS.NANO_SURGE.chargePercentPerDamage.get() / 100.0));
        }
    }

    private static void addLegacyPrimeMeteorCharge(ServerPlayer player, LegacyPrimeGauntletItem gauntlet, float damage) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GauntletSkillGroup group = state.get(gauntlet.gauntletId());
        if (group == null) return;

        SkillState ultimateState = group.get(SkillType.ULTIMATE);
        if (!(ultimateState instanceof ConditionalChargeState ccs)) return;
        if (ccs.isUsable()) return;

        float progress = damage * (float) (OLRUConfig.LEGACY_PRIME.METEOR_STRIKE.chargePercentPerDamage.get() / 100.0);
        ccs.addProgress(progress);
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
    }

    public static void addHorusNanoCharge(ServerPlayer player, float progress) {
        if (progress <= 0f) return;
        if (!(player.getMainHandItem().getItem() instanceof LegacyOfHorusGauntletItem gauntlet)) return;

        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GauntletSkillGroup group = state.getOrCreate(
                gauntlet.gauntletId(), gauntlet::createDefaultSkillGroup).group();

        SkillState ultimateState = group.get(SkillType.ULTIMATE);
        if (!(ultimateState instanceof ConditionalChargeState ccs)) return;
        if (ccs.isUsable()) return;

        ccs.addProgress(progress);
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
    }

    public static void spawnBulletTrail(ServerPlayer shooter,
            @Nullable Vec3 hitCenter, double range, int impactCount) {
        ServerLevel level = shooter.level();
        Vec3 origin = shooter.getEyePosition();
        Vec3 dir = shooter.getLookAngle();
        Vec3 endpoint = hitCenter != null ? hitCenter : origin.add(dir.scale(range));
        double dist = origin.distanceTo(endpoint);

        // Trail: one CRIT particle every 0.6 blocks along the ray
        for (double d = 0.5; d <= dist; d += 0.6) {
            Vec3 p = origin.add(dir.scale(d));
            // count=1 with tiny spread and speed=0 places a particle at the exact position
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }

        // Impact burst at the hit point (skipped on miss)
        if (hitCenter != null && impactCount > 0) {
            level.sendParticles(ParticleTypes.CRIT,
                    hitCenter.x, hitCenter.y, hitCenter.z,
                    impactCount, 0.25, 0.25, 0.25, 0.1);
        }
    }
}
