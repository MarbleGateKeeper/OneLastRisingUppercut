package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BioticOrbConfig {
    public final ModConfigSpec.DoubleValue speed;
    public final ModConfigSpec.DoubleValue tetheredSpeed;
    public final ModConfigSpec.IntValue lifeTicks;
    public final ModConfigSpec.DoubleValue radius;
    public final ModConfigSpec.DoubleValue damagePerPulse;
    public final ModConfigSpec.IntValue pulseIntervalTicks;
    public final ModConfigSpec.DoubleValue damagePool;
    public final ModConfigSpec.IntValue cooldownTicks;

    BioticOrbConfig(ModConfigSpec.Builder builder) {
        speed = builder
                .comment("Travel speed of the Biotic Orb in blocks per tick while no enemy is in range")
                .defineInRange("speed", 0.6, 0.01, 5.0);
        tetheredSpeed = builder
                .comment("Speed of the Biotic Orb in blocks per tick while an enemy is within its radius")
                .defineInRange("tetheredSpeed", 0.15, 0.0, 5.0);
        lifeTicks = builder
                .comment("Lifetime in ticks before the Biotic Orb dissipates")
                .defineInRange("lifeTicks", 160, 20, 1200);
        radius = builder
                .comment("Radius in blocks around the orb in which targets are pulsed")
                .defineInRange("radius", 5.0, 1.0, 16.0);
        damagePerPulse = builder
                .comment("Damage dealt per pulse to each hostile target in range")
                .defineInRange("damagePerPulse", 2.0, 0.0, 100.0);
        pulseIntervalTicks = builder
                .comment("Interval in ticks between orb pulses")
                .defineInRange("pulseIntervalTicks", 5, 1, 100);
        damagePool = builder
                .comment("Total damage the orb can deal before dissipating")
                .defineInRange("damagePool", 40.0, 1.0, 1000.0);
        cooldownTicks = builder
                .comment("Cooldown in ticks before Biotic Orb can be used again")
                .defineInRange("cooldownTicks", 200, 1, 2400);
    }
}
