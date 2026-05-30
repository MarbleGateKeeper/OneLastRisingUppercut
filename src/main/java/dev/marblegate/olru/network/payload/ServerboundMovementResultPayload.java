package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.core.movement.MovementTaskCancelReason;
import dev.marblegate.olru.common.core.movement.task.AwaitingClientResultTask;
import dev.marblegate.olru.network.codec.OLRUStreamCodecs;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundMovementResultPayload(
        UUID taskId,
        Vec3 claimedPosition,
        List<UUID> hitEntityIds,
        boolean wallHit) implements CustomPacketPayload {

    /** Max drift between claimed position and task start (blocks). Generous for high-latency players. */
    private static final double MAX_POSITION_DRIFT = 8.0;
    /** Max distance for an entity to count as a valid hit, measured from the claimed collision position. */
    private static final double MAX_HIT_DISTANCE = 16.0;
    /** Extra reach used to pull in nearby entities around the collision point when at least one hit was reported. */
    private static final double IMPACT_SPLASH_RADIUS = 2.75;

    public static final Type<ServerboundMovementResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "movement_result"));

    public static final StreamCodec<ByteBuf, ServerboundMovementResultPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ServerboundMovementResultPayload::taskId,
            Vec3.STREAM_CODEC, ServerboundMovementResultPayload::claimedPosition,
            OLRUStreamCodecs.UUID_LIST, ServerboundMovementResultPayload::hitEntityIds,
            ByteBufCodecs.BOOL, ServerboundMovementResultPayload::wallHit,
            ServerboundMovementResultPayload::new);
    public static void handle(ServerboundMovementResultPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            if (!(MovementManager.getTask(player) instanceof AwaitingClientResultTask awaiting)) return;
            if (!awaiting.taskId().equals(payload.taskId())) return;

            Vec3 claimed = payload.claimedPosition();

            if (claimed.distanceTo(awaiting.startPosition()) > awaiting.maxDistance() + MAX_POSITION_DRIFT) {
                MovementManager.cancel(player, MovementTaskCancelReason.SERVER_REJECTED_CLIENT_RESULT);
                return;
            }

            List<UUID> hitIds = payload.hitEntityIds();
            List<LivingEntity> validTargets = new ArrayList<>();
            for (UUID id : hitIds) {
                Entity e = player.level().getEntity(id);
                if (e == null) {
                    e = player.level().getServer().getPlayerList().getPlayer(id);
                }
                if (!(e instanceof LivingEntity living)) continue;
                if (living == player || living.level() != player.level()) continue;
                if (living.position().distanceTo(claimed) > MAX_HIT_DISTANCE) continue;
                if (validTargets.contains(living)) continue;
                validTargets.add(living);
            }

            if (!validTargets.isEmpty()) {
                AABB splashBox = new AABB(
                        claimed.x - IMPACT_SPLASH_RADIUS,
                        claimed.y - 0.75,
                        claimed.z - IMPACT_SPLASH_RADIUS,
                        claimed.x + IMPACT_SPLASH_RADIUS,
                        claimed.y + 0.75,
                        claimed.z + IMPACT_SPLASH_RADIUS);
                for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                        splashBox,
                        e -> e != player && e.isAlive() && (e.isPickable() || e instanceof ServerPlayer) && !e.isSpectator())) {
                    if (nearby.level() != player.level()) continue;
                    if (nearby.position().distanceTo(claimed) > MAX_HIT_DISTANCE) continue;
                    if (validTargets.contains(nearby)) continue;
                    validTargets.add(nearby);
                }
            }

            awaiting.handleResult(player, validTargets, payload.wallHit());
            MovementManager.complete(player);

            // Accept claimed position to keep server in sync
            player.teleportTo(claimed.x, claimed.y, claimed.z);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
