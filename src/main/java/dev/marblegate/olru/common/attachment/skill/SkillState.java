package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface SkillState {
    void tick();

    boolean isUsable();

    void consume();

    SkillDisplayData displayData();

    SkillStateType stateType();

    void encodeNetwork(ByteBuf buf);

    default void transferRuntimeFrom(SkillState source) {}

    Codec<SkillState> CODEC = SkillStateType.CODEC.dispatch(
            SkillState::stateType,
            SkillStateType::mapCodec);

    StreamCodec<ByteBuf, SkillState> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                SkillStateType.STREAM_CODEC.encode(buf, state.stateType());
                state.encodeNetwork(buf);
            },
            buf -> {
                SkillStateType type = SkillStateType.STREAM_CODEC.decode(buf);
                return switch (type) {
                    case COOLDOWN -> CooldownSkillState.decodeNetwork(buf);
                    case INCREMENTAL_CHARGE -> IncrementalChargeState.decodeNetwork(buf);
                    case FULL_CHARGE -> FullChargeState.decodeNetwork(buf);
                    case CONDITIONAL -> ConditionalChargeState.decodeNetwork(buf);
                    case RESOURCE_COOLDOWN -> ResourceCooldownState.decodeNetwork(buf);
                };
            });
}
