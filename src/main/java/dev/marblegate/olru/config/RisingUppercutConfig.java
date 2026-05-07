package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RisingUppercutConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.DoubleValue riseHeight;
    public final ModConfigSpec.DoubleValue riseSpeedPlayer;
    public final ModConfigSpec.DoubleValue riseSpeedMob;
    public final ModConfigSpec.DoubleValue mobDamage;
    public final ModConfigSpec.DoubleValue frontConeRange;
    public final ModConfigSpec.DoubleValue frontConeAngleDegrees;

    RisingUppercutConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Cooldown in ticks before Rising Uppercut can be used again")
                .defineInRange("cooldownTicks", 80, 1, 2400);
        riseHeight = builder
                .comment("Height (blocks) the player and hit mobs are launched upward")
                .defineInRange("riseHeight", 8.0, 1.0, 50.0);
        riseSpeedPlayer = builder
                .comment("Upward speed (blocks/tick) applied to the player")
                .defineInRange("riseSpeedPlayer", 1.2, 0.1, 10.0);
        riseSpeedMob = builder
                .comment("Upward speed (blocks/tick) applied to struck mobs")
                .defineInRange("riseSpeedMob", 1.2, 0.1, 10.0);
        mobDamage = builder
                .comment("Damage dealt to each mob hit by Rising Uppercut")
                .defineInRange("mobDamage", 10.0, 0.0, 1000.0);
        frontConeRange = builder
                .comment("Range (blocks) of the front cone that detects mobs")
                .defineInRange("frontConeRange", 5.0, 0.5, 20.0);
        frontConeAngleDegrees = builder
                .comment("Total angle (degrees) of the front cone")
                .defineInRange("frontConeAngleDegrees", 60.0, 10.0, 180.0);
    }
}
