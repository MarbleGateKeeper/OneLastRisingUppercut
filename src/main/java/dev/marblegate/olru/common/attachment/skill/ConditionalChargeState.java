package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

public class ConditionalChargeState implements SkillState {
    private float progress; // 0..1
    private final boolean fullRequired;

    public ConditionalChargeState() {
        this(0f);
    }

    private ConditionalChargeState(float progress) {
        this(progress, true);
    }

    private ConditionalChargeState(float progress, boolean fullRequired) {
        this.progress = Math.clamp(progress, 0f, 1f);
        this.fullRequired = fullRequired;
    }

    /** Resource mode (e.g. biotic energy): usable whenever any progress remains. Runtime-only flag, never serialized. */
    public static ConditionalChargeState resource() {
        return new ConditionalChargeState(0f, false);
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
        return fullRequired ? progress >= 1f : progress > 0f;
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
                0, 0, isUsable(), 0, 0);
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
