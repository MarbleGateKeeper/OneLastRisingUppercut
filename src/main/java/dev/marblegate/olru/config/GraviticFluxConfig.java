package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GraviticFluxConfig {
    public final ModConfigSpec.DoubleValue riseHeight;
    public final ModConfigSpec.IntValue riseTicks;
    public final ModConfigSpec.IntValue aimTicks;
    public final ModConfigSpec.DoubleValue zoneRadius;
    public final ModConfigSpec.DoubleValue liftHeight;
    public final ModConfigSpec.IntValue suspendTicks;
    public final ModConfigSpec.DoubleValue slamMaxHealthFraction;
    public final ModConfigSpec.DoubleValue slamDamageCap;
    public final ModConfigSpec.IntValue slowTicks;
    public final ModConfigSpec.DoubleValue chargePercentPerDamage;

    GraviticFluxConfig(ModConfigSpec.Builder builder) {
        riseHeight = builder
                .comment("Height in blocks the caster rises when Gravitic Flux starts")
                .defineInRange("riseHeight", 8.0, 0.0, 64.0);
        riseTicks = builder
                .comment("Ticks the caster takes to rise before aiming")
                .defineInRange("riseTicks", 10, 1, 200);
        aimTicks = builder
                .comment("Ticks the caster can aim the zone before it triggers")
                .defineInRange("aimTicks", 60, 1, 600);
        zoneRadius = builder
                .comment("Radius in blocks of the Gravitic Flux zone")
                .defineInRange("zoneRadius", 5.0, 1.0, 32.0);
        liftHeight = builder
                .comment("Height in blocks enemies in the zone are lifted")
                .defineInRange("liftHeight", 6.0, 0.0, 32.0);
        suspendTicks = builder
                .comment("Ticks lifted enemies are suspended before the slam")
                .defineInRange("suspendTicks", 30, 0, 600);
        slamMaxHealthFraction = builder
                .comment("Fraction of a target's max health dealt by the slam")
                .defineInRange("slamMaxHealthFraction", 0.5, 0.0, 1.0);
        slamDamageCap = builder
                .comment("Maximum damage the slam can deal to a single target")
                .defineInRange("slamDamageCap", 100.0, 1.0, 1000.0);
        slowTicks = builder
                .comment("Duration in ticks slammed targets are slowed afterwards")
                .defineInRange("slowTicks", 60, 0, 600);
        chargePercentPerDamage = builder
                .comment("Ultimate charge percent gained per point of damage dealt")
                .defineInRange("chargePercentPerDamage", 0.35, 0.0, 100.0);
    }
}
