package dev.marblegate.olru.client.animation;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import java.util.EnumMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

public record GauntletPoseRenderData(EnumMap<GauntletPose.Part, GauntletPose.PartPose> parts, float weight) {
    public static final ContextKey<GauntletPoseRenderData> KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gauntlet_pose"));
}
