package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundGauntletEffectPayload(
        EffectType effectType,
        int sourceEntityId,
        int targetEntityId,
        Vec3 position,
        float primaryValue,
        float secondaryValue,
        int durationTicks,
        boolean active) implements CustomPacketPayload {

    public static final Type<ClientboundGauntletEffectPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gauntlet_effect"));

    public static final StreamCodec<ByteBuf, ClientboundGauntletEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                EffectType.STREAM_CODEC.encode(buf, payload.effectType());
                buf.writeInt(payload.sourceEntityId());
                buf.writeInt(payload.targetEntityId());
                Vec3.STREAM_CODEC.encode(buf, payload.position());
                buf.writeFloat(payload.primaryValue());
                buf.writeFloat(payload.secondaryValue());
                buf.writeInt(payload.durationTicks());
                buf.writeBoolean(payload.active());
            },
            buf -> new ClientboundGauntletEffectPayload(
                    EffectType.STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    buf.readInt(),
                    Vec3.STREAM_CODEC.decode(buf),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readBoolean()));
    public static void handle(ClientboundGauntletEffectPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientGauntletEffects.handle(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum EffectType {
        ROCKET_CHARGE(0),
        METEOR_TARGET(1),
        FIELD_EXTRACTION_BEAM(2),
        SEDATED(3),
        NANO_SURGE(4);

        private final byte networkId;

        EffectType(int networkId) {
            this.networkId = (byte) networkId;
        }

        public byte networkId() {
            return networkId;
        }

        public static EffectType byNetworkId(byte networkId) {
            for (EffectType type : values()) {
                if (type.networkId == networkId) return type;
            }
            throw new IllegalArgumentException("Unknown gauntlet effect type id: " + networkId);
        }

        public static final StreamCodec<ByteBuf, EffectType> STREAM_CODEC = ByteBufCodecs.BYTE.map(EffectType::byNetworkId, EffectType::networkId);
    }
}
