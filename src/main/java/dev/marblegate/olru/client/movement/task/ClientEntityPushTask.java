package dev.marblegate.olru.client.movement.task;

import dev.marblegate.olru.common.util.SweptCollisionHelper;
import dev.marblegate.olru.common.util.SweptCollisionHelper.SweepResult;
import dev.marblegate.olru.network.payload.ServerboundMovementResultPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ClientEntityPushTask implements ClientMovementTask {
    private final UUID taskId;
    private final Vec3 velocity;
    private final boolean preserveEndVelocity;
    private double remainingDistance;

    public ClientEntityPushTask(UUID taskId, Vec3 velocity, double maxDistance) {
        this(taskId, velocity, maxDistance, false);
    }

    public ClientEntityPushTask(UUID taskId, Vec3 velocity, double maxDistance, boolean preserveEndVelocity) {
        this.taskId = taskId;
        this.velocity = velocity;
        this.remainingDistance = maxDistance;
        this.preserveEndVelocity = preserveEndVelocity;
    }

    @Override
    public boolean tick(LocalPlayer player, ClientLevel level) {
        SweepResult sweep = SweptCollisionHelper.sweepBlocks(player, level, velocity);

        if (sweep.hasCollision()) {
            Vec3 snap = sweep.snapOffset();
            Vec3 pos = player.position();
            player.setPosRaw(pos.x + snap.x, pos.y + snap.y, pos.z + snap.z);
            player.setDeltaMovement(Vec3.ZERO);
            ClientPacketDistributor.sendToServer(new ServerboundMovementResultPayload(
                    taskId, player.position(), ServerboundMovementResultPayload.horizontalFacing(player.getLookAngle()),
                    List.of(), true));
            return true;
        }

        player.setDeltaMovement(velocity);
        remainingDistance -= velocity.length();

        if (remainingDistance <= 0) {
            if (preserveEndVelocity) {
                player.setDeltaMovement(velocity);
                player.hurtMarked = true;
                player.resetFallDistance();
            } else {
                player.setDeltaMovement(Vec3.ZERO);
            }
            ClientPacketDistributor.sendToServer(new ServerboundMovementResultPayload(
                    taskId, player.position(), ServerboundMovementResultPayload.horizontalFacing(player.getLookAngle()),
                    List.of(), false));
            return true;
        }
        return false;
    }

    @Override
    public void onCancelled(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
    }
}
