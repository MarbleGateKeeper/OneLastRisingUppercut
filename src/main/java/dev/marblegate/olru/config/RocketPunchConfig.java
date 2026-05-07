package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RocketPunchConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.IntValue maxChargeTicks;
    public final ModConfigSpec.DoubleValue maxLaunchDistance;
    public final ModConfigSpec.DoubleValue maxLaunchSpeed;
    public final ModConfigSpec.DoubleValue mobDamageMin;
    public final ModConfigSpec.DoubleValue mobDamageMax;
    public final ModConfigSpec.DoubleValue wallBonusDamage;
    public final ModConfigSpec.DoubleValue maxMobLaunchSpeed;
    public final ModConfigSpec.IntValue raycastRange;

    RocketPunchConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Cooldown in ticks before Rocket Punch can be used again")
                .defineInRange("cooldownTicks", 60, 1, 2400);
        maxChargeTicks = builder
                .comment("Number of ticks to reach full charge")
                .defineInRange("maxChargeTicks", 40, 5, 200);
        maxLaunchDistance = builder
                .comment("Maximum distance (blocks) the player is launched at full charge")
                .defineInRange("maxLaunchDistance", 20.0, 1.0, 100.0);
        maxLaunchSpeed = builder
                .comment("Maximum horizontal speed (blocks/tick) of the player launch at full charge")
                .defineInRange("maxLaunchSpeed", 3.5, 0.1, 20.0);
        mobDamageMin = builder
                .comment("Damage dealt to mob at minimum charge")
                .defineInRange("mobDamageMin", 6.0, 0.0, 1000.0);
        mobDamageMax = builder
                .comment("Damage dealt to mob at full charge")
                .defineInRange("mobDamageMax", 18.0, 0.0, 1000.0);
        wallBonusDamage = builder
                .comment("Extra damage dealt when a launched mob collides with a wall")
                .defineInRange("wallBonusDamage", 10.0, 0.0, 1000.0);
        maxMobLaunchSpeed = builder
                .comment("Speed (blocks/tick) at which the struck mob is launched")
                .defineInRange("maxMobLaunchSpeed", 3.0, 0.1, 20.0);
        raycastRange = builder
                .comment("Raycast range (blocks) for detecting a mob in the punch path")
                .defineInRange("raycastRange", 24, 1, 64);
    }
}
