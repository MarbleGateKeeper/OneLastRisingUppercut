package dev.marblegate.olru.client.movement.task;

public record ClientMovementRuntimeData(Type type) {
    public static ClientMovementRuntimeData jumpPressed() {
        return new ClientMovementRuntimeData(Type.JUMP_PRESSED);
    }

    public enum Type {
        JUMP_PRESSED
    }
}
