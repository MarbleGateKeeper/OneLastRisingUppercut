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
