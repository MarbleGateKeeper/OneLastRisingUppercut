package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.core.movement.task.MovementTaskActionType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundMovementTaskActionPayload(
        MovementTaskActionType actionType) implements CustomPacketPayload {
    public static final Type<ServerboundMovementTaskActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "movement_task_action"));

    public static final StreamCodec<ByteBuf, ServerboundMovementTaskActionPayload> STREAM_CODEC = StreamCodec.composite(
            MovementTaskActionType.STREAM_CODEC, ServerboundMovementTaskActionPayload::actionType,
            ServerboundMovementTaskActionPayload::new);

    public static ServerboundMovementTaskActionPayload primaryAttackPressed() {
        return new ServerboundMovementTaskActionPayload(MovementTaskActionType.PRIMARY_ATTACK_PRESSED);
    }

    public static void handle(ServerboundMovementTaskActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            MovementManager.submitRuntimeAction(player, payload.actionType());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
