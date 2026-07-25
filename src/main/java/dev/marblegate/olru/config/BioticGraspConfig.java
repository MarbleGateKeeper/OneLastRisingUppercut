package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BioticGraspConfig {
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.DoubleValue coneAngleDegrees;
    public final ModConfigSpec.DoubleValue damage;
    public final ModConfigSpec.DoubleValue selfHeal;
    public final ModConfigSpec.DoubleValue energyPerHit;
    public final ModConfigSpec.IntValue swingCooldownTicks;
    public final ModConfigSpec.IntValue pulseIntervalTicks;

    BioticGraspConfig(ModConfigSpec.Builder builder) {
        range = builder
                .comment("Maximum reach in blocks for Biotic Grasp")
                .defineInRange("range", 12.0, 1.0, 32.0);
        coneAngleDegrees = builder
                .comment("Forward cone angle in degrees for Biotic Grasp target selection")
                .defineInRange("coneAngleDegrees", 30.0, 5.0, 180.0);
        damage = builder
                .comment("Damage dealt to the grasped target")
                .defineInRange("damage", 4.0, 0.0, 100.0);
        selfHeal = builder
                .comment("Health restored to the wielder per connected grasp")
                .defineInRange("selfHeal", 1.5, 0.0, 100.0);
        energyPerHit = builder
                .comment("Biotic energy gained per connected grasp pulse, as a fraction of the full bar")
                .defineInRange("energyPerHit", 0.015, 0.0, 1.0);
        swingCooldownTicks = builder
                .comment("Cooldown in ticks between Biotic Grasp swings; 0 means no cooldown")
                .defineInRange("swingCooldownTicks", 0, 0, 200);
        pulseIntervalTicks = builder
                .comment("Interval in ticks between Biotic Grasp damage pulses while held")
                .defineInRange("pulseIntervalTicks", 4, 1, 100);
    }
}
