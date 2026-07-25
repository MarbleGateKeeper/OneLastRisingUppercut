package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.IntSupplier;

public class IncrementalChargeState implements SkillState {
    private int cdRemaining;
    private int currentCharges;
    private IntSupplier cdTotal;
    private IntSupplier maxCharges;

    public IncrementalChargeState(IntSupplier cdTotal, IntSupplier maxCharges) {
        this.cdTotal = cdTotal;
        this.maxCharges = maxCharges;
        this.currentCharges = 0;
        this.cdRemaining = 0;
    }

    private IncrementalChargeState(int cdRemaining, int currentCharges) {
        this.cdRemaining = cdRemaining;
        this.currentCharges = currentCharges;
    }

    @Override
    public void tick() {
        int max = maxCharges.getAsInt();
        if (currentCharges >= max) return;
        if (--cdRemaining <= 0) {
            currentCharges++;
            int total = cdTotal.getAsInt();
            cdRemaining = currentCharges < max ? total : 0;
        }
    }

    @Override
    public boolean isUsable() {
        return currentCharges > 0;
    }

    @Override
    public void consume() {
        if (currentCharges <= 0) return;
        if (maxCharges == null || cdTotal == null) return;
        boolean wasAtMax = currentCharges >= maxCharges.getAsInt();
        currentCharges--;
        if (wasAtMax) cdRemaining = cdTotal.getAsInt();
    }

    @Override
    public SkillDisplayData displayData() {
        if (cdTotal == null || maxCharges == null)
            return SkillDisplayData.unboundPlaceholder(SkillStateType.INCREMENTAL_CHARGE);
        int max = maxCharges.getAsInt();
        int total = cdTotal.getAsInt();
        float fraction = (currentCharges < max && total > 0)
                ? (float) cdRemaining / total
                : 0f;
        return new SkillDisplayData(
                SkillStateType.INCREMENTAL_CHARGE, fraction,
                currentCharges, max, isUsable(), cdRemaining, total);
    }

    @Override
    public SkillStateType stateType() {
        return SkillStateType.INCREMENTAL_CHARGE;
    }

    @Override
    public void transferRuntimeFrom(SkillState source) {
        IncrementalChargeState s = (IncrementalChargeState) source;
        this.cdRemaining = s.cdRemaining;
        this.currentCharges = s.currentCharges;
    }

    @Override
    public void encodeNetwork(ByteBuf buf) {
        buf.writeInt(cdRemaining);
        buf.writeInt(currentCharges);
    }

    public static IncrementalChargeState decodeNetwork(ByteBuf buf) {
        return new IncrementalChargeState(buf.readInt(), buf.readInt());
    }

    public static final MapCodec<IncrementalChargeState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("r").forGetter(s -> s.cdRemaining),
            Codec.INT.fieldOf("c").forGetter(s -> s.currentCharges))
            .apply(i, IncrementalChargeState::new));
}
