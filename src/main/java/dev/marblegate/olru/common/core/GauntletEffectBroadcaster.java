package dev.marblegate.olru.common.core;

import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload;
import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload.EffectType;
import dev.marblegate.olru.network.payload.ClientboundGauntletPosePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class GauntletEffectBroadcaster {
    private static final double DEFAULT_RANGE_SQR = 96.0 * 96.0;
    public static final int FLUX_TARGET_LIFT = 1;
    public static final int FLUX_TARGET_FALL = 2;

    public static void rocketCharge(ServerPlayer player, float chargePercent, int durationTicks) {
        broadcast(player.level(), player.position(), new ClientboundGauntletEffectPayload(
                EffectType.ROCKET_CHARGE, player.getId(), -1, player.position(),
                chargePercent, 0f, durationTicks, true));
    }

    public static void stopRocketCharge(ServerPlayer player) {
        broadcast(player.level(), player.position(), new ClientboundGauntletEffectPayload(
                EffectType.ROCKET_CHARGE, player.getId(), -1, player.position(),
                0f, 0f, 0, false));
    }

    public static void meteorTarget(ServerPlayer player, Vec3 target, float innerRadius, float outerRadius) {
        meteorTarget(player, target, innerRadius, outerRadius, 8);
    }

    public static void meteorTarget(ServerPlayer player, Vec3 target, float innerRadius, float outerRadius, int durationTicks) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.METEOR_TARGET, player.getId(), -1, target,
                innerRadius, outerRadius, durationTicks, true);
        PacketDistributor.sendToPlayer(player, payload);
        broadcast(player.level(), target, payload);
    }

    public static void stopMeteorTarget(ServerPlayer player) {
        stopMeteorTarget(player, player.position());
    }

    public static void stopMeteorTarget(ServerPlayer player, Vec3 target) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.METEOR_TARGET, player.getId(), -1, target,
                0f, 0f, 0, false);
        PacketDistributor.sendToPlayer(player, payload);
        broadcast(player.level(), target, payload);
    }

    public static void graviticZone(ServerPlayer caster, Vec3 pos, float radius, int durationTicks) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.GRAVITIC_ZONE, caster.getId(), -1, pos,
                radius, 0f, durationTicks, true);
        PacketDistributor.sendToPlayer(caster, payload);
        broadcast(caster.level(), pos, payload);
    }

    public static void stopGraviticZone(ServerPlayer caster) {
        stopGraviticZone(caster, caster.position());
    }

    public static void stopGraviticZone(ServerPlayer caster, Vec3 pos) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.GRAVITIC_ZONE, caster.getId(), -1, pos,
                0f, 0f, 0, false);
        PacketDistributor.sendToPlayer(caster, payload);
        broadcast(caster.level(), pos, payload);
    }

    /**
     * Gravitic Flux phase field: 1 = RISING (on the caster, no radius), 2 = AIMING (zone decal),
     * 3 = SLAM one-shot. Phases 1/2 are TTL-refreshed every tick; phase 3 fires once with ttl 0.
     */
    public static void fluxField(ServerPlayer caster, int phase, Vec3 pos, float radius, int ttl) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.FLUX_FIELD, caster.getId(), -1, pos,
                phase, radius, ttl, true);
        PacketDistributor.sendToPlayer(caster, payload);
        broadcast(caster.level(), pos, payload);
    }

    public static void stopFluxField(ServerPlayer caster) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.FLUX_FIELD, caster.getId(), -1, caster.position(),
                0f, 0f, 0, false);
        PacketDistributor.sendToPlayer(caster, payload);
        broadcast(caster.level(), caster.position(), payload);
    }

    /** Per-target Gravitic Flux state; phase is LIFT or FALL and is TTL-refreshed every tick. */
    public static void fluxTarget(ServerPlayer caster, Entity target, int phase, int ttl) {
        broadcast(caster.level(), target.position(), new ClientboundGauntletEffectPayload(
                EffectType.FLUX_TARGET, caster.getId(), target.getId(), target.position(),
                phase, 0f, ttl, true));
    }

    public static void stopFluxTarget(ServerPlayer caster, Entity target) {
        broadcast(caster.level(), target.position(), new ClientboundGauntletEffectPayload(
                EffectType.FLUX_TARGET, caster.getId(), target.getId(), target.position(),
                0f, 0f, 0, false));
    }

    public static void fieldExtractionBeam(ServerPlayer player, Entity target, int durationTicks) {
        broadcast(player.level(), player.position(), new ClientboundGauntletEffectPayload(
                EffectType.FIELD_EXTRACTION_BEAM, player.getId(), target.getId(), target.position(),
                0f, 0f, durationTicks, true));
    }

    public static void orbTether(Entity orb, Entity target, int durationTicks) {
        if (!(orb.level() instanceof ServerLevel level)) return;
        broadcast(level, orb.position(), new ClientboundGauntletEffectPayload(
                EffectType.ORB_TETHER, orb.getId(), target.getId(), orb.position(),
                0f, 0f, durationTicks, true));
    }

    /**
     * Biotic Grasp damage beam from the casting player to the current hit point; TTL-refreshed
     * on every grasp pulse so it dies on its own shortly after the channel stops.
     */
    public static void graspTether(ServerPlayer player, Entity target, Vec3 hitCenter, int ttl) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.GRASP_TETHER, player.getId(), target.getId(), hitCenter,
                0f, 0f, ttl, true);
        PacketDistributor.sendToPlayer(player, payload);
        broadcast(player.level(), hitCenter, payload);
    }

    public static void stopGraspTether(ServerPlayer player) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.GRASP_TETHER, player.getId(), -1, player.position(),
                0f, 0f, 0, false);
        PacketDistributor.sendToPlayer(player, payload);
        broadcast(player.level(), player.position(), payload);
    }

    public static void sedated(Entity target, int durationTicks, boolean active) {
        if (!(target.level() instanceof ServerLevel level)) return;
        broadcast(level, target.position(), new ClientboundGauntletEffectPayload(
                EffectType.SEDATED, target.getId(), -1, target.position(),
                0f, 0f, durationTicks, active));
    }

    public static void nanoSurge(Entity target, int durationTicks) {
        if (!(target.level() instanceof ServerLevel level)) return;
        broadcast(level, target.position(), new ClientboundGauntletEffectPayload(
                EffectType.NANO_SURGE, target.getId(), -1, target.position(),
                0f, 0f, durationTicks, true));
    }

    public static void stopNanoSurge(Entity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        broadcast(level, target.position(), new ClientboundGauntletEffectPayload(
                EffectType.NANO_SURGE, target.getId(), -1, target.position(),
                0f, 0f, 0, false));
    }

    public static void coalescenceBeam(ServerPlayer caster, int durationTicks) {
        broadcast(caster.level(), caster.position(), new ClientboundGauntletEffectPayload(
                EffectType.COALESCENCE_BEAM, caster.getId(), -1, caster.position(),
                0f, 0f, durationTicks, true));
    }

    /** Fade vignette is personal: sent directly to the fading player only, never radius-broadcast. */
    public static void fadeVignette(ServerPlayer player, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new ClientboundGauntletEffectPayload(
                EffectType.FADE, player.getId(), -1, player.position(),
                0f, 0f, durationTicks, true));
    }

    public static void stopFadeVignette(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundGauntletEffectPayload(
                EffectType.FADE, player.getId(), -1, player.position(),
                0f, 0f, 0, false));
    }

    public static void stopCoalescenceBeam(ServerPlayer caster) {
        broadcast(caster.level(), caster.position(), new ClientboundGauntletEffectPayload(
                EffectType.COALESCENCE_BEAM, caster.getId(), -1, caster.position(),
                0f, 0f, 0, false));
    }

    public static void kineticGraspField(ServerPlayer player, int durationTicks) {
        broadcast(player.level(), player.position(), new ClientboundGauntletEffectPayload(
                EffectType.KINETIC_GRASP_FIELD, player.getId(), -1, player.position(),
                0f, 0f, durationTicks, true));
    }

    public static void stopKineticGraspField(ServerPlayer player) {
        broadcast(player.level(), player.position(), new ClientboundGauntletEffectPayload(
                EffectType.KINETIC_GRASP_FIELD, player.getId(), -1, player.position(),
                0f, 0f, 0, false));
    }

    public static void bioticSprayContact(ServerPlayer caster, Entity target, int durationTicks) {
        broadcast(caster.level(), target.position(), new ClientboundGauntletEffectPayload(
                EffectType.BIOTIC_SPRAY_CONTACT, caster.getId(), target.getId(), target.position(),
                0f, 0f, durationTicks, true));
    }

    public static void coalescenceContact(ServerPlayer caster, Entity target, boolean healing, int durationTicks) {
        broadcast(caster.level(), target.position(), new ClientboundGauntletEffectPayload(
                EffectType.COALESCENCE_CONTACT, caster.getId(), target.getId(), target.position(),
                healing ? 1f : -1f, 0f, durationTicks, true));
    }

    public static void bioticOrbBounce(Entity orb, Vec3 pos) {
        if (!(orb.level() instanceof ServerLevel level)) return;
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.BIOTIC_ORB_BOUNCE, orb.getId(), -1, pos,
                0f, 0f, 8, true));
    }

    public static void bioticOrbBurst(Entity orb, Vec3 pos) {
        if (!(orb.level() instanceof ServerLevel level)) return;
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.BIOTIC_ORB_BURST, orb.getId(), -1, pos,
                0f, 0f, 14, true));
    }

    public static void kineticGraspAbsorb(ServerPlayer caster, Vec3 projectilePos, boolean heavy) {
        broadcast(caster.level(), projectilePos, new ClientboundGauntletEffectPayload(
                EffectType.KINETIC_GRASP_ABSORB, caster.getId(), -1, projectilePos,
                heavy ? 1f : 0f, 0f, heavy ? 10 : 7, true));
    }

    public static void fluxTargetImpact(ServerPlayer caster, Entity target, Vec3 pos) {
        broadcast(caster.level(), pos, new ClientboundGauntletEffectPayload(
                EffectType.FLUX_TARGET_IMPACT, caster.getId(), target.getId(), pos,
                Math.max(0.5f, target.getBbWidth()), 0f, 14, true));
    }

    public static void rocketPunchImpact(ServerLevel level, Vec3 pos, float intensity) {
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.ROCKET_PUNCH_IMPACT, -1, -1, pos,
                intensity, 0f, 12, true));
    }

    public static void seismicSlamRing(ServerLevel level, Vec3 pos, float radius) {
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.SEISMIC_SLAM_RING, -1, -1, pos,
                radius, 0f, 14, true));
    }

    public static void hyperspherePulse(ServerLevel level, Vec3 pos, float radius) {
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.HYPERSPHERE_PULSE, -1, -1, pos,
                radius, 0f, 12, true));
    }

    public static void fluxSlamRing(ServerLevel level, Vec3 center, float radius) {
        broadcast(level, center, new ClientboundGauntletEffectPayload(
                EffectType.FLUX_SLAM_RING, -1, -1, center,
                radius, 0f, 14, true));
    }

    public static void meteorImpact(ServerPlayer caster, Vec3 pos, float radius) {
        ClientboundGauntletEffectPayload payload = new ClientboundGauntletEffectPayload(
                EffectType.METEOR_IMPACT, caster.getId(), -1, pos,
                radius, 0f, 16, true);
        PacketDistributor.sendToPlayer(caster, payload);
        broadcast(caster.level(), pos, payload);
    }

    public static void uppercutBurst(ServerLevel level, Vec3 pos) {
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.UPPERCUT_BURST, -1, -1, pos,
                0f, 0f, 12, true));
    }

    public static void nanoSurgeCast(ServerLevel level, Vec3 pos) {
        broadcast(level, pos, new ClientboundGauntletEffectPayload(
                EffectType.NANO_SURGE_CAST, -1, -1, pos,
                0f, 0f, 12, true));
    }

    public static void pose(Entity entity, GauntletPoseType pose, float param, int durationTicks) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        broadcast(level, entity.position(), new ClientboundGauntletPosePayload(
                entity.getId(), pose, param, durationTicks, true));
    }

    public static void stopPose(Entity entity, GauntletPoseType pose) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        broadcast(level, entity.position(), new ClientboundGauntletPosePayload(
                entity.getId(), pose, 0f, 0, false));
    }

    private static void broadcast(ServerLevel level, Vec3 origin, CustomPacketPayload payload) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(origin) <= DEFAULT_RANGE_SQR) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
