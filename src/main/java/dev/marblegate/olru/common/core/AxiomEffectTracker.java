package dev.marblegate.olru.common.core;

import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.core.movement.MovementTaskAssignmentResult;
import dev.marblegate.olru.common.core.movement.MovementTaskProperties;
import dev.marblegate.olru.common.core.movement.task.EntityPushTask;
import dev.marblegate.olru.common.entity.AxiomBarrierEntity;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.GraviticFluxConfig;
import dev.marblegate.olru.config.KineticGraspConfig;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side tracker for The Axiom's timed effects: Experimental Barrier regen, the Kinetic
 * Grasp absorb channel, Accretion NoAI knockdowns, and the Gravitic Flux phase machine.
 */
public class AxiomEffectTracker {
    private static final Map<UUID, Long> LAST_BARRIER_ACTIVITY = new HashMap<>();
    private static final Map<UUID, UUID> ACTIVE_BARRIERS = new HashMap<>(); // owner -> barrier entity
    private static final Map<UUID, KineticGrasp> KINETIC_GRASPS = new HashMap<>();
    private static final Map<UUID, Knockdown> KNOCKED_DOWN_MOBS = new HashMap<>();
    private static final Map<UUID, FluxState> FLUXES = new HashMap<>();

    /** Stamps barrier activity, restarting the regen delay. Called by the channel tick and barrier hurt. */
    public static void noteBarrierActivity(ServerPlayer player) {
        LAST_BARRIER_ACTIVITY.put(player.getUUID(), player.level().getGameTime());
    }

    public static void setActiveBarrier(ServerPlayer player, AxiomBarrierEntity barrier) {
        ACTIVE_BARRIERS.put(player.getUUID(), barrier.getUUID());
    }

    /** The player's currently deployed barrier, or null; stale map entries are pruned on lookup. */
    public static @Nullable AxiomBarrierEntity getActiveBarrier(ServerPlayer player) {
        UUID barrierId = ACTIVE_BARRIERS.get(player.getUUID());
        if (barrierId == null) return null;
        if (player.level().getEntity(barrierId) instanceof AxiomBarrierEntity barrier && barrier.isAlive()) {
            return barrier;
        }
        ACTIVE_BARRIERS.remove(player.getUUID());
        return null;
    }

    public static void clearActiveBarrier(AxiomBarrierEntity barrier) {
        UUID owner = barrier.getOwnerUUID();
        if (owner != null && barrier.getUUID().equals(ACTIVE_BARRIERS.get(owner))) {
            ACTIVE_BARRIERS.remove(owner);
        }
    }

    /** Starts the Kinetic Grasp absorb channel; the shield is granted when the channel ends. */
    public static void startKineticGrasp(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        KINETIC_GRASPS.put(player.getUUID(), new KineticGrasp(ticks));
    }

    /** Accretion knockdown for mobs: suppresses AI, restoring the previous NoAI flag on expiry. */
    public static void knockdown(Mob mob, int ticks) {
        if (ticks <= 0) return;
        Knockdown previous = KNOCKED_DOWN_MOBS.get(mob.getUUID());
        boolean wasNoAi = previous != null ? previous.wasNoAi : mob.isNoAi();
        int remaining = previous != null ? Math.max(previous.ticksRemaining, ticks) : ticks;
        KNOCKED_DOWN_MOBS.put(mob.getUUID(), new Knockdown(remaining, wasNoAi));
        mob.setNoAi(true);
    }

    /**
     * Starts Gravitic Flux: the caster rises for riseTicks (client-predicted push, like Rising
     * Uppercut), then hovers and aims the lift zone.
     */
    public static void startFlux(ServerPlayer player) {
        FluxState existing = FLUXES.get(player.getUUID());
        if (existing != null) endFlux(player, existing);

        var cfg = OLRUConfig.THE_AXIOM.GRAVITIC_FLUX;
        int riseTicks = Math.max(1, cfg.riseTicks.get());
        MovementManager.assign(player,
                new EntityPushTask(new Vec3(0, cfg.riseHeight.get() / riseTicks, 0), cfg.riseHeight.get(), true),
                MovementTaskProperties.playerActive(player.getUUID()));
        FLUXES.put(player.getUUID(), new FluxState(FluxPhase.RISING, riseTicks));
        refreshFluxPose(player);
        GauntletSoundHelper.fluxRise(player.level(), player.position());
    }

