package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AccretionConfig {
    public final ModConfigSpec.DoubleValue damage;
    public final ModConfigSpec.DoubleValue splashDamage;
    public final ModConfigSpec.DoubleValue splashRadius;
    public final ModConfigSpec.IntValue knockdownTicks;
    public final ModConfigSpec.IntValue windupTicks;
    public final ModConfigSpec.IntValue cooldownTicks;

    AccretionConfig(ModConfigSpec.Builder builder) {
        damage = builder
                .comment("Damage dealt by a direct Accretion boulder hit")
                .defineInRange("damage", 8.0, 0.0, 100.0);
        splashDamage = builder
                .comment("Damage dealt by the Accretion splash to targets in its radius")
                .defineInRange("splashDamage", 4.0, 0.0, 100.0);
        splashRadius = builder
                .comment("Radius in blocks of the Accretion splash")
                .defineInRange("splashRadius", 3.0, 0.5, 16.0);
        knockdownTicks = builder
                .comment("Duration in ticks hit targets are knocked down")
                .defineInRange("knockdownTicks", 40, 0, 400);
        windupTicks = builder
                .comment("Ticks the Accretion boulder materializes in front of the caster before being thrown")
                .defineInRange("windupTicks", 8, 0, 100);
        cooldownTicks = builder
                .comment("Cooldown in ticks before Accretion can be used again")
                .defineInRange("cooldownTicks", 200, 1, 2400);
    }
}
