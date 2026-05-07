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

public class ClientMeteorFallTask implements ClientMovementTask {
    private final UUID taskId;
    private final Vec3 velocity;
    private double remainingDistance;

    public ClientMeteorFallTask(UUID taskId, Vec3 velocity, double maxDistance) {
        this.taskId = taskId;
        this.velocity = velocity;
        this.remainingDistance = maxDistance;
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
                    taskId, player.position(), List.of(), true));
            return true;
        }

        player.setDeltaMovement(velocity);
        remainingDistance -= velocity.length();

        if (remainingDistance <= 0) {
            player.setDeltaMovement(Vec3.ZERO);
            ClientPacketDistributor.sendToServer(new ServerboundMovementResultPayload(
                    taskId, player.position(), List.of(), false));
            return true;
        }
        return false;
    }

    @Override
    public void onCancelled(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
    }
}
