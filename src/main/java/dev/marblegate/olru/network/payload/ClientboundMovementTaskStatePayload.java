package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.client.movement.ClientMovementInteractionState;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundMovementTaskStatePayload(
        StateType stateType,
        boolean active) implements CustomPacketPayload {
    public enum StateType {
        METEOR_STRIKE_HOVER;

        private static final StateType[] VALUES = values();

        public static StateType byOrdinal(byte ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                throw new IllegalArgumentException("Unknown movement task state ordinal: " + ordinal);
            }
            return VALUES[ordinal];
        }

        public static final StreamCodec<ByteBuf, StateType> STREAM_CODEC = ByteBufCodecs.BYTE
                .map(StateType::byOrdinal, t -> (byte) t.ordinal());
    }

    public static final Type<ClientboundMovementTaskStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "movement_task_state"));

    public static final StreamCodec<ByteBuf, ClientboundMovementTaskStatePayload> STREAM_CODEC = StreamCodec.composite(
            StateType.STREAM_CODEC, ClientboundMovementTaskStatePayload::stateType,
            ByteBufCodecs.BOOL, ClientboundMovementTaskStatePayload::active,
            ClientboundMovementTaskStatePayload::new);

    public static ClientboundMovementTaskStatePayload meteorStrikeHover(boolean active) {
        return new ClientboundMovementTaskStatePayload(StateType.METEOR_STRIKE_HOVER, active);
    }

    public static void handle(ClientboundMovementTaskStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (payload.stateType() == StateType.METEOR_STRIKE_HOVER) {
                ClientMovementInteractionState.setMeteorStrikeHoverActive(payload.active());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
