package dev.marblegate.olru.common.core;

import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload;
import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload.EffectType;
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

    private static void broadcast(ServerLevel level, Vec3 origin, ClientboundGauntletEffectPayload payload) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(origin) <= DEFAULT_RANGE_SQR) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
