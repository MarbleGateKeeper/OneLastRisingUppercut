package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ExperimentalBarrierConfig {
    public final ModConfigSpec.DoubleValue width;
    public final ModConfigSpec.DoubleValue height;
    public final ModConfigSpec.DoubleValue minDeployDistance;
    public final ModConfigSpec.DoubleValue maxDeployDistance;
    public final ModConfigSpec.DoubleValue deploySpeedPerTick;
    public final ModConfigSpec.DoubleValue maxDurability;
    public final ModConfigSpec.DoubleValue durabilityRegenPerSecond;
    public final ModConfigSpec.IntValue regenDelayTicks;
    public final ModConfigSpec.DoubleValue absorbZoneThickness;
    public final ModConfigSpec.IntValue brokenCooldownTicks;
    public final ModConfigSpec.IntValue recallCooldownTicks;

    ExperimentalBarrierConfig(ModConfigSpec.Builder builder) {
        width = builder
                .comment("Width in blocks of the Experimental Barrier")
                .defineInRange("width", 4.0, 0.5, 16.0);
        height = builder
                .comment("Height in blocks of the Experimental Barrier")
                .defineInRange("height", 2.5, 0.5, 16.0);
        minDeployDistance = builder
                .comment("Minimum distance in blocks the barrier travels away from the caster")
                .defineInRange("minDeployDistance", 4.0, 0.0, 32.0);
        maxDeployDistance = builder
                .comment("Maximum distance in blocks the barrier travels away from the caster")
                .defineInRange("maxDeployDistance", 12.0, 1.0, 64.0);
        deploySpeedPerTick = builder
                .comment("Travel speed of the barrier in blocks per tick while deployed")
                .defineInRange("deploySpeedPerTick", 0.2, 0.01, 5.0);
        maxDurability = builder
                .comment("Maximum damage the barrier can absorb before breaking")
                .defineInRange("maxDurability", 40.0, 1.0, 1000.0);
        durabilityRegenPerSecond = builder
                .comment("Barrier durability regenerated per second while not deployed")
                .defineInRange("durabilityRegenPerSecond", 4.0, 0.0, 100.0);
        regenDelayTicks = builder
                .comment("Ticks after recall or damage before barrier durability starts regenerating")
                .defineInRange("regenDelayTicks", 40, 0, 2400);
        absorbZoneThickness = builder
                .comment("Thickness in blocks of the barrier's projectile absorb zone")
                .defineInRange("absorbZoneThickness", 0.3, 0.05, 2.0);
        brokenCooldownTicks = builder
                .comment("Cooldown in ticks before the barrier can be deployed again after breaking")
                .defineInRange("brokenCooldownTicks", 100, 1, 2400);
        recallCooldownTicks = builder
                .comment("Cooldown in ticks before the barrier can be deployed again after a recall")
                .defineInRange("recallCooldownTicks", 40, 0, 2400);
    }
}
