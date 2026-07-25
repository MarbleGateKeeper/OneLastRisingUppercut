package dev.marblegate.olru.common.attachment.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntSupplier;

public class CooldownSkillState implements SkillState {
    private int cdRemaining;
    private IntSupplier cdTotal;

    public CooldownSkillState(IntSupplier cdTotal) {
        this.cdTotal = cdTotal;
        this.cdRemaining = 0;
    }

    private CooldownSkillState(int cdRemaining) {
        this.cdRemaining = cdRemaining;
    }

    @Override
    public void tick() {
        if (cdRemaining > 0) cdRemaining--;
    }

    @Override
    public boolean isUsable() {
        return cdRemaining == 0;
    }

    @Override
    public void consume() {
        if (cdTotal == null) return;
        cdRemaining = cdTotal.getAsInt();
    }

    @Override
    public SkillDisplayData displayData() {
        if (cdTotal == null) return SkillDisplayData.unboundPlaceholder(SkillStateType.COOLDOWN);
        int total = cdTotal.getAsInt();
        return new SkillDisplayData(
                SkillStateType.COOLDOWN,
                total > 0 ? (float) cdRemaining / total : 0f,
                0, 0, isUsable(), cdRemaining, total);
    }

    @Override
    public SkillStateType stateType() {
        return SkillStateType.COOLDOWN;
    }

    @Override
    public void transferRuntimeFrom(SkillState source) {
        this.cdRemaining = ((CooldownSkillState) source).cdRemaining;
    }

    @Override
    public void encodeNetwork(ByteBuf buf) {
        buf.writeInt(cdRemaining);
    }

    public static CooldownSkillState decodeNetwork(ByteBuf buf) {
        return new CooldownSkillState(buf.readInt());
    }

    public static final MapCodec<CooldownSkillState> MAP_CODEC = Codec.INT.fieldOf("r").xmap(CooldownSkillState::new, s -> s.cdRemaining);
}
