package dev.marblegate.olru.common.attachment;

import com.mojang.serialization.Codec;
import dev.marblegate.olru.common.attachment.skill.SkillState;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;

public class GauntletSkillGroup {
    private final SkillState[] states;
    boolean configBound;

    public GauntletSkillGroup(SkillState... states) {
        if (states.length != SkillType.values().length) {
            throw new IllegalArgumentException("Expected " + SkillType.values().length + " skill states, got " + states.length);
        }
        this.states = states;
        this.configBound = true;
    }

    private GauntletSkillGroup(SkillState[] states, boolean configBound) {
        if (states.length != SkillType.values().length) {
            throw new IllegalArgumentException("Expected " + SkillType.values().length + " skill states, got " + states.length);
        }
        this.states = states;
        this.configBound = configBound;
    }

    static GauntletSkillGroup fromDecode(SkillState[] states) {
        return new GauntletSkillGroup(states, false);
    }

    public SkillState get(SkillType type) {
        return states[type.ordinal()];
    }

    public void tick() {
        if (!configBound) return;
        for (SkillState s : states) s.tick();
    }

    public boolean isConfigBound() {
        return configBound;
    }

    public void transferRuntimeFrom(GauntletSkillGroup source) {
        for (SkillType type : SkillType.values()) {
            SkillState targetState = states[type.ordinal()];
            SkillState sourceState = source.states[type.ordinal()];
            if (targetState.stateType() == sourceState.stateType()) {
                targetState.transferRuntimeFrom(sourceState);
            }
        }
    }

    public static final Codec<GauntletSkillGroup> CODEC = SkillState.CODEC
            .listOf()
            .xmap(
                    list -> GauntletSkillGroup.fromDecode(list.toArray(SkillState[]::new)),
                    group -> List.of(group.states));

    public static final StreamCodec<ByteBuf, GauntletSkillGroup> STREAM_CODEC = StreamCodec.of(
            (buf, group) -> {
                for (SkillType type : SkillType.values()) {
                    SkillState.STREAM_CODEC.encode(buf, group.get(type));
                }
            },
            buf -> {
                SkillState[] s = new SkillState[SkillType.values().length];
                for (int i = 0; i < s.length; i++) s[i] = SkillState.STREAM_CODEC.decode(buf);
                return GauntletSkillGroup.fromDecode(s);
            });
}
