package dev.marblegate.olru.client.movement;

import dev.marblegate.olru.client.effect.ClientCameraEffects;
import dev.marblegate.olru.client.movement.task.ClientEntityPushTask;
import dev.marblegate.olru.client.movement.task.ClientMovementRuntimeData;
import dev.marblegate.olru.client.movement.task.ClientMovementTask;
import dev.marblegate.olru.client.movement.task.ClientMovementTask.RuntimeDataResult;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ClientMovementManager {
    private static @Nullable UUID activeTaskId = null;
    private static @Nullable ClientMovementTask activeTask = null;

    public static void start(UUID taskId, ClientMovementTask task) {
        Minecraft mc = Minecraft.getInstance();
        if (activeTask != null && mc.player != null) activeTask.onCancelled(mc.player);
        activeTaskId = taskId;
        activeTask = task;
        // Entity push covers Rocket Punch victims and the Rising Uppercut launch
        if (task instanceof ClientEntityPushTask) ClientCameraEffects.shake(0.5f);
    }

    public static void stopAndCorrect(UUID taskId, boolean force, Vec3 correctionPosition) {
        if (!force && (activeTaskId == null || !activeTaskId.equals(taskId))) return;

        Minecraft mc = Minecraft.getInstance();
        if (activeTask != null && mc.player != null) activeTask.onCancelled(mc.player);
        activeTaskId = null;
        activeTask = null;
        ClientMovementInteractionState.clear();
        if (mc.player != null) {
            mc.player.setPosRaw(correctionPosition.x, correctionPosition.y, correctionPosition.z);
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static boolean hasTask() {
        return activeTask != null;
    }

    public static boolean isActiveTask(Class<? extends ClientMovementTask> taskClass) {
        return activeTask != null && taskClass.isInstance(activeTask);
    }

    public static boolean submitRuntimeData(ClientMovementRuntimeData data) {
        if (activeTask == null) return false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return false;

        RuntimeDataResult result = activeTask.onRuntimeData(player, level, data);
        if (result == RuntimeDataResult.COMPLETE) {
            activeTaskId = null;
            activeTask = null;
            return true;
        }
        return result == RuntimeDataResult.CONSUMED;
    }

    public static void tick() {
        if (activeTask == null) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            activeTaskId = null;
            activeTask = null;
            return;
        }
        if (activeTask.tick(player, level)) {
            activeTaskId = null;
            activeTask = null;
        }
    }
}
