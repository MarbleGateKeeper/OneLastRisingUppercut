package dev.marblegate.olru.common.core.movement.task;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum MovementTaskActionType {
    PRIMARY_ATTACK_PRESSED;

    private static final MovementTaskActionType[] VALUES = values();

    public static MovementTaskActionType byOrdinal(byte ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            throw new IllegalArgumentException("Unknown movement task action ordinal: " + ordinal);
        }
        return VALUES[ordinal];
    }

    public static final StreamCodec<ByteBuf, MovementTaskActionType> STREAM_CODEC = ByteBufCodecs.BYTE
            .map(MovementTaskActionType::byOrdinal, t -> (byte) t.ordinal());
}
