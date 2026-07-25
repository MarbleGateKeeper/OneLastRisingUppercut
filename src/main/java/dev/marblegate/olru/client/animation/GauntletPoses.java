package dev.marblegate.olru.client.animation;

import dev.marblegate.olru.common.animation.GauntletPoseType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Mth;

public final class GauntletPoses {
    private static final EnumMap<GauntletPoseType, GauntletPose> POSES = new EnumMap<>(GauntletPoseType.class);
    private static final int LOOP_DURATION_TICKS = 200;

    static {
        register(GauntletPoseType.PRIME_HAND_CANNON_RECOIL, 6, 1, 4, fp(0f, 0.03f, 0.10f, 8f),
                part(GauntletPose.Part.RIGHT_ARM, -90f, 0f, 0f),
                part(GauntletPose.Part.BODY, 0f, -8f, 0f));
        registerLoop(GauntletPoseType.ROCKET_PUNCH_CHARGE, 3, 3, fp(0.06f, -0.04f, 0.14f, 12f),
                part(GauntletPose.Part.RIGHT_ARM, 25f, 0f, 20f),
                part(GauntletPose.Part.LEFT_ARM, -45f, 0f, 0f),
                part(GauntletPose.Part.BODY, 0f, 25f, 0f, 0f, 1.2f, 0f),
                part(GauntletPose.Part.RIGHT_LEG, -15f, 0f, 0f),
                part(GauntletPose.Part.LEFT_LEG, 15f, 0f, 0f));
        registerLoop(GauntletPoseType.ROCKET_PUNCH_FLIGHT, 2, 4, fp(-0.12f, 0.06f, -0.18f, -18f),
                part(GauntletPose.Part.RIGHT_ARM, -90f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, 30f, 0f, 0f),
                part(GauntletPose.Part.BODY, 15f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_LEG, 10f, 0f, 0f),
                part(GauntletPose.Part.LEFT_LEG, 10f, 0f, 0f));
        register(GauntletPoseType.ROCKET_PUNCH_IMPACT, 8, 1, 6, fp(-0.10f, 0.04f, -0.16f, -14f),
                part(GauntletPose.Part.RIGHT_ARM, -100f, 0f, 0f),
                part(GauntletPose.Part.BODY, 20f, 0f, 0f));
        register(GauntletPoseType.RISING_UPPERCUT, 12, 2, 4, fp(-0.08f, 0.10f, -0.10f, -25f),
                part(GauntletPose.Part.RIGHT_ARM, -170f, 0f, 0f),
                part(GauntletPose.Part.BODY, -12f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_LEG, 20f, 0f, 0f),
                part(GauntletPose.Part.LEFT_LEG, 20f, 0f, 0f));
        registerLoop(GauntletPoseType.SEISMIC_SLAM_LEAP, 3, 3, fp(-0.05f, 0.08f, -0.05f, -20f),
                part(GauntletPose.Part.RIGHT_ARM, -160f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, -160f, 0f, 0f),
                part(GauntletPose.Part.BODY, 10f, 0f, 0f));
        register(GauntletPoseType.SEISMIC_SLAM_LAND, 12, 2, 8, fp(0.04f, -0.10f, 0.16f, 18f),
                part(GauntletPose.Part.BODY, 0f, 0f, 0f, 0f, 3f, 0f),
                part(GauntletPose.Part.RIGHT_ARM, -70f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, 30f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_LEG, -35f, 0f, 0f),
                part(GauntletPose.Part.LEFT_LEG, 35f, 0f, 0f));
        registerLoop(GauntletPoseType.METEOR_HOVER, 4, 4, fp(0.08f, -0.02f, 0.06f, 0f),
                part(GauntletPose.Part.RIGHT_ARM, 0f, 0f, 70f),
                part(GauntletPose.Part.LEFT_ARM, 0f, 0f, -70f));
        registerLoop(GauntletPoseType.METEOR_DIVE, 3, 4, fp(-0.14f, 0.10f, -0.22f, -30f),
                part(GauntletPose.Part.RIGHT_ARM, -100f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, 30f, 0f, 0f),
                part(GauntletPose.Part.BODY, 75f, 0f, 0f),
                part(GauntletPose.Part.HEAD, -40f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_LEG, 8f, 0f, 0f),
                part(GauntletPose.Part.LEFT_LEG, 8f, 0f, 0f));
        register(GauntletPoseType.METEOR_LAND, 14, 2, 8, fp(0.05f, -0.12f, 0.18f, 20f),
                part(GauntletPose.Part.BODY, 0f, 0f, 0f, 0f, 3.5f, 0f),
                part(GauntletPose.Part.RIGHT_ARM, -85f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, 30f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_LEG, -40f, 0f, 0f),
                part(GauntletPose.Part.LEFT_LEG, 40f, 0f, 0f),
                part(GauntletPose.Part.HEAD, 15f, 0f, 0f));
        register(GauntletPoseType.HORUS_BIOTIC_ROUND_RECOIL, 5, 1, 4, fp(0f, 0.02f, 0.08f, 6f),
                part(GauntletPose.Part.RIGHT_ARM, -70f, 0f, 0f),
                part(GauntletPose.Part.BODY, 0f, -6f, 0f));
        registerLoop(GauntletPoseType.FIELD_EXTRACTION_CHANNEL, 3, 3, fp(-0.06f, 0f, -0.12f, -8f),
                part(GauntletPose.Part.RIGHT_ARM, -75f, 0f, -10f),
                part(GauntletPose.Part.BODY, 8f, 0f, 0f));
        register(GauntletPoseType.FIELD_EXTRACTION_RELEASE, 8, 1, 5, fp(0f, 0.06f, 0.10f, 10f),
                part(GauntletPose.Part.RIGHT_ARM, -120f, 0f, 0f));
        register(GauntletPoseType.SEDATIVE_DART_FIRE, 8, 2, 5, fp(-0.10f, 0.02f, -0.14f, -6f),
                part(GauntletPose.Part.BODY, 0f, 30f, 0f),
                part(GauntletPose.Part.RIGHT_ARM, -90f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, -60f, -30f, 0f));
        register(GauntletPoseType.BIOTIC_GRENADE_THROW, 8, 2, 5, fp(0f, 0.10f, -0.06f, -35f),
                part(GauntletPose.Part.RIGHT_ARM, -150f, 0f, 0f),
                part(GauntletPose.Part.BODY, 0f, -15f, 0f));
        register(GauntletPoseType.NANO_SURGE_CAST, 16, 3, 6, fp(-0.04f, 0.12f, -0.08f, -20f),
                part(GauntletPose.Part.RIGHT_ARM, -160f, 0f, 20f),
                part(GauntletPose.Part.LEFT_ARM, -160f, 0f, -20f),
                part(GauntletPose.Part.BODY, -8f, 0f, 0f));
        registerLoop(GauntletPoseType.SEDATED_SLUMP, 6, 6, fp(0f, 0f, 0f, 0f),
                part(GauntletPose.Part.HEAD, 35f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_ARM, 10f, 0f, 5f),
                part(GauntletPose.Part.LEFT_ARM, 10f, 0f, -5f));
        registerLoop(GauntletPoseType.GRASP_FIRE, 2, 3, fp(0f, 0.02f, 0.08f, 6f),
                part(GauntletPose.Part.RIGHT_ARM, -75f, 0f, 0f),
                part(GauntletPose.Part.BODY, 0f, -6f, 0f));
        registerLoop(GauntletPoseType.SPRAY_CHANNEL, 3, 3, fp(-0.06f, 0f, -0.10f, -6f),
                part(GauntletPose.Part.RIGHT_ARM, -60f, 0f, -15f),
                part(GauntletPose.Part.BODY, 6f, 0f, 0f));
        register(GauntletPoseType.FADE, 6, 1, 4, fp(0f, 0f, 0.12f, 0f),
                part(GauntletPose.Part.BODY, 18f, 0f, 0f),
                part(GauntletPose.Part.RIGHT_ARM, 25f, 0f, 0f),
                part(GauntletPose.Part.LEFT_ARM, 25f, 0f, 0f));
        register(GauntletPoseType.ORB_THROW, 8, 2, 5, fp(0f, 0.08f, -0.06f, -25f),
                part(GauntletPose.Part.RIGHT_ARM, -140f, 0f, 0f),
                part(GauntletPose.Part.BODY, 0f, -12f, 0f));
        registerLoop(GauntletPoseType.COALESCENCE_CHANNEL, 3, 3, fp(-0.10f, 0.04f, -0.16f, -12f),
                part(GauntletPose.Part.RIGHT_ARM, -85f, 0f, 8f),
                part(GauntletPose.Part.LEFT_ARM, -85f, 0f, -8f),
                part(GauntletPose.Part.BODY, 4f, 0f, 0f));
    }

