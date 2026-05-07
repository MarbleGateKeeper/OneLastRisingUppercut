package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NanoSurgeConfig {
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.DoubleValue healAmount;
    public final ModConfigSpec.DoubleValue emergencyHealAmount;
    public final ModConfigSpec.DoubleValue emergencyHealthFraction;
    public final ModConfigSpec.IntValue buffTicks;
    public final ModConfigSpec.DoubleValue healingMultiplier;
    public final ModConfigSpec.IntValue regenerationAmplifier;
    public final ModConfigSpec.IntValue damageAmplifier;
    public final ModConfigSpec.IntValue resistanceAmplifier;
    public final ModConfigSpec.IntValue absorptionAmplifier;
    public final ModConfigSpec.DoubleValue chargePercentPerDamage;
    public final ModConfigSpec.DoubleValue chargePercentPerHealing;

    NanoSurgeConfig(ModConfigSpec.Builder builder) {
        range = builder
                .comment("Ally effect radius")
                .defineInRange("range", 16.0, 1.0, 64.0);
        healAmount = builder
                .comment("Immediate health restored to allies")
                .defineInRange("healAmount", 8.0, 0.0, 1000.0);
        emergencyHealAmount = builder
                .comment("Additional immediate health restored to critically wounded allies")
                .defineInRange("emergencyHealAmount", 12.0, 0.0, 1000.0);
        emergencyHealthFraction = builder
                .comment("Targets at or below this health fraction receive emergency aid")
                .defineInRange("emergencyHealthFraction", 0.3, 0.0, 1.0);
        buffTicks = builder
                .comment("Nano Surge buff duration")
                .defineInRange("buffTicks", 200, 1, 2400);
        healingMultiplier = builder
                .comment("Multiplier applied to healing received while Nano Surge is active")
                .defineInRange("healingMultiplier", 1.35, 1.0, 10.0);
        regenerationAmplifier = builder
                .comment("Regeneration amplifier")
                .defineInRange("regenerationAmplifier", 0, 0, 10);
        damageAmplifier = builder
                .comment("Damage boost amplifier")
                .defineInRange("damageAmplifier", 0, 0, 10);
        resistanceAmplifier = builder
                .comment("Damage resistance amplifier")
                .defineInRange("resistanceAmplifier", 0, 0, 10);
        absorptionAmplifier = builder
                .comment("Absorption amplifier for emergency death protection")
                .defineInRange("absorptionAmplifier", 1, 0, 10);
        chargePercentPerDamage = builder
                .comment("Nano Surge charge percent gained per point of damage dealt to a valid hostile target")
                .defineInRange("chargePercentPerDamage", 2.0, 0.0, 100.0);
        chargePercentPerHealing = builder
                .comment("Nano Surge charge percent gained per point of actual healing done to other friendly entities")
                .defineInRange("chargePercentPerHealing", 3.0, 0.0, 100.0);
    }
}
