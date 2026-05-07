package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FieldExtractionConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.IntValue maxChargeTicks;
    public final ModConfigSpec.DoubleValue allyLockRange;
    public final ModConfigSpec.DoubleValue allyConeAngleDegrees;
    public final ModConfigSpec.DoubleValue pullSpeed;
    public final ModConfigSpec.DoubleValue selfDashDistance;
    public final ModConfigSpec.DoubleValue selfDashSpeed;
    public final ModConfigSpec.DoubleValue healAmount;
    public final ModConfigSpec.IntValue protectionTicks;

    FieldExtractionConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Cooldown in ticks before Field Extraction can be used again")
                .defineInRange("cooldownTicks", 160, 1, 2400);
        maxChargeTicks = builder
                .comment("Ticks to reach full Field Extraction charge")
                .defineInRange("maxChargeTicks", 30, 5, 200);
        allyLockRange = builder
                .comment("Maximum radius in blocks for pulling allied entities during Field Extraction")
                .defineInRange("allyLockRange", 24.0, 1.0, 64.0);
        allyConeAngleDegrees = builder
                .comment("Forward cone angle in degrees for Field Extraction ally pulling")
                .defineInRange("allyConeAngleDegrees", 70.0, 5.0, 180.0);
        pullSpeed = builder
                .comment("Speed applied to pulled allies at full charge")
                .defineInRange("pullSpeed", 1.8, 0.1, 10.0);
        selfDashDistance = builder
                .comment("Fallback self reposition distance when no ally is locked")
                .defineInRange("selfDashDistance", 7.0, 1.0, 32.0);
        selfDashSpeed = builder
                .comment("Fallback self reposition speed")
                .defineInRange("selfDashSpeed", 1.4, 0.1, 10.0);
        healAmount = builder
                .comment("Health restored when the extraction completes")
                .defineInRange("healAmount", 6.0, 0.0, 100.0);
        protectionTicks = builder
                .comment("Duration of short protection after extraction")
                .defineInRange("protectionTicks", 60, 1, 600);
    }
}
