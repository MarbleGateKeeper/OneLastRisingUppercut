package dev.marblegate.olru.common.core.movement.task;

import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.network.payload.ClientboundMovementTaskStatePayload;
import dev.marblegate.olru.network.payload.ClientboundStartMovementPayload;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class MeteorStrikeTask implements MovementTask {
    private static final double HOVER_HEIGHT_EPSILON = 0.03125;
    private static final double HOVER_HORIZONTAL_SPEED = 0.45;
    private static final double HOVER_SPRINT_MULTIPLIER = 1.25;
    private static final double HOVER_DISTANCE_MARGIN = 4.0;

    private enum Phase {
        HOVER,
        FALL_REQUESTED
    }

    private Phase phase = Phase.HOVER;
    private int hoverTicksRemaining;
    private final double fallSpeed;
    private final double maxFallDistance;
    private final float innerRadius;
    private final float outerRadius;
    private final Runnable onLand;
    private boolean hoverStateSent = false;
    private boolean hoverTaskSent = false;
    private Vec3 lastLandingTarget = null;
    private double hoverY = Double.NaN;
    private Vec3 hoverStartPosition = null;
    private Vec3 lastSafeHoverPosition = null;
    private int totalHoverTicks;

    public MeteorStrikeTask(
            int hoverTicks, double fallSpeed, double maxFallDistance,
            float innerRadius, float outerRadius, Runnable onLand) {
        this.hoverTicksRemaining = hoverTicks;
        this.totalHoverTicks = hoverTicks;
        this.fallSpeed = fallSpeed;
        this.maxFallDistance = maxFallDistance;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.onLand = onLand;
    }

    @Override
    public boolean tick(LivingEntity entity, ServerLevel level) {
        ServerPlayer player = (ServerPlayer) entity;
        if (phase == Phase.HOVER) {
            return tickHover(player);
        }

        startFall(player);
        return false;
    }

    private boolean tickHover(ServerPlayer player) {
        sendHoverState(player, true);
        if (Double.isNaN(hoverY)) {
            hoverY = player.getY();
            hoverStartPosition = new Vec3(player.getX(), hoverY, player.getZ());
            lastSafeHoverPosition = hoverStartPosition;
        }
        player.setNoGravity(true);
        keepHoverHeight(player);
        startClientHoverTask(player);
        player.connection.send(new ClientboundPlayerRotationPacket(0f, true, 90f, false));
        lastLandingTarget = landingTarget(player);
        GauntletEffectBroadcaster.meteorTarget(player, lastLandingTarget, innerRadius, outerRadius);

        if (--hoverTicksRemaining <= 0) {
            phase = Phase.FALL_REQUESTED;
        }
        return false;
    }

    private void startFall(ServerPlayer player) {
        sendHoverState(player, false);
        keepMeteorTargetVisibleDuringFall(player);
        player.setNoGravity(false);
        UUID taskId = MovementManager.getTaskId(player);
        if (taskId == null) return;

        player.teleportTo(player.getX(), hoverYForFall(player), player.getZ());
        Vec3 velocity = new Vec3(0, -fallSpeed, 0);
        PacketDistributor.sendToPlayer(player, new ClientboundStartMovementPayload(
                taskId, MovementTaskType.METEOR_FALL, velocity, maxFallDistance, 0f, false));

        MovementManager.switchTo(player, new AwaitingClientResultTask(
                taskId, player.position(), maxFallDistance,
                null,
                p -> {
                    p.setNoGravity(false);
                    stopMeteorTarget(p);
                    onLand.run();
                }));
    }

    @Override
    public MovementTaskActionResult onRuntimeAction(
            LivingEntity entity, ServerLevel level, MovementTaskActionType actionType) {
        if (actionType != MovementTaskActionType.PRIMARY_ATTACK_PRESSED) return MovementTaskActionResult.IGNORED;
        if (phase != Phase.HOVER) return MovementTaskActionResult.CONSUMED;

        phase = Phase.FALL_REQUESTED;
        hoverTicksRemaining = 0;
        return MovementTaskActionResult.CONSUMED;
    }

    @Override
    public void onCancelled(LivingEntity entity) {
        entity.setNoGravity(false);
        if (entity instanceof ServerPlayer player) {
            sendHoverState(player, false);
            stopMeteorTarget(player);
        }
    }

    private Vec3 landingTarget(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos start = player.blockPosition();
        for (int y = start.getY(); y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            if (!level.getBlockState(pos).isAir()) {
                return new Vec3(player.getX(), y + 1.0, player.getZ());
            }
        }
        return new Vec3(player.getX(), level.getMinY(), player.getZ());
    }

    private void keepHoverHeight(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, 0.0, movement.z);
        player.hurtMarked = true;
        player.resetFallDistance();

        if (Math.abs(player.getY() - hoverY) > HOVER_HEIGHT_EPSILON) {
            player.teleportTo(player.getX(), hoverY, player.getZ());
        }

        if (hoverStartPosition == null) return;
        double maxDistance = maxHoverHorizontalDistance();
        Vec3 offset = new Vec3(
                player.getX() - hoverStartPosition.x,
                0.0,
                player.getZ() - hoverStartPosition.z);
        if (offset.horizontalDistance() > maxDistance) {
            Vec3 clamped = offset.normalize().scale(maxDistance);
            player.teleportTo(hoverStartPosition.x + clamped.x, hoverY, hoverStartPosition.z + clamped.z);
        }
        validateHoverCollision(player);
    }

    private void validateHoverCollision(ServerPlayer player) {
        if (player.level().noCollision(player, player.getBoundingBox())) {
            lastSafeHoverPosition = new Vec3(player.getX(), hoverY, player.getZ());
            return;
        }

        Vec3 fallback = lastSafeHoverPosition != null ? lastSafeHoverPosition : hoverStartPosition;
        if (fallback != null) {
            player.teleportTo(fallback.x, hoverY, fallback.z);
        }
    }

    private double hoverYForFall(ServerPlayer player) {
        return Double.isNaN(hoverY) ? player.getY() : hoverY;
    }

    private void startClientHoverTask(ServerPlayer player) {
        if (hoverTaskSent) return;
        UUID taskId = MovementManager.getTaskId(player);
        if (taskId == null) return;
        hoverTaskSent = true;
        PacketDistributor.sendToPlayer(player, new ClientboundStartMovementPayload(
                taskId,
                MovementTaskType.METEOR_HOVER,
                new Vec3(HOVER_HORIZONTAL_SPEED, hoverY, 0.0),
                maxHoverHorizontalDistance(),
                0f,
                false));
    }

    private double maxHoverHorizontalDistance() {
        return HOVER_HORIZONTAL_SPEED * HOVER_SPRINT_MULTIPLIER * Math.max(1, totalHoverTicks) + HOVER_DISTANCE_MARGIN;
    }

    private void sendHoverState(ServerPlayer player, boolean active) {
        if (active && hoverStateSent) return;
        hoverStateSent = active;
        PacketDistributor.sendToPlayer(player, ClientboundMovementTaskStatePayload.meteorStrikeHover(active));
    }

    private void stopMeteorTarget(ServerPlayer player) {
        GauntletEffectBroadcaster.stopMeteorTarget(
                player,
                lastLandingTarget != null ? lastLandingTarget : player.position());
    }

    private void keepMeteorTargetVisibleDuringFall(ServerPlayer player) {
        Vec3 target = lastLandingTarget != null ? lastLandingTarget : landingTarget(player);
        lastLandingTarget = target;
        int fallTicks = (int) Math.ceil(maxFallDistance / Math.max(0.05, fallSpeed)) + 20;
        GauntletEffectBroadcaster.meteorTarget(
                player, target, innerRadius, outerRadius, Math.max(20, Math.min(160, fallTicks)));
    }
}
