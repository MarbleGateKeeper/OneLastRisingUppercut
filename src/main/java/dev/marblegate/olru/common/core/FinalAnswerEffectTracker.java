package dev.marblegate.olru.common.core;

import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.CoalescenceConfig;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-side tracker for The Final Answer timed effects: Fade and Coalescence. */
public class FinalAnswerEffectTracker {
    private static final Map<UUID, FadeState> FADING_PLAYERS = new HashMap<>();
    private static final Map<UUID, CoalescenceState> COALESCING = new HashMap<>();

    public static void startFade(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        FADING_PLAYERS.put(player.getUUID(), new FadeState(ticks, player.isInvisible()));
        player.setInvisible(true);
        GauntletEffectBroadcaster.fadeVignette(player, ticks);
    }

    public static boolean isFading(LivingEntity entity) {
        return FADING_PLAYERS.containsKey(entity.getUUID());
    }

    public static void startCoalescence(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        COALESCING.put(player.getUUID(), new CoalescenceState(ticks));
    }

    public static boolean isCoalescing(LivingEntity entity) {
        return COALESCING.containsKey(entity.getUUID());
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity instanceof ServerPlayer player) {
            tickFade(player);
            tickCoalescence(player);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        cleanup(entity);
    }

    public static void onEntityDeath(LivingDeathEvent event) {
        cleanup(event.getEntity());
    }

    private static void tickFade(ServerPlayer player) {
        FadeState state = FADING_PLAYERS.get(player.getUUID());
        if (state == null) return;

        if (!player.isAlive()) {
            endFade(player, state);
            return;
        }

        var cfg = OLRUConfig.FINAL_ANSWER.FADE;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, cfg.speedAmplifier.get(), false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 10, cfg.jumpBoostAmplifier.get(), false, false, false));
        player.invulnerableTime = 20;
        if (--state.ticksRemaining <= 0) {
            endFade(player, state);
        }
    }

    private static void endFade(ServerPlayer player, FadeState state) {
        FADING_PLAYERS.remove(player.getUUID());
        // Momentum is preserved: only invisibility is restored, velocity is never touched.
        player.setInvisible(state.wasInvisible);
        GauntletEffectBroadcaster.stopFadeVignette(player);
    }

    private static void cleanup(Entity entity) {
        FadeState state = FADING_PLAYERS.remove(entity.getUUID());
        if (state != null && entity instanceof LivingEntity living) {
            living.setInvisible(state.wasInvisible);
        }
        if (state != null && entity instanceof ServerPlayer player) {
            GauntletEffectBroadcaster.stopFadeVignette(player);
        }
        if (COALESCING.remove(entity.getUUID()) != null && entity instanceof ServerPlayer player) {
            GauntletEffectBroadcaster.stopCoalescenceBeam(player);
            GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.COALESCENCE_CHANNEL);
        }
    }

    private static void tickCoalescence(ServerPlayer player) {
        CoalescenceState state = COALESCING.get(player.getUUID());
        if (state == null) return;

        if (!player.isAlive()) {
            endCoalescence(player);
            return;
        }

        var cfg = OLRUConfig.FINAL_ANSWER.COALESCENCE;
        ServerLevel level = player.level();
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, cfg.speedAmplifier.get(), false, false, false));

        // Beam starts slightly forward and below the eyes; kept in sync with the client renderer.
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(direction.scale(0.5)).subtract(0, 0.45, 0);
        double length = cfg.length.get();
        double radius = cfg.radius.get();

        if (state.age % cfg.pulseIntervalTicks.get() == 0) {
            applyCoalescencePulse(player, level, cfg, origin, direction, length, radius);
        }
        if (state.age % 2 == 0) {
            GauntletParticleHelper.coalescenceBeam(level, origin, direction, length);
        }
        GauntletEffectBroadcaster.coalescenceBeam(player, 4);
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.COALESCENCE_CHANNEL, 0, 4);
        if (state.age % 10 == 0) {
            GauntletSoundHelper.coalescenceLoop(level, player.position());
        }

        state.age++;
        if (--state.ticksRemaining <= 0) {
            endCoalescence(player);
        }
    }

    private static void applyCoalescencePulse(
            ServerPlayer player, ServerLevel level, CoalescenceConfig cfg,
            Vec3 origin, Vec3 direction, double length, double radius) {
        float healedTotal = 0f;
        float beforeSelf = player.getHealth();
        player.heal((float) cfg.selfHealPerPulse.getAsDouble());
        healedTotal += Math.max(0f, player.getHealth() - beforeSelf);

        Vec3 end = origin.add(direction.scale(length));
        AABB searchBox = new AABB(
                Math.min(origin.x, end.x) - radius, Math.min(origin.y, end.y) - radius, Math.min(origin.z, end.z) - radius,
                Math.max(origin.x, end.x) + radius, Math.max(origin.y, end.y) + radius, Math.max(origin.z, end.z) + radius);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (!isInsideBeam(origin, direction, length, radius, target)) continue;
            if (GauntletHelper.isFriendly(player, target)) {
                float before = target.getHealth();
                target.heal((float) cfg.allyHealPerPulse.getAsDouble());
                healedTotal += Math.max(0f, target.getHealth() - before);
            } else {
                target.hurt(OLRUDamageTypes.finalAnswerCoalescence(level, player),
                        (float) cfg.enemyDamagePerPulse.getAsDouble());
            }
        }

        if (healedTotal > 0f) {
            // Beam healing feeds the next Coalescence at the same rate as Biotic Spray healing.
            GauntletEventHandlers.addFinalAnswerCoalescenceCharge(
                    player,
                    healedTotal * (float) (OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY.ultChargePercentPerHeal.get() / 100.0));
        }
    }

    private static boolean isInsideBeam(Vec3 origin, Vec3 direction, double length, double radius, LivingEntity target) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double along = center.subtract(origin).dot(direction);
        if (along < 0.0 || along > length) return false;
        Vec3 axisPoint = origin.add(direction.scale(along));
        return center.distanceToSqr(axisPoint) <= radius * radius;
    }

    private static void endCoalescence(ServerPlayer player) {
        COALESCING.remove(player.getUUID());
        GauntletEffectBroadcaster.stopCoalescenceBeam(player);
        GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.COALESCENCE_CHANNEL);
    }

    private static class FadeState {
        int ticksRemaining;
        final boolean wasInvisible;

        FadeState(int ticksRemaining, boolean wasInvisible) {
            this.ticksRemaining = ticksRemaining;
            this.wasInvisible = wasInvisible;
        }
    }

    private static class CoalescenceState {
        int ticksRemaining;
        int age;

        CoalescenceState(int ticksRemaining) {
            this.ticksRemaining = ticksRemaining;
        }
    }
}
