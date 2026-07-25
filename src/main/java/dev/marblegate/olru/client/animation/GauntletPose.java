package dev.marblegate.olru.client.animation;

import java.util.EnumMap;

public record GauntletPose(int defaultDurationTicks, boolean looping, int blendInTicks, int blendOutTicks,
        EnumMap<GauntletPose.Part, PartPose> parts, FpTransform fp) {
    public enum Part {
        HEAD,
        BODY,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_LEG,
        LEFT_LEG
    }

    /** Rotations in radians, translations in model-space units (16 = 1 block). */
    public record PartPose(float xRot, float yRot, float zRot, float x, float y, float z) {}

    /** First-person PoseStack transform; rotations in degrees. */
    public record FpTransform(float x, float y, float z, float xRot, float yRot, float zRot) {}
}
