package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum SkillStateType {
    COOLDOWN,
    INCREMENTAL_CHARGE,
    FULL_CHARGE,
    CONDITIONAL;

    private static final SkillStateType[] VALUES = values();

    public static final Codec<SkillStateType> CODEC = Codec.STRING.xmap(s -> SkillStateType.valueOf(s.toUpperCase()), t -> t.name().toLowerCase());

    public static SkillStateType byOrdinal(byte ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            throw new IllegalArgumentException("Unknown skill state type ordinal: " + ordinal);
        }
        return VALUES[ordinal];
    }

    public static final StreamCodec<ByteBuf, SkillStateType> STREAM_CODEC = ByteBufCodecs.BYTE.map(SkillStateType::byOrdinal, t -> (byte) t.ordinal());

    public MapCodec<? extends SkillState> mapCodec() {
        return switch (this) {
            case COOLDOWN -> CooldownSkillState.MAP_CODEC;
            case INCREMENTAL_CHARGE -> IncrementalChargeState.MAP_CODEC;
            case FULL_CHARGE -> FullChargeState.MAP_CODEC;
            case CONDITIONAL -> ConditionalChargeState.MAP_CODEC;
        };
    }
}
