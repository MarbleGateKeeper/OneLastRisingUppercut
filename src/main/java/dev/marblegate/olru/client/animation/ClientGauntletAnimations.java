package dev.marblegate.olru.client.animation;

import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.network.payload.ClientboundGauntletPosePayload;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.Nullable;

public final class ClientGauntletAnimations {
    private static final Map<Integer, ActivePose> ACTIVE = new HashMap<>();
    private static final Map<Integer, Snapshot> LAST = new HashMap<>();
    private static final float CROSSFADE_TICKS = 2.0f;
    private static final GauntletPose.PartPose IDENTITY = new GauntletPose.PartPose(0f, 0f, 0f, 0f, 0f, 0f);

    private static int clientTicks = 0;

    private ClientGauntletAnimations() {}

    public static void handle(ClientboundGauntletPosePayload payload) {
        if (payload.active()) {
            GauntletPose pose = GauntletPoses.get(payload.pose());
            if (pose == null) return;
            ActivePose previous = ACTIVE.get(payload.entityId());
            // Keep the original start time on channel refreshes so blend-in is not restarted
            float startTime = previous != null && previous.type() == payload.pose() ? previous.startTime() : clientTicks;
            ACTIVE.put(payload.entityId(), new ActivePose(payload.pose(), pose, startTime, clientTicks, payload.param(), payload.durationTicks()));
        } else {
            ActivePose current = ACTIVE.get(payload.entityId());
            if (current != null && current.type() == payload.pose()) {
                ACTIVE.remove(payload.entityId());
            }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            LAST.clear();
            return;
        }
        clientTicks++;
        Iterator<ActivePose> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            ActivePose active = iterator.next();
            if (active.type() == GauntletPoseType.SEDATED_SLUMP) continue; // locally managed by evaluate
            if (clientTicks - active.refreshTime() >= active.durationTicks()) iterator.remove();
        }
    }

    public static <T extends Avatar & ClientAvatarEntity> void extractInto(T avatar, AvatarRenderState state) {
        if (avatar == null || !avatar.isAlive()) {
            state.setRenderData(GauntletPoseRenderData.KEY, null);
            return;
        }
        Frame frame = evaluateFrame(avatar.getId());
        if (frame == null) {
            state.setRenderData(GauntletPoseRenderData.KEY, null);
            return;
        }
        // Whole-body spin (e.g. Rising Uppercut) rides the entity render rotation, like vanilla spin attacks
        state.bodyRot += frame.bodySpin();
        state.setRenderData(GauntletPoseRenderData.KEY, new GauntletPoseRenderData(frame.parts(), frame.weight()));
    }

    /** Returns the currently active pose type for the given entity, or null when no pose is active. */
    public static @Nullable GauntletPoseType getActivePoseType(int entityId) {
        ActivePose active = ACTIVE.get(entityId);
        return active != null ? active.type() : null;
    }

    /**
     * Evaluates the first-person arm transform for the given entity, using the same time, blending and
     * crossfade logic as the third-person pose evaluation. Returns null when no pose is active.
     */
    public static @Nullable WeightedFp evaluateFp(int entityId) {
        Frame frame = evaluateFrame(entityId);
        return frame != null ? new WeightedFp(frame.fp(), frame.weight()) : null;
    }

    private static @Nullable Frame evaluateFrame(int entityId) {
        float time = clientTicks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        ActivePose active = ACTIVE.get(entityId);
        boolean sedated = ClientGauntletEffects.isSedated(entityId);
        if (active == null && sedated) {
            active = new ActivePose(
                    GauntletPoseType.SEDATED_SLUMP,
                    GauntletPoses.get(GauntletPoseType.SEDATED_SLUMP),
                    time, time, 0f, Integer.MAX_VALUE);
            ACTIVE.put(entityId, active);
        } else if (active != null && active.type() == GauntletPoseType.SEDATED_SLUMP && !sedated) {
            ACTIVE.remove(entityId);
            active = null;
        }

        Evaluation current = active != null ? evaluateActive(active, time) : null;
        Snapshot snapshot = LAST.get(entityId);
        if (current != null) {
            if (snapshot != null && snapshot.type() != active.type() && time - snapshot.time() <= CROSSFADE_TICKS) {
                float t = (time - snapshot.time()) / CROSSFADE_TICKS;
                return new Frame(
                        lerpParts(snapshot.parts(), current.parts(), t),
                        lerpFp(snapshot.fp(), current.fp(), t),
                        Mth.lerp(t, snapshot.weight(), current.weight()),
                        current.bodySpin());
            }
            LAST.put(entityId, new Snapshot(active.type(), current.parts(), current.fp(), current.weight(), time));
            return new Frame(current.parts(), current.fp(), current.weight(), current.bodySpin());
        }
        if (snapshot != null) {
            float elapsed = time - snapshot.time();
            if (elapsed <= CROSSFADE_TICKS) {
                return new Frame(snapshot.parts(), snapshot.fp(), snapshot.weight() * (1.0f - elapsed / CROSSFADE_TICKS), 0f);
            }
            LAST.remove(entityId);
        }
        return null;
    }

    private static Evaluation evaluateActive(ActivePose active, float time) {
        GauntletPose pose = active.pose();
        float age = Math.max(0f, time - active.startTime());
        float weight = smoothstep(age, pose.blendInTicks());
        if (!pose.looping()) {
            weight *= smoothstep(active.durationTicks() - age, pose.blendOutTicks());
        }
        EnumMap<GauntletPose.Part, GauntletPose.PartPose> parts = new EnumMap<>(GauntletPose.Part.class);
        pose.parts().forEach(parts::put);
        applyAdjustments(active.type(), parts, active.param(), time);
        return new Evaluation(parts, pose.fp(), weight, spinDegrees(active.type(), age, active.durationTicks()));
    }

    private static void applyAdjustments(GauntletPoseType type, EnumMap<GauntletPose.Part, GauntletPose.PartPose> parts, float param, float time) {
        switch (type) {
            case ROCKET_PUNCH_CHARGE, FIELD_EXTRACTION_CHANNEL -> {
                float charge = Math.clamp(param, 0f, 1f);
                GauntletPose.PartPose arm = parts.get(GauntletPose.Part.RIGHT_ARM);
                if (arm != null) {
                    float jitter = (float) Math.sin(time * 1.3) * charge * 0.04f;
                    parts.put(GauntletPose.Part.RIGHT_ARM, new GauntletPose.PartPose(
                            arm.xRot() * charge + jitter, arm.yRot() * charge, arm.zRot() * charge,
                            arm.x() * charge, arm.y() * charge, arm.z() * charge));
                }
            }
            case METEOR_HOVER -> {
                GauntletPose.PartPose body = parts.getOrDefault(GauntletPose.Part.BODY, IDENTITY);
                parts.put(GauntletPose.Part.BODY, new GauntletPose.PartPose(
                        body.xRot(), body.yRot(), body.zRot(),
                        body.x(), body.y() + (float) Math.sin(time * 0.25) * 0.3f, body.z()));
            }
            case SEDATED_SLUMP -> {
                GauntletPose.PartPose body = parts.getOrDefault(GauntletPose.Part.BODY, IDENTITY);
                parts.put(GauntletPose.Part.BODY, new GauntletPose.PartPose(
                        body.xRot(), body.yRot(), body.zRot() + (float) Math.sin(time * 0.06) * 3.0f * Mth.DEG_TO_RAD,
                        body.x(), body.y(), body.z()));
            }
            default -> {}
        }
    }

    private static EnumMap<GauntletPose.Part, GauntletPose.PartPose> lerpParts(
            EnumMap<GauntletPose.Part, GauntletPose.PartPose> from,
            EnumMap<GauntletPose.Part, GauntletPose.PartPose> to,
            float t) {
        EnumMap<GauntletPose.Part, GauntletPose.PartPose> result = new EnumMap<>(GauntletPose.Part.class);
        for (GauntletPose.Part part : GauntletPose.Part.values()) {
            GauntletPose.PartPose a = from.get(part);
            GauntletPose.PartPose b = to.get(part);
            if (a == null && b == null) continue;
            result.put(part, lerpPose(a != null ? a : IDENTITY, b != null ? b : IDENTITY, t));
        }
        return result;
    }

    private static GauntletPose.PartPose lerpPose(GauntletPose.PartPose a, GauntletPose.PartPose b, float t) {
        return new GauntletPose.PartPose(
                Mth.lerp(t, a.xRot(), b.xRot()), Mth.lerp(t, a.yRot(), b.yRot()), Mth.lerp(t, a.zRot(), b.zRot()),
                Mth.lerp(t, a.x(), b.x()), Mth.lerp(t, a.y(), b.y()), Mth.lerp(t, a.z(), b.z()));
    }

    private static GauntletPose.FpTransform lerpFp(GauntletPose.FpTransform a, GauntletPose.FpTransform b, float t) {
        return new GauntletPose.FpTransform(
                Mth.lerp(t, a.x(), b.x()), Mth.lerp(t, a.y(), b.y()), Mth.lerp(t, a.z(), b.z()),
                Mth.lerp(t, a.xRot(), b.xRot()), Mth.lerp(t, a.yRot(), b.yRot()), Mth.lerp(t, a.zRot(), b.zRot()));
    }

    private static float smoothstep(float value, float periodTicks) {
        if (periodTicks <= 0f) return 1f;
        float t = Math.clamp(value / periodTicks, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    /**
     * Whole-body render spin for poses that have one, in degrees. Must complete a whole number of turns
     * exactly at duration end, so the render rotation hands back to the entity's own yaw without a jump.
     */
    private static float spinDegrees(GauntletPoseType type, float ageTicks, float durationTicks) {
        if (type == GauntletPoseType.RISING_UPPERCUT) {
            return 360f * smoothstep(ageTicks, durationTicks);
        }
        return 0f;
    }

    /** First-person arm transform of the active pose together with its current blend weight. */
    public record WeightedFp(GauntletPose.FpTransform transform, float weight) {}

    private record ActivePose(GauntletPoseType type, GauntletPose pose, float startTime, float refreshTime, float param,
            int durationTicks) {}

    private record Evaluation(EnumMap<GauntletPose.Part, GauntletPose.PartPose> parts, GauntletPose.FpTransform fp, float weight,
            float bodySpin) {}

    private record Frame(EnumMap<GauntletPose.Part, GauntletPose.PartPose> parts, GauntletPose.FpTransform fp, float weight,
            float bodySpin) {}

    private record Snapshot(GauntletPoseType type, EnumMap<GauntletPose.Part, GauntletPose.PartPose> parts,
            GauntletPose.FpTransform fp, float weight, float time) {}
}
