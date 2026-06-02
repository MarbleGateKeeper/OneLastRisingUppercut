package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class HandCannonConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.IntValue maxCharges;
    public final ModConfigSpec.DoubleValue damage;
    public final ModConfigSpec.DoubleValue range;
    public final ModConfigSpec.IntValue particleCount;

    HandCannonConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Ticks to refill one Hand Cannon ammo charge")
                .defineInRange("cooldownTicks", 12, 1, 1200);
        maxCharges = builder
                .comment("Maximum number of Hand Cannon charges")
                .defineInRange("maxCharges", 4, 1, 20);
        damage = builder
                .comment("Damage dealt by Hand Cannon")
                .defineInRange("damage", 8.0, 0.0, 1000.0);
        range = builder
                .comment("Hitscan range (blocks) — how far the shot can reach")
                .defineInRange("range", 20.0, 1.0, 64.0);
        particleCount = builder
                .comment("Number of CRIT particles spawned on hit")
                .defineInRange("particleCount", 12, 0, 100);
    }
}
