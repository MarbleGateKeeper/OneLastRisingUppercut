package dev.marblegate.olru.common.core.movement.task;

import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.core.movement.MovementTaskProperties;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.network.payload.ClientboundStartMovementPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class RocketPunchTask implements MovementTask {
    private final Vec3 velocity;
    private final double maxDistance;
    private final float damage;
    private final float wallBonusDamage;
    private final float mobLaunchSpeed;
    private final UUID launcherUUID;

    public RocketPunchTask(Vec3 velocity, double distance,
            float damage, float wallBonusDamage, float mobLaunchSpeed,
            UUID launcherUUID) {
        this.velocity = velocity;
        this.maxDistance = distance;
        this.damage = damage;
        this.wallBonusDamage = wallBonusDamage;
        this.mobLaunchSpeed = mobLaunchSpeed;
        this.launcherUUID = launcherUUID;
    }

    @Override
    public boolean tick(LivingEntity entity, ServerLevel level) {
        ServerPlayer player = (ServerPlayer) entity;
        UUID taskId = MovementManager.getTaskId(player);
        if (taskId == null) return true;

        PacketDistributor.sendToPlayer(player, new ClientboundStartMovementPayload(
                taskId, MovementTaskType.ROCKET_PUNCH, velocity, maxDistance, 0f, false));
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.ROCKET_PUNCH_FLIGHT, 1.0f, 200);
        GauntletSoundHelper.rocketLaunch(level, player.position());

        Vec3 startPos = player.position();
        MovementManager.switchTo(player, new AwaitingClientResultTask(
                taskId, startPos, maxDistance,
                (p, targets) -> applyHitEffects(p, targets, false),
                (p, context) -> GauntletEffectBroadcaster.stopPose(p, GauntletPoseType.ROCKET_PUNCH_FLIGHT)));
        return false;
    }

    @Override
    public void onCancelled(LivingEntity entity) {
        GauntletEffectBroadcaster.stopPose(entity, GauntletPoseType.ROCKET_PUNCH_FLIGHT);
    }

    private void applyHitEffects(ServerPlayer launcher, List<LivingEntity> targets, boolean wallHit) {
        GauntletEffectBroadcaster.stopPose(launcher, GauntletPoseType.ROCKET_PUNCH_FLIGHT);
        GauntletEffectBroadcaster.pose(launcher, GauntletPoseType.ROCKET_PUNCH_IMPACT, 0, 8);
        if (targets.isEmpty()) return;
        ServerLevel level = launcher.level();
        ServerPlayer src = level.getServer().getPlayerList().getPlayer(launcherUUID);
        Vec3 knockbackDir = velocity.normalize();

        Vec3 impactPos = launcher.position().add(0, launcher.getBbHeight() * 0.5, 0);
        GauntletParticleHelper.rocketImpactBurst(level, impactPos);
        GauntletEffectBroadcaster.rocketPunchImpact(level, impactPos, 1.0f);
        GauntletSoundHelper.rocketImpact(level, impactPos);

        for (LivingEntity target : targets) {
            target.hurt(OLRUDamageTypes.legacyPrimeRocketPunch(level, src),
                    wallHit ? damage + wallBonusDamage : damage);
            target.invulnerableTime = 0;
            MovementManager.assign(target, new EntityPushTask(
                    knockbackDir.scale(mobLaunchSpeed), maxDistance, wallBonusDamage, launcherUUID,
                    OLRUDamageTypes.LEGACY_PRIME_ROCKET_PUNCH_WALL_IMPACT),
                    MovementTaskProperties.externalKnockback(launcherUUID));
        }
    }
}
