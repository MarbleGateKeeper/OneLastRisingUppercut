package dev.marblegate.olru.common.core.movement.task;

import dev.marblegate.olru.common.core.movement.MovementTaskCancelReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface MovementTask {
    boolean tick(LivingEntity entity, ServerLevel level);

    default MovementTaskActionResult onRuntimeAction(
            LivingEntity entity, ServerLevel level, MovementTaskActionType actionType) {
        return MovementTaskActionResult.IGNORED;
    }

    default void onCancelled(LivingEntity entity, MovementTaskCancelReason reason) {
        onCancelled(entity);
    }

    default void onCancelled(LivingEntity entity) {}
}
