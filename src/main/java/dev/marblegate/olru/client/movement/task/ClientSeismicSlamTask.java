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

public class ClientSeismicSlamTask implements ClientMovementTask {
    private final UUID taskId;
    private final double gravity;
    private int ticksRemaining;
    private int elapsedTicks;
    private Vec3 velocity;
    private boolean descending;

    public ClientSeismicSlamTask(UUID taskId, Vec3 initialVelocity, double maxTravelTicks, double gravity) {
        this.taskId = taskId;
        this.velocity = initialVelocity;
        this.ticksRemaining = Math.max(1, (int) Math.ceil(maxTravelTicks));
        this.elapsedTicks = 0;
        this.gravity = Math.max(0.01, gravity);
        this.descending = initialVelocity.y <= 0.0;
    }

    @Override
    public boolean tick(LocalPlayer player, ClientLevel level) {
        player.resetFallDistance();
        elapsedTicks++;
        if (ticksRemaining-- <= 0) {
            finish(player, true);
            return true;
        }

        if (velocity.y < 0.0) descending = true;
        SweepResult sweep = SweptCollisionHelper.sweepBlocks(player, level, velocity);
        if (sweep.blockHit()) {
            Vec3 snap = sweep.snapOffset();
            Vec3 pos = player.position();
            player.setPosRaw(pos.x + snap.x, pos.y + snap.y, pos.z + snap.z);
            if (descending) {
                finish(player, true);
                return true;
            }
            velocity = new Vec3(velocity.x * 0.9, -gravity, velocity.z * 0.9);
            player.setDeltaMovement(velocity);
            return false;
        }

        player.setDeltaMovement(velocity);
        if (descending && player.onGround()) {
            finish(player, true);
            return true;
        }

        velocity = new Vec3(velocity.x * 0.985, velocity.y - gravity, velocity.z * 0.985);
        return false;
    }

    @Override
    public void onCancelled(LocalPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
    }

    private void finish(LocalPlayer player, boolean groundImpact) {
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        ClientPacketDistributor.sendToServer(new ServerboundMovementResultPayload(
                taskId, player.position(), ServerboundMovementResultPayload.horizontalFacing(player.getLookAngle()),
                elapsedTicks, List.of(), groundImpact));
    }
}
