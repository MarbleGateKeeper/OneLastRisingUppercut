package dev.marblegate.olru.client.effect;

import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.movement.task.ClientEntityPushTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorFallTask;
import dev.marblegate.olru.client.movement.task.ClientRocketPunchTask;
import dev.marblegate.olru.client.movement.task.ClientSeismicSlamTask;
import dev.marblegate.olru.config.OLRUClientConfig;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client-side camera feedback: screen shake on heavy impacts and an FOV kick while movement
 * tasks are active. Shake is triggered via {@link #shake(float)} and decays every client tick.
 */
public final class ClientCameraEffects {
    private static final float MAX_SHAKE_POWER = 1.5f;
    private static final float SHAKE_DECAY = 0.88f;
    private static final float SHAKE_CUTOFF = 0.01f;
    private static final float SHAKE_DEGREES = 1.2f;
    private static final float FOV_SMOOTHING = 0.25f;
    private static final float MIN_FOV_BOOST = 0.05f;

    private static float shakePower = 0f;
    private static float fovBoost = 0f;
    private static int clientTicks = 0;

    private ClientCameraEffects() {}

    public static void shake(float intensity) {
        shakePower = Math.clamp(Math.max(shakePower, intensity), 0f, MAX_SHAKE_POWER);
    }

    public static void tick() {
        clientTicks++;
        shakePower *= SHAKE_DECAY;
        if (shakePower < SHAKE_CUTOFF) shakePower = 0f;
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!OLRUClientConfig.CAMERA_SHAKE.get() || shakePower <= 0f) return;
        float time = clientTicks + (float) event.getPartialTick();
        float scale = shakePower * SHAKE_DEGREES;
        event.setPitch(event.getPitch() + noise(time) * scale);
        event.setRoll(event.getRoll() + noise(time + 40f) * scale);
    }

    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!OLRUClientConfig.FOV_KICK.get()) return;
        float targetBoost = 0f;
        if (ClientMovementManager.isActiveTask(ClientRocketPunchTask.class)) targetBoost = Math.max(targetBoost, 10f);
        if (ClientMovementManager.isActiveTask(ClientMeteorFallTask.class)) targetBoost = Math.max(targetBoost, 10f);
        if (ClientMovementManager.isActiveTask(ClientSeismicSlamTask.class)) targetBoost = Math.max(targetBoost, 6f);
        if (ClientMovementManager.isActiveTask(ClientEntityPushTask.class)) targetBoost = Math.max(targetBoost, 5f);
        fovBoost = Mth.lerp(FOV_SMOOTHING, fovBoost, targetBoost);
        if (fovBoost < MIN_FOV_BOOST) return;
        event.setFOV(event.getFOV() + fovBoost);
    }

    private static float noise(float time) {
        return (float) (Math.sin(time * 3.1) * 0.6 + Math.sin(time * 7.7) * 0.4);
    }
}
