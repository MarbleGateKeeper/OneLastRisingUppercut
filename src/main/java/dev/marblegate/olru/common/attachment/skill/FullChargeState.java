package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.IntSupplier;

public class FullChargeState implements SkillState {
    private int cdRemaining;
    private int currentCharges;
    private IntSupplier cdTotal;
    private IntSupplier maxCharges;

    public FullChargeState(IntSupplier cdTotal, IntSupplier maxCharges) {
        this.cdTotal = cdTotal;
        this.maxCharges = maxCharges;
        this.currentCharges = 0;
        this.cdRemaining = 0;
    }

    private FullChargeState(int cdRemaining, int currentCharges) {
        this.cdRemaining = cdRemaining;
        this.currentCharges = currentCharges;
    }

    @Override
    public void tick() {
        int max = maxCharges.getAsInt();
        if (currentCharges >= max) return;
        int total = cdTotal.getAsInt();
        if (total <= 0) {
            currentCharges = max;
            return;
        }
        if (--cdRemaining <= 0) {
            currentCharges = max;
            cdRemaining = 0;
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
            return SkillDisplayData.unboundPlaceholder(SkillStateType.FULL_CHARGE);
        int max = maxCharges.getAsInt();
        int total = cdTotal.getAsInt();
        float fraction = (currentCharges < max && total > 0)
                ? (float) cdRemaining / total
                : 0f;
        return new SkillDisplayData(
                SkillStateType.FULL_CHARGE, fraction,
                currentCharges, max, isUsable(), cdRemaining, total);
    }

    @Override
    public SkillStateType stateType() {
        return SkillStateType.FULL_CHARGE;
    }

    @Override
    public void transferRuntimeFrom(SkillState source) {
        FullChargeState s = (FullChargeState) source;
        this.cdRemaining = s.cdRemaining;
        this.currentCharges = s.currentCharges;
    }

    @Override
    public void encodeNetwork(ByteBuf buf) {
        buf.writeInt(cdRemaining);
        buf.writeInt(currentCharges);
    }

    public static FullChargeState decodeNetwork(ByteBuf buf) {
        return new FullChargeState(buf.readInt(), buf.readInt());
    }

    public static final MapCodec<FullChargeState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("r").forGetter(s -> s.cdRemaining),
            Codec.INT.fieldOf("c").forGetter(s -> s.currentCharges))
            .apply(i, FullChargeState::new));
}
