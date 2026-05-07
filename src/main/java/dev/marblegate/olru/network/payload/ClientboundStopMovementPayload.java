package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundStopMovementPayload(UUID taskId, boolean force, Vec3 correctionPosition) implements CustomPacketPayload {

    public static final Type<ClientboundStopMovementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "stop_movement"));

    public static final StreamCodec<ByteBuf, ClientboundStopMovementPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ClientboundStopMovementPayload::taskId,
            ByteBufCodecs.BOOL, ClientboundStopMovementPayload::force,
            Vec3.STREAM_CODEC, ClientboundStopMovementPayload::correctionPosition,
            ClientboundStopMovementPayload::new);
    public static void handle(ClientboundStopMovementPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientMovementManager.stopAndCorrect(
                payload.taskId(), payload.force(), payload.correctionPosition()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
