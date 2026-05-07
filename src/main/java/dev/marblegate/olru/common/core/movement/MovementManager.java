package dev.marblegate.olru.common.core.movement;

import dev.marblegate.olru.common.core.movement.task.MovementTask;
import dev.marblegate.olru.common.core.movement.task.MovementTaskActionResult;
import dev.marblegate.olru.common.core.movement.task.MovementTaskActionType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

public class MovementManager {
    private static final Map<UUID, MovementTaskEntry> tasks = new HashMap<>();

    public static MovementTaskAssignmentResult assign(LivingEntity entity, MovementTask task) {
        return assign(entity, task, MovementTaskProperties.defaults());
    }

    public static MovementTaskAssignmentResult assign(
            LivingEntity entity, MovementTask task, MovementTaskProperties properties) {
        MovementTaskEntry existing = tasks.get(entity.getUUID());
        if (existing != null) {
            if (existing.properties().protectedFromExternalReplacement()) {
                return MovementTaskAssignmentResult.REJECTED_PROTECTED;
            }
            if (properties.priority() < existing.properties().priority()) {
                return MovementTaskAssignmentResult.REJECTED_LOWER_PRIORITY;
            }
            existing.task().onCancelled(entity, MovementTaskCancelReason.REPLACED);
        }

        tasks.put(entity.getUUID(), new MovementTaskEntry(UUID.randomUUID(), task, properties));
        return existing == null ? MovementTaskAssignmentResult.ASSIGNED : MovementTaskAssignmentResult.REPLACED;
    }

    public static void switchTo(LivingEntity entity, MovementTask task) {
        MovementTaskEntry existing = tasks.get(entity.getUUID());
        if (existing == null) {
            tasks.put(entity.getUUID(), new MovementTaskEntry(
                    UUID.randomUUID(), task, MovementTaskProperties.defaults()));
            return;
        }
        tasks.put(entity.getUUID(), new MovementTaskEntry(
                existing.taskId(), task, existing.properties().awaitingClientResult()));
    }

    public static boolean cancel(LivingEntity entity) {
        return cancel(entity, MovementTaskCancelReason.EXTERNAL_REQUEST);
    }

    public static boolean cancel(LivingEntity entity, MovementTaskCancelReason reason) {
        MovementTaskEntry existing = tasks.get(entity.getUUID());
        if (existing == null) return false;
        if (existing.properties().protectedFromExternalCancel() && !reason.mayCancelProtectedTask()) {
            return false;
        }

        tasks.remove(entity.getUUID());
        existing.task().onCancelled(entity, reason);
        return true;
    }

    public static boolean complete(LivingEntity entity) {
        return tasks.remove(entity.getUUID()) != null;
    }

    public static boolean hasTask(LivingEntity entity) {
        return tasks.containsKey(entity.getUUID());
    }

    public static @Nullable MovementTask getTask(LivingEntity entity) {
        MovementTaskEntry entry = tasks.get(entity.getUUID());
        return entry != null ? entry.task() : null;
    }

    public static @Nullable UUID getTaskId(LivingEntity entity) {
        MovementTaskEntry entry = tasks.get(entity.getUUID());
        return entry != null ? entry.taskId() : null;
    }

    public static @Nullable MovementTaskProperties getProperties(LivingEntity entity) {
        MovementTaskEntry entry = tasks.get(entity.getUUID());
        return entry != null ? entry.properties() : null;
    }

    public static boolean submitRuntimeAction(LivingEntity entity, MovementTaskActionType actionType) {
        MovementTaskEntry entry = tasks.get(entity.getUUID());
        if (entry == null) return false;
        if (!(entity.level() instanceof ServerLevel level)) return false;

        MovementTaskActionResult result = entry.task().onRuntimeAction(entity, level, actionType);
        if (result == MovementTaskActionResult.COMPLETE) {
            complete(entity);
        } else if (result == MovementTaskActionResult.CANCEL) {
            cancel(entity, MovementTaskCancelReason.INTERNAL_EXPLICIT);
        }
        return result != MovementTaskActionResult.IGNORED;
    }

    private static void tickEntity(LivingEntity entity, ServerLevel level) {
        MovementTaskEntry before = tasks.get(entity.getUUID());
        if (before == null) return;
        boolean done = before.task().tick(entity, level);
        MovementTaskEntry after = tasks.get(entity.getUUID());
        if (done && after == before) tasks.remove(entity.getUUID());
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof LivingEntity living)) return;
        cancel(living, MovementTaskCancelReason.ENTITY_LEFT_LEVEL);
    }

    public static void onEntityDeath(LivingDeathEvent event) {
        cancel(event.getEntity(), MovementTaskCancelReason.ENTITY_DIED); // good it only triggers on server
    }

    public static void tick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (!MovementManager.hasTask(living)) return;
        MovementManager.tickEntity(living, (ServerLevel) living.level());
    }
}
