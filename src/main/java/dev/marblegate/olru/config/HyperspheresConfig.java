package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class HyperspheresConfig {
    public final ModConfigSpec.IntValue maxCharges;
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.DoubleValue directDamage;
    public final ModConfigSpec.DoubleValue implosionDamage;
    public final ModConfigSpec.DoubleValue implosionRadius;
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.DoubleValue speed;
    public final ModConfigSpec.IntValue pairIntervalTicks;

    HyperspheresConfig(ModConfigSpec.Builder builder) {
        maxCharges = builder
                .comment("Maximum stored Hyperspheres pairs")
                .defineInRange("maxCharges", 5, 1, 20);
        cooldownTicks = builder
                .comment("Ticks to refill one Hyperspheres pair")
                .defineInRange("cooldownTicks", 40, 1, 2400);
        directDamage = builder
                .comment("Damage dealt by a direct sphere hit before the implosion")
                .defineInRange("directDamage", 4.0, 0.0, 100.0);
        implosionDamage = builder
                .comment("Damage dealt by the sphere implosion to targets in its radius")
                .defineInRange("implosionDamage", 2.0, 0.0, 100.0);
        implosionRadius = builder
                .comment("Radius in blocks of the sphere implosion")
                .defineInRange("implosionRadius", 2.5, 0.5, 16.0);
        range = builder
                .comment("Distance in blocks a sphere travels before imploding in mid-air")
                .defineInRange("range", 22.0, 1.0, 64.0);
        speed = builder
                .comment("Travel speed of a Hypersphere in blocks per tick")
                .defineInRange("speed", 0.8, 0.01, 5.0);
        pairIntervalTicks = builder
                .comment("Ticks between the first and second sphere of a pair")
                .defineInRange("pairIntervalTicks", 2, 0, 20);
    }
}
