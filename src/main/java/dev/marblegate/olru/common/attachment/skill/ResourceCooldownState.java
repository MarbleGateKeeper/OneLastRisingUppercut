package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.IntSupplier;

/**
 * A resource bar (0..1) that can also be put on a timed cooldown: while the cooldown runs the
 * display switches to the cooldown sweep, otherwise it shows the resource ring. Used by The
 * Axiom's Experimental Barrier, where the resource is barrier durability and the cooldown is the
 * recall/broken redeploy block. {@code cdTotal} is null on decoded copies, like
 * {@link CooldownSkillState}.
 */
public class ResourceCooldownState implements SkillState {
    private float progress; // 0..1
    private int cdRemaining;
    private IntSupplier cdTotal;

    public ResourceCooldownState(IntSupplier cdTotal, float progress) {
        this.cdTotal = cdTotal;
        this.progress = Math.clamp(progress, 0f, 1f);
        this.cdRemaining = 0;
    }

    private ResourceCooldownState(float progress, int cdRemaining) {
        this.progress = Math.clamp(progress, 0f, 1f);
        this.cdRemaining = cdRemaining;
    }

    @Override
    public void tick() {
        if (cdRemaining > 0) cdRemaining--;
    }

    @Override
    public boolean isUsable() {
        return cdRemaining == 0 && progress > 0f;
    }

    @Override
    public void consume() {
        if (cdTotal == null) return;
        cdRemaining = cdTotal.getAsInt();
    }

    /** Starts a cooldown capped at the full one (e.g. the shorter recall cooldown). */
    public void consume(int ticks) {
        if (cdTotal == null) return;
        cdRemaining = Math.min(ticks, cdTotal.getAsInt());
    }

    public float getProgress() {
        return progress;
    }

    public int getCdRemaining() {
        return cdRemaining;
    }

    public void setProgress(float progress) {
        this.progress = Math.clamp(progress, 0f, 1f);
    }

    public void addProgress(float delta) {
        setProgress(progress + delta);
    }

    @Override
    public SkillDisplayData displayData() {
        if (cdRemaining > 0) {
            if (cdTotal == null) return SkillDisplayData.unboundPlaceholder(SkillStateType.COOLDOWN);
            int total = cdTotal.getAsInt();
            return new SkillDisplayData(
                    SkillStateType.COOLDOWN,
                    total > 0 ? (float) cdRemaining / total : 0f,
                    0, 0, false, cdRemaining, total);
        }
        // cdFraction = 1 - progress so that 0 = full/ready matches other modes
        return new SkillDisplayData(
                SkillStateType.CONDITIONAL,
                1f - progress,
                0, 0, isUsable(), 0, 0);
    }

    @Override
    public SkillStateType stateType() {
        return SkillStateType.RESOURCE_COOLDOWN;
    }

    @Override
    public void transferRuntimeFrom(SkillState source) {
        ResourceCooldownState s = (ResourceCooldownState) source;
        this.progress = s.progress;
        this.cdRemaining = s.cdRemaining;
    }

    @Override
    public void encodeNetwork(ByteBuf buf) {
        buf.writeFloat(progress);
        buf.writeInt(cdRemaining);
    }

    public static ResourceCooldownState decodeNetwork(ByteBuf buf) {
        return new ResourceCooldownState(buf.readFloat(), buf.readInt());
    }

    public static final MapCodec<ResourceCooldownState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("p").forGetter(s -> s.progress),
            Codec.INT.fieldOf("r").forGetter(s -> s.cdRemaining))
            .apply(i, ResourceCooldownState::new));
}
