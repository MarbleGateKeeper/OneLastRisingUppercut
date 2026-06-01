package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BioticGrenadeConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.DoubleValue throwSpeed;
    public final ModConfigSpec.DoubleValue throwInaccuracy;
    public final ModConfigSpec.DoubleValue explosionRadius;
    public final ModConfigSpec.DoubleValue healAmount;
    public final ModConfigSpec.IntValue regenerationTicks;
    public final ModConfigSpec.IntValue regenerationAmplifier;
    public final ModConfigSpec.DoubleValue damage;

    BioticGrenadeConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Cooldown in ticks before Biotic Grenade can be used again")
                .defineInRange("cooldownTicks", 360, 1, 2400);
        throwSpeed = builder
                .comment("Initial throw speed in blocks per tick")
                .defineInRange("throwSpeed", 1.25, 0.1, 6.0);
        throwInaccuracy = builder
                .comment("Projectile inaccuracy. Lower is more accurate.")
                .defineInRange("throwInaccuracy", 1.0, 0.0, 10.0);
        explosionRadius = builder
                .comment("Splash radius in blocks when the grenade hits an entity or block")
                .defineInRange("explosionRadius", 4.0, 0.5, 24.0);
        healAmount = builder
                .comment("Immediate health restored to allied targets in the splash")
                .defineInRange("healAmount", 8.0, 0.0, 1000.0);
        regenerationTicks = builder
                .comment("Regeneration duration applied to allied targets in the splash")
                .defineInRange("regenerationTicks", 100, 1, 2400);
        regenerationAmplifier = builder
                .comment("Regeneration amplifier applied to allied targets")
                .defineInRange("regenerationAmplifier", 0, 0, 10);
        damage = builder
                .comment("Damage dealt to hostile targets in the splash")
                .defineInRange("damage", 8.0, 0.0, 1000.0);
    }
}
