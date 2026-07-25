package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CoalescenceConfig {
    public final ModConfigSpec.IntValue durationTicks;
    public final ModConfigSpec.DoubleValue length;
    public final ModConfigSpec.DoubleValue radius;
    public final ModConfigSpec.DoubleValue enemyDamagePerPulse;
    public final ModConfigSpec.DoubleValue allyHealPerPulse;
    public final ModConfigSpec.DoubleValue selfHealPerPulse;
    public final ModConfigSpec.IntValue pulseIntervalTicks;
    public final ModConfigSpec.IntValue speedAmplifier;
    public final ModConfigSpec.DoubleValue chargePercentPerDamage;

    CoalescenceConfig(ModConfigSpec.Builder builder) {
        durationTicks = builder
                .comment("Duration in ticks of the Coalescence beam")
                .defineInRange("durationTicks", 160, 20, 1200);
        length = builder
                .comment("Length in blocks of the Coalescence beam")
                .defineInRange("length", 30.0, 5.0, 64.0);
        radius = builder
                .comment("Radius in blocks around the beam axis in which targets are affected")
                .defineInRange("radius", 1.5, 0.5, 8.0);
        enemyDamagePerPulse = builder
                .comment("Damage dealt per pulse to each hostile target in the beam")
                .defineInRange("enemyDamagePerPulse", 3.0, 0.0, 100.0);
        allyHealPerPulse = builder
                .comment("Health restored per pulse to each allied target in the beam")
                .defineInRange("allyHealPerPulse", 4.0, 0.0, 100.0);
        selfHealPerPulse = builder
                .comment("Health restored per pulse to the wielder while channeling")
                .defineInRange("selfHealPerPulse", 2.0, 0.0, 100.0);
        pulseIntervalTicks = builder
                .comment("Interval in ticks between beam pulses")
                .defineInRange("pulseIntervalTicks", 5, 1, 100);
        speedAmplifier = builder
                .comment("Speed effect amplifier while channeling Coalescence")
                .defineInRange("speedAmplifier", 1, 0, 9);
        chargePercentPerDamage = builder
                .comment("Coalescence charge percent gained per point of hostile damage dealt")
                .defineInRange("chargePercentPerDamage", 0.35, 0.0, 100.0);
    }
}
