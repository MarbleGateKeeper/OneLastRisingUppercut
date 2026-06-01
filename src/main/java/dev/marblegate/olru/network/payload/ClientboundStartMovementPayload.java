package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.movement.task.ClientEntityPushTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorFallTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorHoverTask;
import dev.marblegate.olru.client.movement.task.ClientRocketPunchTask;
import dev.marblegate.olru.client.movement.task.ClientSeismicSlamTask;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.core.movement.task.MovementTaskType;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundStartMovementPayload(
        UUID taskId,
        MovementTaskType taskType,
        Vec3 velocity,
        double maxDistance,
        float collisionDamage,
        boolean preserveEndVelocity) implements CustomPacketPayload {

    public static final Type<ClientboundStartMovementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "start_movement"));

    public static final StreamCodec<ByteBuf, ClientboundStartMovementPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, payload.taskId());
                MovementTaskType.STREAM_CODEC.encode(buf, payload.taskType());
                Vec3.STREAM_CODEC.encode(buf, payload.velocity());
                buf.writeDouble(payload.maxDistance());
                buf.writeFloat(payload.collisionDamage());
                buf.writeBoolean(payload.preserveEndVelocity());
            },
            buf -> new ClientboundStartMovementPayload(
                    UUIDUtil.STREAM_CODEC.decode(buf),
                    MovementTaskType.STREAM_CODEC.decode(buf),
                    Vec3.STREAM_CODEC.decode(buf),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readBoolean()));
    public static void handle(ClientboundStartMovementPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientMovementManager.start(payload.taskId(), switch (payload.taskType()) {
                case ROCKET_PUNCH -> new ClientRocketPunchTask(payload.taskId(), payload.velocity(), payload.maxDistance());
                case ENTITY_PUSH -> new ClientEntityPushTask(
                        payload.taskId(), payload.velocity(), payload.maxDistance(), payload.preserveEndVelocity());
                case SEISMIC_SLAM -> new ClientSeismicSlamTask(
                        payload.taskId(), payload.velocity(), payload.maxDistance(), payload.collisionDamage());
                case METEOR_HOVER -> new ClientMeteorHoverTask(
                        payload.taskId(), payload.velocity().y(), payload.velocity().x(), payload.maxDistance());
                case METEOR_FALL -> new ClientMeteorFallTask(payload.taskId(), payload.velocity(), payload.maxDistance());
            });
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
