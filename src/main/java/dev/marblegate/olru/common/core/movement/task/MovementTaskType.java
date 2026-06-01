package dev.marblegate.olru.common.core.movement.task;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum MovementTaskType {
    ROCKET_PUNCH,
    ENTITY_PUSH,
    SEISMIC_SLAM,
    METEOR_FALL,
    METEOR_HOVER;

    private static final MovementTaskType[] VALUES = values();

    public static MovementTaskType byOrdinal(byte ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            throw new IllegalArgumentException("Unknown movement task type ordinal: " + ordinal);
        }
        return VALUES[ordinal];
    }

    public static final StreamCodec<ByteBuf, MovementTaskType> STREAM_CODEC = ByteBufCodecs.BYTE.map(MovementTaskType::byOrdinal, t -> (byte) t.ordinal());
}
