package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SedativeDartConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.DoubleValue damage;
    public final ModConfigSpec.IntValue sleepTicks;
    public final ModConfigSpec.IntValue bossSleepTicks;
    public final ModConfigSpec.DoubleValue flyingDropSpeed;
    public final ModConfigSpec.IntValue particleCount;

    SedativeDartConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Cooldown in ticks before Sedative Dart can be used again")
                .defineInRange("cooldownTicks", 120, 1, 2400);
        range = builder
                .comment("Hitscan range (blocks)")
                .defineInRange("range", 28.0, 1.0, 80.0);
        damage = builder
                .comment("Light impact damage")
                .defineInRange("damage", 3.0, 0.0, 100.0);
        sleepTicks = builder
                .comment("AI suppression duration for normal hostile mobs")
                .defineInRange("sleepTicks", 70, 1, 600);
        bossSleepTicks = builder
                .comment("Reduced control duration for boss-like mobs. AI is not disabled for bosses.")
                .defineInRange("bossSleepTicks", 20, 0, 200);
        flyingDropSpeed = builder
                .comment("Downward speed applied each tick to airborne sedated targets")
                .defineInRange("flyingDropSpeed", 1.2, 0.0, 10.0);
        particleCount = builder
                .comment("Particles spawned at impact")
                .defineInRange("particleCount", 8, 0, 100);
    }
}
