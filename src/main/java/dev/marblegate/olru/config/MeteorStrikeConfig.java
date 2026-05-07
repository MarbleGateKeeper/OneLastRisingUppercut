package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MeteorStrikeConfig {
    public final ModConfigSpec.IntValue teleportHeightOffset;
    public final ModConfigSpec.IntValue hoverTicks;
    public final ModConfigSpec.DoubleValue fallSpeed;
    public final ModConfigSpec.DoubleValue innerRadius;
    public final ModConfigSpec.DoubleValue outerRadius;
    public final ModConfigSpec.DoubleValue outerDamageMax;
    public final ModConfigSpec.DoubleValue outerDamageMin;
    public final ModConfigSpec.DoubleValue innerDamageMultiplier;
    public final ModConfigSpec.DoubleValue chargePercentPerDamage;

    MeteorStrikeConfig(ModConfigSpec.Builder builder) {
        teleportHeightOffset = builder
                .comment("How many blocks below the world build height limit the player is teleported to")
                .defineInRange("teleportHeightOffset", 5, 1, 100);
        hoverTicks = builder
                .comment("How long (ticks) the player hovers before slamming down. During this time they can move horizontally.")
                .defineInRange("hoverTicks", 60, 0, 200);
        fallSpeed = builder
                .comment("Downward speed (blocks/tick) during the slam")
                .defineInRange("fallSpeed", 4.0, 0.5, 20.0);
        innerRadius = builder
                .comment("Inner radius (blocks) of the impact zone — entities here take max-health damage")
                .defineInRange("innerRadius", 4.0, 0.5, 20.0);
        outerRadius = builder
                .comment("Outer radius (blocks) of the impact zone — entities here take reduced damage")
                .defineInRange("outerRadius", 10.0, 1.0, 40.0);
        outerDamageMax = builder
                .comment("Maximum damage at the inner edge of the outer ring")
                .defineInRange("outerDamageMax", 40.0, 0.0, 1000.0);
        outerDamageMin = builder
                .comment("Minimum damage at the outer edge of the outer ring")
                .defineInRange("outerDamageMin", 5.0, 0.0, 1000.0);
        innerDamageMultiplier = builder
                .comment("Multiplier applied to target max-health for inner-ring damage (1.0 = 100% max HP)")
                .defineInRange("innerDamageMultiplier", 1.0, 0.0, 10.0);
        chargePercentPerDamage = builder
                .comment("Charge percent gained per 1 point of damage dealt (default 1.0 means 100 damage = fully charged)")
                .defineInRange("chargePercentPerDamage", 1.0, 0.0, 100.0);
    }
}
