package dev.marblegate.olru.common.core.movement;

public enum MovementTaskCancelReason {
    REPLACED,
    EXTERNAL_REQUEST,
    INTERNAL_EXPLICIT,
    SERVER_REJECTED_CLIENT_RESULT,
    CLIENT_TIMEOUT,
    ENTITY_LEFT_LEVEL,
    ENTITY_DIED;

    public boolean mayCancelProtectedTask() {
        return switch (this) {
            case INTERNAL_EXPLICIT, SERVER_REJECTED_CLIENT_RESULT, CLIENT_TIMEOUT, ENTITY_LEFT_LEVEL, ENTITY_DIED -> true;
            default -> false;
        };
    }
}
