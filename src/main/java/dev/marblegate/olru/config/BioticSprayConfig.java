package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BioticSprayConfig {
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.DoubleValue coneAngleDegrees;
    public final ModConfigSpec.DoubleValue healPerTick;
    public final ModConfigSpec.DoubleValue energyDrainPerTick;
    public final ModConfigSpec.IntValue lingerTicks;
    public final ModConfigSpec.IntValue lingerAmplifier;
    public final ModConfigSpec.DoubleValue ultChargePercentPerHeal;
    public final ModConfigSpec.IntValue pulseIntervalTicks;

    BioticSprayConfig(ModConfigSpec.Builder builder) {
        range = builder
                .comment("Maximum reach in blocks for Biotic Spray")
                .defineInRange("range", 8.0, 1.0, 32.0);
        coneAngleDegrees = builder
                .comment("Forward cone angle in degrees for Biotic Spray healing")
                .defineInRange("coneAngleDegrees", 60.0, 5.0, 180.0);
        healPerTick = builder
                .comment("Health restored per tick to each allied target in the spray cone")
                .defineInRange("healPerTick", 0.8, 0.0, 20.0);
        energyDrainPerTick = builder
                .comment("Biotic energy drained per tick while spraying, as a fraction of the full bar")
                .defineInRange("energyDrainPerTick", 0.006, 0.0, 1.0);
        lingerTicks = builder
                .comment("Duration in ticks of the lingering regeneration applied by Biotic Spray")
                .defineInRange("lingerTicks", 60, 0, 1200);
        lingerAmplifier = builder
                .comment("Amplifier of the lingering regeneration effect")
                .defineInRange("lingerAmplifier", 1, 0, 4);
        ultChargePercentPerHeal = builder
                .comment("Coalescence charge percent gained per point of allied health restored by Biotic Spray")
                .defineInRange("ultChargePercentPerHeal", 0.5, 0.0, 100.0);
        pulseIntervalTicks = builder
                .comment("Interval in ticks between Biotic Spray pulses while held")
                .defineInRange("pulseIntervalTicks", 2, 1, 100);
    }
}
