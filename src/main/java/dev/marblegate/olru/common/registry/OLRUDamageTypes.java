package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.Nullable;

public class OLRUDamageTypes {
    public static final ResourceKey<DamageType> LEGACY_PRIME_HAND_CANNON = key("legacy_prime_hand_cannon");
    public static final ResourceKey<DamageType> LEGACY_PRIME_ROCKET_PUNCH = key("legacy_prime_rocket_punch");
    public static final ResourceKey<DamageType> LEGACY_PRIME_ROCKET_PUNCH_WALL_IMPACT = key("legacy_prime_rocket_punch_wall_impact");
    public static final ResourceKey<DamageType> LEGACY_PRIME_RISING_UPPERCUT = key("legacy_prime_rising_uppercut");
    public static final ResourceKey<DamageType> LEGACY_PRIME_SEISMIC_SLAM = key("legacy_prime_seismic_slam");
    public static final ResourceKey<DamageType> LEGACY_PRIME_METEOR_STRIKE = key("legacy_prime_meteor_strike");
    public static final ResourceKey<DamageType> LEGACY_OF_HORUS_BIOTIC_ROUND = key("legacy_of_horus_biotic_round");
    public static final ResourceKey<DamageType> LEGACY_OF_HORUS_SEDATIVE_DART = key("legacy_of_horus_sedative_dart");
    public static final ResourceKey<DamageType> LEGACY_OF_HORUS_BIOTIC_GRENADE = key("legacy_of_horus_biotic_grenade");

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(
                Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, path));
    }

    public static DamageSource legacyPrimeHandCannon(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_PRIME_HAND_CANNON, player);
    }

    public static DamageSource legacyPrimeRocketPunch(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_PRIME_ROCKET_PUNCH, player);
    }

    public static DamageSource legacyPrimeRisingUppercut(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_PRIME_RISING_UPPERCUT, player);
    }

    public static DamageSource legacyPrimeSeismicSlam(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_PRIME_SEISMIC_SLAM, player);
    }

    public static DamageSource legacyPrimeMeteorStrike(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_PRIME_METEOR_STRIKE, player);
    }

    public static DamageSource legacyOfHorusBioticRound(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_OF_HORUS_BIOTIC_ROUND, player);
    }

    public static DamageSource legacyOfHorusSedativeDart(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_OF_HORUS_SEDATIVE_DART, player);
    }

    public static DamageSource legacyOfHorusBioticGrenade(ServerLevel level, @Nullable ServerPlayer player) {
        return source(level, LEGACY_OF_HORUS_BIOTIC_GRENADE, player);
    }

    public static DamageSource source(
            ServerLevel level, ResourceKey<DamageType> damageType, @Nullable ServerPlayer player) {
        return player != null
                ? level.damageSources().source(damageType, player)
                : level.damageSources().generic();
    }
}
