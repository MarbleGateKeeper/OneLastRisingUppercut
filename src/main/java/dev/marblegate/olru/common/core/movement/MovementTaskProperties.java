package dev.marblegate.olru.common.core.movement;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public record MovementTaskProperties(
        MovementTaskCategory category,
        int priority,
        @Nullable UUID ownerUuid,
        boolean protectedFromExternalReplacement,
        boolean protectedFromExternalCancel) {

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_PLAYER_ACTIVE = 50;
    public static final int PRIORITY_EXTERNAL_KNOCKBACK = 80;
    public static final int PRIORITY_ULTIMATE = 100;
    public static MovementTaskProperties defaults() {
        return new MovementTaskProperties(MovementTaskCategory.DEFAULT, PRIORITY_DEFAULT, null, false, false);
    }

    public static MovementTaskProperties playerActive(UUID ownerUuid) {
        return new MovementTaskProperties(
                MovementTaskCategory.PLAYER_ACTIVE, PRIORITY_PLAYER_ACTIVE, ownerUuid, false, false);
    }

    public static MovementTaskProperties externalKnockback(@Nullable UUID ownerUuid) {
        return new MovementTaskProperties(
                MovementTaskCategory.EXTERNAL_KNOCKBACK, PRIORITY_EXTERNAL_KNOCKBACK, ownerUuid, false, false);
    }

    public static MovementTaskProperties meteorStrike(UUID ownerUuid) {
        return new MovementTaskProperties(
                MovementTaskCategory.ULTIMATE, PRIORITY_ULTIMATE, ownerUuid, true, true);
    }

    public MovementTaskProperties awaitingClientResult() {
        return new MovementTaskProperties(
                MovementTaskCategory.AWAITING_CLIENT_RESULT,
                priority,
                ownerUuid,
                protectedFromExternalReplacement,
                protectedFromExternalCancel);
    }
}
