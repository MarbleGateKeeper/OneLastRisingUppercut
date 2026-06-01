package dev.marblegate.olru.common.core.movement.task;

import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.network.payload.ClientboundStartMovementPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class SeismicSlamTask implements MovementTask {
    private final Vec3 initialVelocity;
    private final double gravity;
    private final int maxTravelTicks;
    private final double impactRange;
    private final double impactConeAngleDegrees;
    private final float damage;
    private final int slowTicks;
    private final int slowAmplifier;
    private final UUID launcherUUID;

    public SeismicSlamTask(Vec3 initialVelocity, double gravity, int maxTravelTicks,
            double impactRange, double impactConeAngleDegrees, float damage,
            int slowTicks, int slowAmplifier, UUID launcherUUID) {
        this.initialVelocity = initialVelocity;
        this.gravity = gravity;
        this.maxTravelTicks = maxTravelTicks;
        this.impactRange = impactRange;
        this.impactConeAngleDegrees = impactConeAngleDegrees;
        this.damage = damage;
        this.slowTicks = slowTicks;
        this.slowAmplifier = slowAmplifier;
        this.launcherUUID = launcherUUID;
    }

    @Override
    public boolean tick(LivingEntity entity, ServerLevel level) {
        if (!(entity instanceof ServerPlayer player)) return true;
        UUID taskId = MovementManager.getTaskId(player);
        if (taskId == null) return true;

        PacketDistributor.sendToPlayer(player, new ClientboundStartMovementPayload(
                taskId, MovementTaskType.SEISMIC_SLAM, initialVelocity,
                maxTravelTicks, (float) gravity, false));

        Vec3 startPos = player.position();
        MovementManager.switchTo(player, new AwaitingClientResultTask(
                taskId, startPos, maxValidationDistance(),
                null,
                this::triggerImpact));
        return false;
    }

    private double maxValidationDistance() {
        Vec3 v = initialVelocity;
        double distance = 0.0;
        for (int i = 0; i < maxTravelTicks; i++) {
            distance += v.length();
            v = new Vec3(v.x * 0.985, v.y - gravity, v.z * 0.985);
        }
        return Math.max(distance, initialVelocity.length() * maxTravelTicks);
    }

    private void triggerImpact(ServerPlayer player, AwaitingClientResultTask.MovementResultContext context) {
        player.resetFallDistance();
        ServerLevel level = player.level();
        ServerPlayer source = level.getServer().getPlayerList().getPlayer(launcherUUID);
        Vec3 facing = context.facing();
        List<LivingEntity> targets = entitiesInImpactCone(player, facing);

        for (LivingEntity target : targets) {
            target.hurt(OLRUDamageTypes.legacyPrimeSeismicSlam(level, source), damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowTicks, slowAmplifier));
        }

        spawnImpactParticles(level, context.claimedPosition(), facing);
    }

    private List<LivingEntity> entitiesInImpactCone(ServerPlayer player, Vec3 facing) {
        Vec3 origin = player.position();
        Vec3 look = horizontalOrDefault(facing);
        double cosHalfAngle = Math.cos(Math.toRadians(impactConeAngleDegrees / 2.0));
        return player.level()
                .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(impactRange),
                        e -> e != player && e.isAlive() && !e.isSpectator() && (e.isPickable() || e instanceof ServerPlayer))
                .stream()
                .filter(e -> {
                    double distance = e.distanceTo(player);
                    if (distance > impactRange) return false;
                    Vec3 dir = new Vec3(e.getX() - origin.x, 0.0, e.getZ() - origin.z);
                    if (dir.lengthSqr() < 1.0E-6) return true;
                    return dir.normalize().dot(look) >= cosHalfAngle;
                })
                .toList();
    }

    private void spawnImpactParticles(ServerLevel level, Vec3 origin, Vec3 facing) {
        Vec3 forward = horizontalOrDefault(facing);
        Vec3 left = rotateY(forward, Math.toRadians(impactConeAngleDegrees / 2.0));
        Vec3 right = rotateY(forward, -Math.toRadians(impactConeAngleDegrees / 2.0));

        for (double d = 0.5; d <= impactRange; d += 0.45) {
            spawnDust(level, origin.add(left.scale(d)), 2, 0.05);
            spawnDust(level, origin.add(right.scale(d)), 2, 0.05);
            spawnShock(level, origin.add(forward.scale(d)), d);
        }

        double half = impactConeAngleDegrees / 2.0;
        for (double angle = -half; angle <= half; angle += 4.0) {
            Vec3 edge = rotateY(forward, Math.toRadians(angle)).scale(impactRange);
            spawnDust(level, origin.add(edge), 2, 0.04);
        }

        var random = level.getRandom();
        int fillCount = Math.clamp((int) (impactRange * impactRange * 2.5), 32, 160);
        for (int i = 0; i < fillCount; i++) {
            double angle = Math.toRadians(-half + random.nextDouble() * impactConeAngleDegrees);
            double distance = Math.sqrt(random.nextDouble()) * impactRange;
            Vec3 dir = rotateY(forward, angle);
            Vec3 point = origin.add(dir.scale(distance)).add(0.0, 0.05 + random.nextDouble() * 0.35, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 1, 0.08, 0.02, 0.08, 0.02);
        }
    }

    private void spawnShock(ServerLevel level, Vec3 point, double distance) {
        level.sendParticles(ParticleTypes.CRIT, point.x, point.y + 0.12, point.z, 2, 0.08, 0.03, 0.08, 0.03 + distance * 0.002);
        if (((int) (distance * 2.0)) % 3 == 0) {
            level.sendParticles(ParticleTypes.POOF, point.x, point.y + 0.08, point.z, 1, 0.12, 0.02, 0.12, 0.01);
        }
    }

    private void spawnDust(ServerLevel level, Vec3 point, int count, double speed) {
        level.sendParticles(ParticleTypes.CLOUD, point.x, point.y + 0.08, point.z, count, 0.06, 0.02, 0.06, speed);
        level.sendParticles(ParticleTypes.CRIT, point.x, point.y + 0.1, point.z, 1, 0.04, 0.02, 0.04, 0.02);
    }

    private static Vec3 horizontalOrDefault(Vec3 value) {
        Vec3 horizontal = new Vec3(value.x, 0.0, value.z);
        return horizontal.lengthSqr() > 1.0E-6 ? horizontal.normalize() : new Vec3(0.0, 0.0, 1.0);
    }

    private static Vec3 rotateY(Vec3 value, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(value.x * cos - value.z * sin, 0.0, value.x * sin + value.z * cos);
    }
}
