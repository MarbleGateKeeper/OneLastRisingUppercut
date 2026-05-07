package dev.marblegate.olru.common.core.movement;

public enum MovementTaskAssignmentResult {
    ASSIGNED,
    REPLACED,
    REJECTED_PROTECTED,
    REJECTED_LOWER_PRIORITY;

    public boolean accepted() {
        return this == ASSIGNED || this == REPLACED;
    }
}
