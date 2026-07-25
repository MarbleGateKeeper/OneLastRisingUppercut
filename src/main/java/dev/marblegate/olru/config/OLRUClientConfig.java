package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OLRUClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CAMERA_SHAKE;
    public static final ModConfigSpec.BooleanValue FOV_KICK;

    static {
        BUILDER.push("camera").comment("Camera feedback effects");
        CAMERA_SHAKE = BUILDER
                .comment("Camera shake on heavy impacts")
                .define("cameraShake", true);
        FOV_KICK = BUILDER
                .comment("FOV increase during movement skills")
                .define("fovKick", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
