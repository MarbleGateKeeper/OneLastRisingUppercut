package dev.marblegate.olru.client.movement.task;

import dev.marblegate.olru.common.util.SweptCollisionHelper;
import dev.marblegate.olru.common.util.SweptCollisionHelper.SweepResult;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ClientMeteorHoverTask implements ClientMovementTask {
    private static final double HORIZONTAL_DECAY = 0.45;
    private static final double SPRINT_MULTIPLIER = 1.25;
    private static final double HEIGHT_EPSILON = 0.03125;

    private final UUID taskId;
    private final double hoverY;
    private final double horizontalSpeed;
    private final double maxHorizontalDistance;
    private Vec3 startPosition = null;

    public ClientMeteorHoverTask(UUID taskId, double hoverY, double horizontalSpeed, double maxHorizontalDistance) {
        this.taskId = taskId;
        this.hoverY = hoverY;
        this.horizontalSpeed = horizontalSpeed;
        this.maxHorizontalDistance = maxHorizontalDistance;
    }

    @Override
    public boolean tick(LocalPlayer player, ClientLevel level) {
        if (startPosition == null) {
            startPosition = new Vec3(player.getX(), hoverY, player.getZ());
        }

        player.setNoGravity(true);
        player.resetFallDistance();

        Vec3 movement = horizontalMovement(player);
        Vec3 allowed = clampToAllowedRadius(player, movement);
        SweepResult sweep = SweptCollisionHelper.sweepBlocks(player, level, allowed);
        Vec3 step = sweep.blockHit() ? sweep.snapOffset() : allowed;

        Vec3 pos = player.position();
        double y = Math.abs(pos.y - hoverY) > HEIGHT_EPSILON ? hoverY : pos.y;
        player.setPosRaw(pos.x + step.x, y, pos.z + step.z);
        player.setDeltaMovement(step.x, 0.0, step.z);
        player.hurtMarked = true;
        return false;
    }

    @Override
    public void onCancelled(LocalPlayer player) {
        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private Vec3 horizontalMovement(LocalPlayer player) {
        Vec2 input = player.input.getMoveVector();
        if (input.lengthSquared() < 1.0E-6F) {
            Vec3 current = player.getDeltaMovement();
            return new Vec3(current.x * HORIZONTAL_DECAY, 0.0, current.z * HORIZONTAL_DECAY);
        }

        double speed = horizontalSpeed;
        if (player.isSprinting() || player.input.keyPresses.sprint()) {
            speed *= SPRINT_MULTIPLIER;
        }

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        double x = input.x * cos - input.y * sin;
        double z = input.y * cos + input.x * sin;
        Vec3 direction = new Vec3(x, 0.0, z);
        return direction.lengthSqr() < 1.0E-6 ? Vec3.ZERO : direction.normalize().scale(speed);
    }

    private Vec3 clampToAllowedRadius(LocalPlayer player, Vec3 requested) {
        if (startPosition == null || maxHorizontalDistance <= 0) return requested;

        Vec3 currentOffset = new Vec3(
                player.getX() - startPosition.x,
                0.0,
                player.getZ() - startPosition.z);
        Vec3 nextOffset = currentOffset.add(requested.x, 0.0, requested.z);
        double nextDistance = nextOffset.horizontalDistance();
        if (nextDistance <= maxHorizontalDistance) return requested;

        Vec3 clampedOffset = nextOffset.normalize().scale(maxHorizontalDistance);
        return new Vec3(
                clampedOffset.x - currentOffset.x,
                0.0,
                clampedOffset.z - currentOffset.z);
    }
}
