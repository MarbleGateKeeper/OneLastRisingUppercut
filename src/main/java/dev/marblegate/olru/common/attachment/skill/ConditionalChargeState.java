package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

public class ConditionalChargeState implements SkillState {
    private float progress; // 0..1

    public ConditionalChargeState() {
        this.progress = 0f;
    }

    private ConditionalChargeState(float progress) {
        this.progress = Math.clamp(progress, 0f, 1f);
    }

    public void addProgress(float delta) {
        progress = Math.clamp(progress + delta, 0f, 1f);
    }

    public float getProgress() {
        return progress;
    }

    @Override
    public void tick() {} // no timer

    @Override
    public boolean isUsable() {
        return progress >= 1f;
    }

    @Override
    public void consume() {
        progress = 0f;
    }

    @Override
    public SkillDisplayData displayData() {
        // cdFraction = 1 - progress so that 0 = full/ready matches other modes
        return new SkillDisplayData(
                SkillStateType.CONDITIONAL,
                1f - progress,
                0, 0, isUsable());
    }

    @Override
    public SkillStateType stateType() {
        return SkillStateType.CONDITIONAL;
    }

    @Override
    public void transferRuntimeFrom(SkillState source) {
        this.progress = ((ConditionalChargeState) source).progress;
    }

    @Override
    public void encodeNetwork(ByteBuf buf) {
        buf.writeFloat(progress);
    }

    public static ConditionalChargeState decodeNetwork(ByteBuf buf) {
        return new ConditionalChargeState(buf.readFloat());
    }

    public static final MapCodec<ConditionalChargeState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("p").forGetter(s -> s.progress))
            .apply(i, ConditionalChargeState::new));
}
