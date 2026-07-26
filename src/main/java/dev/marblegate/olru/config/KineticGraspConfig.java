package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KineticGraspConfig {
    public final ModConfigSpec.IntValue durationTicks;
    public final ModConfigSpec.DoubleValue coneAngleDegrees;
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.DoubleValue creditPerHeavyProjectile;
    public final ModConfigSpec.DoubleValue creditPerProjectile;
    public final ModConfigSpec.DoubleValue shieldConversion;
    public final ModConfigSpec.DoubleValue maxShield;
    public final ModConfigSpec.IntValue shieldDurationTicks;
    public final ModConfigSpec.IntValue selfSlowAmplifier;
    public final ModConfigSpec.IntValue cooldownTicks;

    KineticGraspConfig(ModConfigSpec.Builder builder) {
        durationTicks = builder
                .comment("Duration in ticks the Kinetic Grasp field stays active")
                .defineInRange("durationTicks", 50, 1, 400);
        coneAngleDegrees = builder
                .comment("Opening angle in degrees of the absorb cone in front of the caster")
                .defineInRange("coneAngleDegrees", 120.0, 10.0, 360.0);
        range = builder
                .comment("Range in blocks of the absorb cone")
                .defineInRange("range", 6.0, 1.0, 32.0);
        creditPerHeavyProjectile = builder
                .comment("Shield credit granted per absorbed heavy projectile")
                .defineInRange("creditPerHeavyProjectile", 6.0, 0.0, 100.0);
        creditPerProjectile = builder
                .comment("Shield credit granted per absorbed regular projectile")
                .defineInRange("creditPerProjectile", 2.0, 0.0, 100.0);
        shieldConversion = builder
                .comment("Fraction of absorbed credit converted into temporary shield")
                .defineInRange("shieldConversion", 0.5, 0.0, 2.0);
        maxShield = builder
                .comment("Maximum temporary shield Kinetic Grasp can grant")
                .defineInRange("maxShield", 20.0, 0.0, 100.0);
        shieldDurationTicks = builder
                .comment("Duration in ticks of the temporary shield")
                .defineInRange("shieldDurationTicks", 120, 1, 2400);
        selfSlowAmplifier = builder
                .comment("Slowness amplifier applied to the caster while Kinetic Grasp is active")
                .defineInRange("selfSlowAmplifier", 1, 0, 4);
        cooldownTicks = builder
                .comment("Cooldown in ticks before Kinetic Grasp can be used again")
                .defineInRange("cooldownTicks", 120, 1, 2400);
    }
}
