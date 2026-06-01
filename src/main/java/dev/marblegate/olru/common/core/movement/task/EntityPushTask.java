package dev.marblegate.olru.common.core.movement.task;

import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.util.SweptCollisionHelper;
import dev.marblegate.olru.common.util.SweptCollisionHelper.SweepResult;
import dev.marblegate.olru.network.payload.ClientboundStartMovementPayload;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class EntityPushTask implements MovementTask {
    private final Vec3 velocity;
    private double remainingDistance;
    private final float collisionDamage;
    private final UUID launcherUUID;
    private final @Nullable ResourceKey<DamageType> collisionDamageType;
    private final boolean preservePlayerEndVelocity;

    public EntityPushTask(Vec3 velocity, double distance, float collisionDamage, UUID launcherUUID,
            @Nullable ResourceKey<DamageType> collisionDamageType, boolean preservePlayerEndVelocity) {
        this.velocity = velocity;
        this.remainingDistance = distance;
        this.collisionDamage = collisionDamage;
        this.launcherUUID = launcherUUID;
        this.collisionDamageType = collisionDamageType;
        this.preservePlayerEndVelocity = preservePlayerEndVelocity;
    }

    public EntityPushTask(Vec3 velocity, double distance, float collisionDamage, UUID launcherUUID,
            boolean preservePlayerEndVelocity) {
        this(velocity, distance, collisionDamage, launcherUUID, null, preservePlayerEndVelocity);
    }

    public EntityPushTask(Vec3 velocity, double distance, float collisionDamage, UUID launcherUUID) {
        this(velocity, distance, collisionDamage, launcherUUID, false);
    }

    public EntityPushTask(Vec3 velocity, double distance, float collisionDamage, UUID launcherUUID,
            ResourceKey<DamageType> collisionDamageType) {
        this(velocity, distance, collisionDamage, launcherUUID, collisionDamageType, false);
    }

    public EntityPushTask(Vec3 velocity, double distance, boolean preservePlayerEndVelocity) {
        this(velocity, distance, 0f, null, preservePlayerEndVelocity);
    }

    public EntityPushTask(Vec3 velocity, double distance) {
        this(velocity, distance, false);
    }

    @Override
    public boolean tick(LivingEntity entity, ServerLevel level) {
        if (entity instanceof ServerPlayer player) {
            return tickPlayer(player);
        }
        return tickMob(entity, level);
    }

    private boolean tickPlayer(ServerPlayer player) {
        UUID taskId = MovementManager.getTaskId(player);
        if (taskId == null) return true;

        PacketDistributor.sendToPlayer(player, new ClientboundStartMovementPayload(
                taskId, MovementTaskType.ENTITY_PUSH, velocity, remainingDistance, collisionDamage, preservePlayerEndVelocity));

        Vec3 startPos = player.position();
        MovementManager.switchTo(player, new AwaitingClientResultTask(
                taskId, startPos, remainingDistance,
                null,
                collisionDamage > 0 ? (p, ctx) -> p.hurt(resolveDamageSource(p.level()), collisionDamage) : null));
        return false;
    }

    private boolean tickMob(LivingEntity entity, ServerLevel level) {
        SweepResult sweep = SweptCollisionHelper.sweepBlocks(entity, level, velocity);

        if (sweep.hasCollision()) {
            Vec3 snap = sweep.snapOffset();
            Vec3 pos = entity.position();
            entity.teleportTo(pos.x + snap.x, pos.y + snap.y, pos.z + snap.z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
            if (collisionDamage > 0) entity.hurt(resolveDamageSource(level), collisionDamage);
            return true;
        }

        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
        remainingDistance -= velocity.length();
        return remainingDistance <= 0;
    }

    @Override
    public void onCancelled(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer)) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
        }
    }

    private DamageSource resolveDamageSource(ServerLevel level) {
        ServerPlayer launcher = launcherUUID != null
                ? level.getServer().getPlayerList().getPlayer(launcherUUID)
                : null;
        if (collisionDamageType != null) {
            return OLRUDamageTypes.source(level, collisionDamageType, launcher);
        }
        if (launcherUUID != null) {
            if (launcher != null) return level.damageSources().playerAttack(launcher);
        }
        return level.damageSources().generic();
    }
}
