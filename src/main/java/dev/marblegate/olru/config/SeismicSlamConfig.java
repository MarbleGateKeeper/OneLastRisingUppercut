package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SeismicSlamConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.DoubleValue leapForwardSpeed;
    public final ModConfigSpec.DoubleValue leapUpSpeed;
    public final ModConfigSpec.DoubleValue gravity;
    public final ModConfigSpec.IntValue maxTravelTicks;
    public final ModConfigSpec.DoubleValue impactRange;
    public final ModConfigSpec.DoubleValue impactConeAngleDegrees;
    public final ModConfigSpec.DoubleValue damage;
    public final ModConfigSpec.IntValue slowTicks;
    public final ModConfigSpec.IntValue slowAmplifier;

    SeismicSlamConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Cooldown in ticks before Seismic Slam can be used again")
                .defineInRange("cooldownTicks", 140, 1, 2400);
        leapForwardSpeed = builder
                .comment("Initial forward speed in blocks per tick")
                .defineInRange("leapForwardSpeed", 1.5, 0.1, 10.0);
        leapUpSpeed = builder
                .comment("Initial upward speed in blocks per tick")
                .defineInRange("leapUpSpeed", 2.15, 0.1, 10.0);
        gravity = builder
                .comment("Downward acceleration applied each tick during Seismic Slam")
                .defineInRange("gravity", 0.12, 0.01, 2.0);
        maxTravelTicks = builder
                .comment("Maximum number of ticks the leap can remain active before forcing impact")
                .defineInRange("maxTravelTicks", 60, 5, 200);
        impactRange = builder
                .comment("Forward impact range in blocks")
                .defineInRange("impactRange", 10.0, 0.5, 32.0);
        impactConeAngleDegrees = builder
                .comment("Forward impact cone angle in degrees")
                .defineInRange("impactConeAngleDegrees", 95.0, 10.0, 180.0);
        damage = builder
                .comment("Damage dealt to enemies caught in the impact area")
                .defineInRange("damage", 8.0, 0.0, 1000.0);
        slowTicks = builder
                .comment("Slowness duration applied to enemies caught by the slam")
                .defineInRange("slowTicks", 80, 1, 1200);
        slowAmplifier = builder
                .comment("Slowness amplifier applied to enemies caught by the slam")
                .defineInRange("slowAmplifier", 1, 0, 10);
    }
}