    /** True while the entity's Gravitic Flux is waiting for the lift zone to be confirmed. */
    public static boolean isAiming(LivingEntity entity) {
        FluxState state = FLUXES.get(entity.getUUID());
        return state != null && state.phase == FluxPhase.AIMING;
    }

    /** Confirms the AIMING zone at the caster's current ground projection and starts the lift. */
    public static void confirmAim(ServerPlayer player) {
        FluxState state = FLUXES.get(player.getUUID());
        if (state == null || state.phase != FluxPhase.AIMING) return;
        beginLift(player, state, groundProjection(player));
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity instanceof ServerPlayer player) {
            tickBarrierRegen(player);
            tickKineticGrasp(player);
            tickFlux(player);
        }
        if (entity instanceof Mob mob) {
            tickKnockdown(mob);
        }
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            cleanup(event.getEntity());
        }
    }

    public static void onEntityDeath(LivingDeathEvent event) {
        cleanup(event.getEntity());
    }

    private static void tickBarrierRegen(ServerPlayer player) {
        if (getActiveBarrier(player) != null) return; // deployed: no regen
        var cfg = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER;
        // The recall/broken cooldown lives on the SKILL_ONE state; regen waits for it to run out.
        if (AxiomBarrierState.isCooldownActive(player)) return;
        long now = player.level().getGameTime();
        Long lastActivity = LAST_BARRIER_ACTIVITY.get(player.getUUID());
        if (lastActivity != null && now - lastActivity <= cfg.regenDelayTicks.get()) return;
        // Skip players who never touched The Axiom (no state yet) and full bars, so the group is
        // not pointlessly created and no empty sync is sent.
        if (AxiomBarrierState.getDurabilityFraction(player) >= 1f) return;
        // fraction per tick = per-second rate / 20 / max durability
        AxiomBarrierState.addDurability(player, (float) (cfg.durabilityRegenPerSecond.get() / 20.0 / cfg.maxDurability.get()));
    }

    private static void tickKineticGrasp(ServerPlayer player) {
        KineticGrasp state = KINETIC_GRASPS.get(player.getUUID());
        if (state == null) return;

        if (!player.isAlive()) {
            endKineticGrasp(player, state);
            return;
        }

        var cfg = OLRUConfig.THE_AXIOM.KINETIC_GRASP;
        ServerLevel level = player.level();
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, cfg.selfSlowAmplifier.get(), false, false, false));
        state.accumulatedShield += AxiomAbsorptionHelper.absorbZone(level, graspZone(player, cfg), player);
        GauntletEffectBroadcaster.kineticGraspField(player, 4);
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.KINETIC_GRASP, 0, 4);

        if (--state.ticksRemaining <= 0) {
            endKineticGrasp(player, state);
        }
    }

    /**
     * The frontal absorb cone is approximated by a single AABB spanning the segment from the eye
     * position to eye + look * range, inflated by the cone's end radius (range * tan(halfAngle),
     * capped at range). A generous box is fine here: projectile scans are cheap and Sigma's grasp
     * is forgiving.
     */
    private static AABB graspZone(ServerPlayer player, KineticGraspConfig cfg) {
        double range = cfg.range.get();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        double halfAngleDegrees = Math.min(89.0, cfg.coneAngleDegrees.get() / 2.0);
        double margin = Math.min(range, range * Math.tan(Math.toRadians(halfAngleDegrees)));
        return new AABB(
                Math.min(eye.x, end.x) - margin, Math.min(eye.y, end.y) - margin, Math.min(eye.z, end.z) - margin,
                Math.max(eye.x, end.x) + margin, Math.max(eye.y, end.y) + margin, Math.max(eye.z, end.z) + margin);
    }

    private static void endKineticGrasp(ServerPlayer player, KineticGrasp state) {
        KINETIC_GRASPS.remove(player.getUUID());
        applyGraspShield(player, state);
        GauntletEffectBroadcaster.stopKineticGraspField(player);
        GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.KINETIC_GRASP);
    }

    /** Converts the absorbed credit into Absorption hearts (4 hp per amplifier level). */
    private static void applyGraspShield(ServerPlayer player, KineticGrasp state) {
        var cfg = OLRUConfig.THE_AXIOM.KINETIC_GRASP;
        double shield = Math.min(state.accumulatedShield * cfg.shieldConversion.get(), cfg.maxShield.get());
        if (shield <= 0.0) return;
        int amplifier = Math.max(0, (int) Math.ceil(shield / 4.0) - 1);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, cfg.shieldDurationTicks.get(), amplifier, false, true));
    }

    private static void tickFlux(ServerPlayer player) {
        FluxState state = FLUXES.get(player.getUUID());
        if (state == null) return;

        if (!player.isAlive()) {
            endFlux(player, state);
            return;
        }

        var cfg = OLRUConfig.THE_AXIOM.GRAVITIC_FLUX;
        switch (state.phase) {
            case RISING -> {
                if (--state.ticksRemaining <= 0) {
                    state.phase = FluxPhase.AIMING;
                    state.ticksRemaining = cfg.aimTicks.get();
                    refreshFluxPose(player);
                }
            }
            case AIMING -> {
                // Hover lock: pinned in place, free to aim the zone with the mouse.
                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                player.resetFallDistance();
                GauntletEffectBroadcaster.graviticZone(
                        player, groundProjection(player), (float) cfg.zoneRadius.getAsDouble(), 4);
                if (--state.ticksRemaining <= 0) {
                    confirmAim(player);
                }
            }
            case LIFT -> tickFluxLift(player, state, cfg);
        }
    }

    private static void beginLift(ServerPlayer player, FluxState state, Vec3 zoneCenter) {
        var cfg = OLRUConfig.THE_AXIOM.GRAVITIC_FLUX;
        ServerLevel level = player.level();
        state.phase = FluxPhase.LIFT;
        state.ticksRemaining = cfg.suspendTicks.get();
        state.zoneCenter = zoneCenter;
        GauntletEffectBroadcaster.stopGraviticZone(player, zoneCenter);
        refreshFluxPose(player);

        double radius = cfg.zoneRadius.get();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(
                        zoneCenter.x - radius, zoneCenter.y - radius, zoneCenter.z - radius,
                        zoneCenter.x + radius, zoneCenter.y + radius, zoneCenter.z + radius),
                e -> e != player
                        && e.isAlive()
                        && !e.isSpectator()
                        && e.distanceToSqr(zoneCenter) <= radius * radius
                        && !GauntletHelper.isFriendly(player, e));
        for (LivingEntity target : targets) {
            MovementTaskAssignmentResult result = MovementManager.assign(target,
                    new EntityPushTask(new Vec3(0, cfg.liftHeight.get() / 10.0, 0), cfg.liftHeight.get(), true),
                    MovementTaskProperties.externalKnockback(player.getUUID()));
            if (!result.accepted()) continue;
            state.liftedTargets.add(target.getUUID());
            GauntletParticleHelper.fluxLift(level, target.position(), cfg.liftHeight.get());
        }
        GauntletSoundHelper.fluxLift(level, zoneCenter);
    }

    private static void tickFluxLift(ServerPlayer player, FluxState state, GraviticFluxConfig cfg) {
        ServerLevel level = player.level();
        boolean lastTick = --state.ticksRemaining <= 0;
        for (UUID targetId : state.liftedTargets) {
            if (!(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) continue;
            if (lastTick) {
                target.setNoGravity(false);
            } else {
                // Suspension lock, re-asserted every tick so nothing escapes the lift.
                target.setNoGravity(true);
                target.setDeltaMovement(Vec3.ZERO);
                target.hurtMarked = true;
                target.resetFallDistance();
            }
        }
        if (lastTick) {
            slamFluxTargets(player, state, cfg);
            endFlux(player, state);
        }
    }

    /** Slams the recorded lift list: only enemies actually lifted are hit, no radius re-check. */
    private static void slamFluxTargets(ServerPlayer player, FluxState state, GraviticFluxConfig cfg) {
        ServerLevel level = player.level();
        Vec3 center = state.zoneCenter != null ? state.zoneCenter : player.position();
        DamageSource source = OLRUDamageTypes.axiomGraviticFlux(level, player);
        for (UUID targetId : state.liftedTargets) {
            if (!(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) continue;
            float damage = (float) Math.min(
                    target.getMaxHealth() * cfg.slamMaxHealthFraction.get(), cfg.slamDamageCap.get());
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, cfg.slowTicks.get(), 1, false, false, false));
        }
        GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.FLUX_CHANNEL);
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.FLUX_SLAM, 0, 20);
        GauntletEffectBroadcaster.seismicSlamRing(level, center, (float) cfg.zoneRadius.getAsDouble());
        GauntletParticleHelper.fluxSlamShock(level, center, cfg.zoneRadius.get());
        GauntletSoundHelper.fluxSlam(level, center);
    }

    private static void endFlux(ServerPlayer player, FluxState state) {
        FLUXES.remove(player.getUUID());
        player.setNoGravity(false);
        releaseFluxTargets(player.level(), state);
        GauntletEffectBroadcaster.stopGraviticZone(
                player, state.zoneCenter != null ? state.zoneCenter : player.position());
        GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.FLUX_CHANNEL);
    }

    private static void releaseFluxTargets(ServerLevel level, FluxState state) {
        for (UUID targetId : state.liftedTargets) {
            if (level.getEntity(targetId) instanceof LivingEntity target) {
                target.setNoGravity(false);
            }
        }
        state.liftedTargets.clear();
    }

    /** FLUX_CHANNEL spans the whole ultimate; re-sent at each phase transition to refresh its duration. */
    private static void refreshFluxPose(ServerPlayer player) {
        var cfg = OLRUConfig.THE_AXIOM.GRAVITIC_FLUX;
        int duration = cfg.riseTicks.get() + cfg.aimTicks.get() + cfg.suspendTicks.get() + 20;
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.FLUX_CHANNEL, 0, duration);
    }

    /**
     * The aimed zone center: where the caster's look ray meets a block (up to 30 blocks out),
     * or the ground point directly below the caster when the ray hits nothing.
     */
    private static Vec3 groundProjection(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(
                eye, eye.add(player.getLookAngle().scale(30.0)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        BlockPos start = player.blockPosition();
        for (int y = start.getY(); y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            if (!level.getBlockState(pos).isAir()) {
                return new Vec3(player.getX(), y + 1.0, player.getZ());
            }
        }
        return new Vec3(player.getX(), level.getMinY(), player.getZ());
    }

    private static void tickKnockdown(Mob mob) {
        Knockdown state = KNOCKED_DOWN_MOBS.get(mob.getUUID());
        if (state == null) return;

        if (!mob.isAlive()) {
            KNOCKED_DOWN_MOBS.remove(mob.getUUID());
            mob.setNoAi(state.wasNoAi);
            return;
        }
        if (!mob.isNoAi()) {
            mob.setNoAi(true); // keep suppressed for the whole knockdown
        }
        if (--state.ticksRemaining <= 0) {
            KNOCKED_DOWN_MOBS.remove(mob.getUUID());
            mob.setNoAi(state.wasNoAi);
        }
    }

    private static void cleanup(Entity entity) {
        UUID id = entity.getUUID();
        LAST_BARRIER_ACTIVITY.remove(id);
        ACTIVE_BARRIERS.remove(id);
        KineticGrasp grasp = KINETIC_GRASPS.remove(id);
        if (grasp != null && entity instanceof ServerPlayer player) {
            applyGraspShield(player, grasp);
            GauntletEffectBroadcaster.stopKineticGraspField(player);
            GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.KINETIC_GRASP);
        }
        Knockdown knockdown = KNOCKED_DOWN_MOBS.remove(id);
        if (knockdown != null && entity instanceof Mob mob) {
            mob.setNoAi(knockdown.wasNoAi);
        }
        FluxState flux = FLUXES.remove(id);
        if (flux != null && entity instanceof ServerPlayer player) {
            player.setNoGravity(false);
            releaseFluxTargets(player.level(), flux);
            GauntletEffectBroadcaster.stopGraviticZone(
                    player, flux.zoneCenter != null ? flux.zoneCenter : player.position());
            GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.FLUX_CHANNEL);
            GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.FLUX_SLAM);
        }
        // A suspended lift target leaving or dying gets its gravity back.
        for (FluxState active : FLUXES.values()) {
            if (active.liftedTargets.remove(id) && entity instanceof LivingEntity living) {
                living.setNoGravity(false);
            }
        }
    }

    private static class KineticGrasp {
        int ticksRemaining;
        int accumulatedShield;

        KineticGrasp(int ticksRemaining) {
            this.ticksRemaining = ticksRemaining;
        }
    }

    private enum FluxPhase {
        RISING,
        AIMING,
        LIFT
    }

    private static class FluxState {
        FluxPhase phase;
        int ticksRemaining;
        @Nullable
        Vec3 zoneCenter;
        final List<UUID> liftedTargets = new ArrayList<>();

        FluxState(FluxPhase phase, int ticksRemaining) {
            this.phase = phase;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static class Knockdown {
        int ticksRemaining;
        final boolean wasNoAi;

        Knockdown(int ticksRemaining, boolean wasNoAi) {
            this.ticksRemaining = ticksRemaining;
            this.wasNoAi = wasNoAi;
        }
    }
}
