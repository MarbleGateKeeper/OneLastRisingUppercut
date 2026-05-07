package dev.marblegate.olru.common.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class GauntletEntityState {
    final Map<Identifier, GauntletSkillGroup> groups;

    public GauntletEntityState() {
        this.groups = new HashMap<>();
    }

    GauntletEntityState(Map<Identifier, GauntletSkillGroup> groups) {
        this.groups = new HashMap<>(groups);
    }

    public GroupAccess getOrCreate(Identifier id, Supplier<GauntletSkillGroup> factory) {
        boolean created = !groups.containsKey(id);
        GauntletSkillGroup group = groups.computeIfAbsent(id, k -> factory.get());
        if (!created && !group.isConfigBound()) {
            GauntletSkillGroup fresh = factory.get();
            fresh.transferRuntimeFrom(group);
            groups.put(id, fresh);
            group = fresh;
        }
        return new GroupAccess(group, created);
    }

    public @Nullable GauntletSkillGroup get(Identifier id) {
        return groups.get(id);
    }

    public boolean ensureConfigBound(Identifier id, Supplier<GauntletSkillGroup> factory, boolean createIfMissing) {
        GauntletSkillGroup group = groups.get(id);
        if (group == null) {
            if (!createIfMissing) return false;
            groups.put(id, factory.get());
            return true;
        }

        if (group.isConfigBound()) return false;
        GauntletSkillGroup fresh = factory.get();
        fresh.transferRuntimeFrom(group);
        groups.put(id, fresh);
        return true;
    }

    public void tick() {
        groups.values().forEach(GauntletSkillGroup::tick);
    }

    public record GroupAccess(GauntletSkillGroup group, boolean wasCreated) {}

    public static final MapCodec<GauntletEntityState> CODEC = Codec.unboundedMap(Identifier.CODEC, GauntletSkillGroup.CODEC)
            .fieldOf("groups")
            .xmap(GauntletEntityState::new, s -> s.groups);

    public static final StreamCodec<ByteBuf, GauntletEntityState> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                buf.writeInt(state.groups.size());
                for (var entry : state.groups.entrySet()) {
                    Identifier.STREAM_CODEC.encode(buf, entry.getKey());
                    GauntletSkillGroup.STREAM_CODEC.encode(buf, entry.getValue());
                }
            },
            buf -> {
                int n = buf.readInt();
                Map<Identifier, GauntletSkillGroup> map = new HashMap<>(n);
                for (int i = 0; i < n; i++) {
                    Identifier id = Identifier.STREAM_CODEC.decode(buf);
                    GauntletSkillGroup group = GauntletSkillGroup.STREAM_CODEC.decode(buf);
                    map.put(id, group);
                }
                return new GauntletEntityState(map);
            });
}
