package dev.marblegate.olru.common.core.movement.task;

import dev.marblegate.olru.common.core.movement.MovementTaskCancelReason;
import dev.marblegate.olru.network.payload.ClientboundStopMovementPayload;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class AwaitingClientResultTask implements MovementTask {
    private static final int TIMEOUT_TICKS = 240; // 12 seconds max

    private final Vec3 startPosition;
    private final UUID taskId;
    private final double maxDistance;
    @Nullable
    private final BiConsumer<ServerPlayer, List<LivingEntity>> onEntityHit;
    @Nullable
    private final MovementResultHandler onWallHit;
    @Nullable
    private final Consumer<ServerPlayer> onTerminate;
    private int ticksRemaining;

    public AwaitingClientResultTask(UUID taskId, Vec3 startPosition, double maxDistance,
            BiConsumer<ServerPlayer, @Nullable List<LivingEntity>> onEntityHit, @Nullable MovementResultHandler onWallHit) {
        this(taskId, startPosition, maxDistance, TIMEOUT_TICKS, onEntityHit, onWallHit, null);
    }

    public AwaitingClientResultTask(UUID taskId, Vec3 startPosition, double maxDistance,
            BiConsumer<ServerPlayer, @Nullable List<LivingEntity>> onEntityHit, @Nullable MovementResultHandler onWallHit,
            @Nullable Consumer<ServerPlayer> onTerminate) {
        this(taskId, startPosition, maxDistance, TIMEOUT_TICKS, onEntityHit, onWallHit, onTerminate);
    }

    public AwaitingClientResultTask(UUID taskId, Vec3 startPosition, double maxDistance, int timeoutTicks,
            BiConsumer<ServerPlayer, @Nullable List<LivingEntity>> onEntityHit, @Nullable MovementResultHandler onWallHit) {
        this(taskId, startPosition, maxDistance, timeoutTicks, onEntityHit, onWallHit, null);
    }

    public AwaitingClientResultTask(UUID taskId, Vec3 startPosition, double maxDistance, int timeoutTicks,
            BiConsumer<ServerPlayer, @Nullable List<LivingEntity>> onEntityHit, @Nullable MovementResultHandler onWallHit,
            @Nullable Consumer<ServerPlayer> onTerminate) {
        this.taskId = taskId;
        this.startPosition = startPosition;
        this.maxDistance = maxDistance;
        this.onEntityHit = onEntityHit;
        this.onWallHit = onWallHit;
        this.onTerminate = onTerminate;
        this.ticksRemaining = Math.max(1, timeoutTicks);
    }

    @Override
    public boolean tick(LivingEntity entity, ServerLevel level) {
        if (--ticksRemaining <= 0) {
            if (entity instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new ClientboundStopMovementPayload(
                        taskId, false, player.position()));
                terminate(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onCancelled(LivingEntity entity, MovementTaskCancelReason reason) {
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ClientboundStopMovementPayload(
                    taskId, false, player.position()));
            terminate(player);
        }
    }

    private void terminate(ServerPlayer player) {
        if (onTerminate != null) onTerminate.accept(player);
    }

    public UUID taskId() {
        return taskId;
    }

    public Vec3 startPosition() {
        return startPosition;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public void handleResult(ServerPlayer player, List<LivingEntity> validEntities, boolean wallHit,
            Vec3 claimedPosition, Vec3 facing, int elapsedTicks) {
        if (!validEntities.isEmpty() && onEntityHit != null) {
            onEntityHit.accept(player, validEntities);
        } else if (wallHit && onWallHit != null) {
            onWallHit.accept(player, new MovementResultContext(claimedPosition, facing, wallHit, elapsedTicks));
        }
        terminate(player);
    }

    public record MovementResultContext(Vec3 claimedPosition, Vec3 facing, boolean wallHit, int elapsedTicks) {}

    @FunctionalInterface
    public interface MovementResultHandler {
        void accept(ServerPlayer player, MovementResultContext context);
    }
}
