package dev.marblegate.olru.common.core;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side sound cues for gauntlet skills, built from vanilla {@link SoundEvents} only.
 * Played via {@link ServerLevel#playSound} in the {@link SoundSource#PLAYERS} category.
 */
public final class GauntletSoundHelper {
    private GauntletSoundHelper() {}

    public static void handCannon(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.6f);
    }

    public static void bioticRound(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.9f);
    }

    public static void bioticHeal(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
    }

    public static void rocketChargeStart(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.BEACON_ACTIVATE, 0.4f, 1.8f);
    }

    public static void rocketChargeTick(ServerLevel level, Vec3 pos, float charge) {
        play(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.25f, 1.0f + Math.clamp(charge, 0f, 1f) * 0.8f);
    }

    public static void rocketLaunch(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        play(level, pos, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.2f);
    }

    public static void rocketImpact(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
    }

    public static void rocketWallImpact(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
        play(level, pos, SoundEvents.ANVIL_LAND, 0.5f, 1.5f);
    }

    public static void risingUppercut(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 0.9f);
        play(level, pos, SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.6f);
    }

    public static void slamLeap(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 0.9f);
    }

    public static void slamLand(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.GENERIC_EXPLODE, 0.6f, 0.9f);
        play(level, pos, SoundEvents.ANVIL_LAND, 0.5f, 0.7f);
    }

    public static void meteorHoverStart(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.2f);
    }

    public static void meteorDive(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 0.6f);
    }

    public static void meteorLand(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.GENERIC_EXPLODE, 1.0f, 0.7f);
        play(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 1.2f);
    }

    public static void extractionChannel(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.BEACON_AMBIENT, 0.3f, 1.5f);
    }

    public static void extractionRelease(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.CONDUIT_ACTIVATE, 0.7f, 1.2f);
    }

    public static void sedativeFire(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.3f);
    }

    public static void sedativeHit(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.WOOL_PLACE, 0.8f, 0.6f);
        play(level, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f, 0.7f);
    }

    public static void grenadeThrow(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.SNOWBALL_THROW, 0.8f, 1.0f);
    }

    public static void grenadeExplode(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.SPLASH_POTION_BREAK, 1.0f, 0.8f);
        play(level, pos, SoundEvents.BREWING_STAND_BREW, 0.7f, 1.2f);
    }

    public static void nanoSurge(ServerLevel level, Vec3 pos) {
        play(level, pos, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.1f);
        play(level, pos, SoundEvents.CONDUIT_ACTIVATE, 0.8f, 1.0f);
    }

    private static void play(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void play(ServerLevel level, Vec3 pos, Holder<SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
