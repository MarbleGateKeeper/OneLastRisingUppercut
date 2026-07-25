package dev.marblegate.olru.common.core;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side particle bursts for gauntlet skills, built from vanilla particle types only
 * (plus colored {@link DustParticleOptions}). Spawned via {@link ServerLevel#sendParticles}.
 */
public final class GauntletParticleHelper {
    private GauntletParticleHelper() {}

    public static void muzzleFlash(ServerLevel level, Vec3 pos, int rgbColor) {
        level.sendParticles(new DustParticleOptions(rgbColor, 0.9f), pos.x, pos.y, pos.z, 3, 0.04, 0.04, 0.04, 0.0);
    }

    public static void rocketImpactBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 10, 0.35, 0.35, 0.35, 0.15);
    }

    public static void wallImpactBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 6, 0.25, 0.25, 0.25, 0.02);
    }

    public static void uppercutLaunch(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2.0 / 8;
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.x + Math.cos(angle) * 0.5, pos.y + 0.1, pos.z + Math.sin(angle) * 0.5,
                    1, 0.03, 0.02, 0.03, 0.02);
        }
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.5, pos.z, 6, 0.2, 0.5, 0.2, 0.12);
    }

    public static void slamDustFront(ServerLevel level, Vec3 pos, Vec3 facing) {
        Vec3 forward = new Vec3(facing.x, 0.0, facing.z);
        forward = forward.lengthSqr() > 1.0E-6 ? forward.normalize() : new Vec3(0.0, 0.0, 1.0);
        DustParticleOptions dust = new DustParticleOptions(0xFF9A30, 1.0f);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 12; i++) {
            double angle = (random.nextDouble() - 0.5) * Math.PI * 0.5;
            double distance = 0.4 + random.nextDouble() * 2.2;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 point = pos.add(
                    (forward.x * cos - forward.z * sin) * distance,
                    0.1 + random.nextDouble() * 0.3,
                    (forward.x * sin + forward.z * cos) * distance);
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0.05, 0.03, 0.05, 0.0);
        }
    }

    public static void meteorLandingExtras(ServerLevel level, Vec3 pos) {
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, -1), pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.3, pos.z, 6, 0.6, 0.2, 0.6, 0.0);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0 / 12;
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.x + Math.cos(angle) * 1.2, pos.y + 0.15, pos.z + Math.sin(angle) * 1.2,
                    1, 0.05, 0.02, 0.05, 0.03);
        }
    }

    public static void extractionPullStream(ServerLevel level, Vec3 from, Vec3 to) {
        DustParticleOptions dust = new DustParticleOptions(0x31E8FF, 0.8f);
        double distance = from.distanceTo(to);
        int steps = Math.min(12, Math.max(1, (int) (distance * 2.0)));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, (double) i / steps);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    public static void sedativeHit(ServerLevel level, Vec3 pos) {
        level.sendParticles(new DustParticleOptions(0xB04AD8, 1.0f), pos.x, pos.y, pos.z, 8, 0.25, 0.3, 0.25, 0.02);
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 4, 0.2, 0.25, 0.2, 0.02);
    }

    public static void nanoCastPillar(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 15; i++) {
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.2 + i * 0.14, pos.z, 1, 0.06, 0.02, 0.06, 0.03);
        }
    }
}
