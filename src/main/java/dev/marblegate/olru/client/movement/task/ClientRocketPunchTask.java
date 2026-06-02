package dev.marblegate.olru.client.movement.task;

import dev.marblegate.olru.common.util.SweptCollisionHelper;
import dev.marblegate.olru.common.util.SweptCollisionHelper.SweepResult;
import dev.marblegate.olru.network.payload.ServerboundMovementResultPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ClientRocketPunchTask implements ClientMovementTask {
    private static final double JUMP_OUT_MIN_HORIZONTAL_SPEED = 1.8;
    private static final double JUMP_OUT_MAX_HORIZONTAL_SPEED = 2.4;
    private static final double JUMP_OUT_VERTICAL_SPEED = 0.85;
    private static final int JUMP_OUT_TICKS = 8;
    private static final double JUMP_OUT_HORIZONTAL_DECAY = 0.88;
    private static final double JUMP_OUT_GRAVITY = 0.08;
    private static final double JUMP_OUT_VERTICAL_DECAY = 0.98;
    private static final double IMPACT_EXPAND_HORIZONTAL = 0.9;
    private static final double IMPACT_EXPAND_VERTICAL = 0.6;

    private final UUID taskId;
    private final Vec3 velocity;
    private double remainingDistance;
    private Vec3 jumpOutVelocity = Vec3.ZERO;
    private int jumpOutTicksRemaining = 0;

    public ClientRocketPunchTask(UUID taskId, Vec3 velocity, double maxDistance) {
        this.taskId = taskId;
        this.velocity = velocity;
        this.remainingDistance = maxDistance;
    }

    @Override
    public boolean tick(LocalPlayer player, ClientLevel level) {
        if (jumpOutTicksRemaining > 0) {
            return tickJumpOut(player);
        }

        Vec3 horizontal = new Vec3(velocity.x, 0, velocity.z);

        SweepResult sweep = SweptCollisionHelper.sweep(player, level, horizontal,
                e -> e != player && e.isAlive() && (e.isPickable() || e instanceof Player) && !e.isSpectator());

        if (sweep.hasCollision()) {
            Vec3 snap = sweep.snapOffset();
            Vec3 impactOffset = sweep.collisionOffset();
            Vec3 pos = player.position();
            player.setPosRaw(pos.x + snap.x, pos.y + snap.y, pos.z + snap.z);
            player.setDeltaMovement(Vec3.ZERO);

            List<UUID> hitIds = sweep.entityHits().isEmpty()
                    ? List.of()
                    : collectImpactHits(player, level, sweep, impactOffset);
            sendResult(player, hitIds, sweep.blockHit());
            return true;
        }

        player.setDeltaMovement(velocity);
        remainingDistance -= horizontal.length();

        if (remainingDistance <= 0) {
            player.setDeltaMovement(velocity);
            player.hurtMarked = true;
            player.resetFallDistance();
            sendResult(player, List.of(), false);
            return true;
        }
        return false;
    }

    @Override
    public RuntimeDataResult onRuntimeData(LocalPlayer player, ClientLevel level, ClientMovementRuntimeData data) {
        if (data.type() != ClientMovementRuntimeData.Type.JUMP_PRESSED) return RuntimeDataResult.IGNORED;
        if (jumpOutTicksRemaining > 0) return RuntimeDataResult.CONSUMED;

        sendResult(player, List.of(), false);
        startJumpOut(player);
        return RuntimeDataResult.CONSUMED;
    }

    @Override
    public void onCancelled(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
    }

    private void sendResult(LocalPlayer player, List<UUID> hitIds, boolean wallHit) {
        ClientPacketDistributor.sendToServer(new ServerboundMovementResultPayload(
                taskId, player.position(), ServerboundMovementResultPayload.horizontalFacing(player.getLookAngle()),
                0, hitIds, wallHit));
    }

    private List<UUID> collectImpactHits(LocalPlayer player, ClientLevel level, SweepResult sweep, Vec3 impactOffset) {
        AABB impactBox = player.getBoundingBox()
                .move(impactOffset)
                .inflate(IMPACT_EXPAND_HORIZONTAL, IMPACT_EXPAND_VERTICAL, IMPACT_EXPAND_HORIZONTAL);
        List<UUID> hitIds = new java.util.ArrayList<>();
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, impactBox,
                e -> e != player && e.isAlive() && (e.isPickable() || e instanceof Player) && !e.isSpectator())) {
            UUID id = entity.getUUID();
            if (!hitIds.contains(id)) hitIds.add(id);
        }
        for (UUID id : sweep.entityHits().stream().map(e -> e.getUUID()).toList()) {
            if (!hitIds.contains(id)) hitIds.add(id);
        }
        return hitIds;
    }

    private void startJumpOut(LocalPlayer player) {
        Vec3 forward = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(velocity.x, 0.0, velocity.z);
        }
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }

        double currentHorizontalSpeed = new Vec3(velocity.x, 0.0, velocity.z).length();
        double horizontalSpeed = Math.max(JUMP_OUT_MIN_HORIZONTAL_SPEED, currentHorizontalSpeed * 0.65);
        horizontalSpeed = Math.min(JUMP_OUT_MAX_HORIZONTAL_SPEED, horizontalSpeed);

        jumpOutVelocity = forward.normalize().scale(horizontalSpeed).add(0.0, JUMP_OUT_VERTICAL_SPEED, 0.0);
        jumpOutTicksRemaining = JUMP_OUT_TICKS;
        player.setDeltaMovement(jumpOutVelocity);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    private boolean tickJumpOut(LocalPlayer player) {
        player.setDeltaMovement(jumpOutVelocity);
        player.hurtMarked = true;
        player.resetFallDistance();

        jumpOutTicksRemaining--;
        jumpOutVelocity = new Vec3(
                jumpOutVelocity.x * JUMP_OUT_HORIZONTAL_DECAY,
                (jumpOutVelocity.y - JUMP_OUT_GRAVITY) * JUMP_OUT_VERTICAL_DECAY,
                jumpOutVelocity.z * JUMP_OUT_HORIZONTAL_DECAY);
        return jumpOutTicksRemaining <= 0;
    }
}
