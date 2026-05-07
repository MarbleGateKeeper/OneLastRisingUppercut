package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BioticRoundConfig {
    public final ModConfigSpec.IntValue cooldownTicks;
    public final ModConfigSpec.IntValue maxCharges;
    public final ModConfigSpec.DoubleValue effectiveRange;
    public final ModConfigSpec.DoubleValue healAmount;
    public final ModConfigSpec.DoubleValue damage;
    public final ModConfigSpec.IntValue particleCount;

    BioticRoundConfig(ModConfigSpec.Builder builder) {
        cooldownTicks = builder
                .comment("Ticks to refill all Biotic Round charges")
                .defineInRange("cooldownTicks", 60, 1, 1200);
        maxCharges = builder
                .comment("Maximum number of Biotic Round charges")
                .defineInRange("maxCharges", 6, 1, 20);
        effectiveRange = builder
                .comment("Effective hitscan range in blocks. Defaults well above Legacy Prime Hand Cannon's 20-block range.")
                .defineInRange("effectiveRange", 36.0, 1.0, 96.0);
        healAmount = builder
                .comment("Health restored to allied targets")
                .defineInRange("healAmount", 4.0, 0.0, 100.0);
        damage = builder
                .comment("Damage dealt to hostile targets")
                .defineInRange("damage", 4.0, 0.0, 100.0);
        particleCount = builder
                .comment("Particles spawned at impact")
                .defineInRange("particleCount", 8, 0, 100);
    }
}
