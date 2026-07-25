package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FadeConfig {
    public final ModConfigSpec.IntValue durationTicks;
    public final ModConfigSpec.IntValue speedAmplifier;
    public final ModConfigSpec.IntValue jumpBoostAmplifier;
    public final ModConfigSpec.IntValue cooldownTicks;

    FadeConfig(ModConfigSpec.Builder builder) {
        durationTicks = builder
                .comment("Duration in ticks of Fade")
                .defineInRange("durationTicks", 16, 1, 200);
        speedAmplifier = builder
                .comment("Speed effect amplifier during Fade")
                .defineInRange("speedAmplifier", 7, 0, 9);
        jumpBoostAmplifier = builder
                .comment("Jump boost effect amplifier during Fade")
                .defineInRange("jumpBoostAmplifier", 1, 0, 9);
        cooldownTicks = builder
                .comment("Cooldown in ticks before Fade can be used again")
                .defineInRange("cooldownTicks", 120, 1, 2400);
    }
}