    private GauntletPoses() {}

    public static GauntletPose get(GauntletPoseType type) {
        return POSES.get(type);
    }

    @SafeVarargs
    private static void register(GauntletPoseType type, int durationTicks, int blendInTicks, int blendOutTicks,
            GauntletPose.FpTransform fp, Map.Entry<GauntletPose.Part, GauntletPose.PartPose>... parts) {
        register(type, durationTicks, false, blendInTicks, blendOutTicks, fp, parts);
    }

    @SafeVarargs
    private static void registerLoop(GauntletPoseType type, int blendInTicks, int blendOutTicks,
            GauntletPose.FpTransform fp, Map.Entry<GauntletPose.Part, GauntletPose.PartPose>... parts) {
        register(type, LOOP_DURATION_TICKS, true, blendInTicks, blendOutTicks, fp, parts);
    }

    @SafeVarargs
    private static void register(GauntletPoseType type, int durationTicks, boolean looping, int blendInTicks, int blendOutTicks,
            GauntletPose.FpTransform fp, Map.Entry<GauntletPose.Part, GauntletPose.PartPose>... parts) {
        EnumMap<GauntletPose.Part, GauntletPose.PartPose> map = new EnumMap<>(GauntletPose.Part.class);
        for (Map.Entry<GauntletPose.Part, GauntletPose.PartPose> entry : parts) {
            map.put(entry.getKey(), entry.getValue());
        }
        POSES.put(type, new GauntletPose(durationTicks, looping, blendInTicks, blendOutTicks, map, fp));
    }

    private static Map.Entry<GauntletPose.Part, GauntletPose.PartPose> part(GauntletPose.Part part, float xRot, float yRot, float zRot) {
        return part(part, xRot, yRot, zRot, 0f, 0f, 0f);
    }

    private static Map.Entry<GauntletPose.Part, GauntletPose.PartPose> part(GauntletPose.Part part, float xRot, float yRot, float zRot,
            float x, float y, float z) {
        return Map.entry(part, new GauntletPose.PartPose(
                xRot * Mth.DEG_TO_RAD, yRot * Mth.DEG_TO_RAD, zRot * Mth.DEG_TO_RAD, x, y, z));
    }

    private static GauntletPose.FpTransform fp(float x, float y, float z, float xRot) {
        return new GauntletPose.FpTransform(x, y, z, xRot, 0f, 0f);
    }
}
