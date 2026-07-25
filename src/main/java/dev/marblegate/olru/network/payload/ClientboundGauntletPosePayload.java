package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.client.animation.ClientGauntletAnimations;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.animation.GauntletPoseType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundGauntletPosePayload(
        int entityId,
        GauntletPoseType pose,
        float param,
        int durationTicks,
        boolean active) implements CustomPacketPayload {

    public static final Type<ClientboundGauntletPosePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gauntlet_pose"));

    public static final StreamCodec<ByteBuf, ClientboundGauntletPosePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                GauntletPoseType.STREAM_CODEC.encode(buf, payload.pose());
                buf.writeFloat(payload.param());
                buf.writeInt(payload.durationTicks());
                buf.writeBoolean(payload.active());
            },
            buf -> new ClientboundGauntletPosePayload(
                    buf.readInt(),
                    GauntletPoseType.STREAM_CODEC.decode(buf),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readBoolean()));
    public static void handle(ClientboundGauntletPosePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientGauntletAnimations.handle(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
