package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class LegacyPrimeGauntletConfig {
    public final RocketPunchConfig ROCKET_PUNCH;
    public final RisingUppercutConfig RISING_UPPERCUT;
    public final SeismicSlamConfig SEISMIC_SLAM;
    public final MeteorStrikeConfig METEOR_STRIKE;
    public final HandCannonConfig HAND_CANNON;

    LegacyPrimeGauntletConfig(ModConfigSpec.Builder builder) {
        builder.push("rocket_punch").comment("Rocket Punch - charge and release to launch horizontally");
        ROCKET_PUNCH = new RocketPunchConfig(builder);
        builder.pop();

        builder.push("rising_uppercut").comment("Rising Uppercut - launch self and nearby mobs upward");
        RISING_UPPERCUT = new RisingUppercutConfig(builder);
        builder.pop();

        builder.push("seismic_slam").comment("Seismic Slam - leap forward and slam down to damage and slow enemies in front");
        SEISMIC_SLAM = new SeismicSlamConfig(builder);
        builder.pop();

        builder.push("meteor_strike").comment("Meteor Strike - teleport up then slam down for massive AoE");
        METEOR_STRIKE = new MeteorStrikeConfig(builder);
        builder.pop();

        builder.push("hand_cannon").comment("Hand Cannon - fire a shot with particle effect");
        HAND_CANNON = new HandCannonConfig(builder);
        builder.pop();
    }
}
