package dev.marblegate.olru.common.core.movement;

import dev.marblegate.olru.common.core.movement.task.MovementTask;
import java.util.UUID;

public record MovementTaskEntry(UUID taskId, MovementTask task, MovementTaskProperties properties) {}
