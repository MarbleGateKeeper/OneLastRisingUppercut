package dev.marblegate.olru.common.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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

    /** Colored dust trail along the view ray, like {@code GauntletEventHandlers#spawnBulletTrail}. */
    public static void coloredTrail(ServerPlayer shooter, @Nullable Vec3 hitCenter, double range, int rgbColor) {
        ServerLevel level = shooter.level();
        Vec3 origin = shooter.getEyePosition();
        Vec3 dir = shooter.getLookAngle();
        Vec3 endpoint = hitCenter != null ? hitCenter : origin.add(dir.scale(range));
        double dist = origin.distanceTo(endpoint);
        DustParticleOptions dust = new DustParticleOptions(rgbColor, 0.8f);
        for (double d = 0.5; d <= dist; d += 1.2) {
            Vec3 p = origin.add(dir.scale(d));
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

    /** Purple and gold dust sprinkled along the Coalescence beam, plus a little dragon breath. */
    public static void coalescenceBeam(ServerLevel level, Vec3 origin, Vec3 direction, double length) {
        DustParticleOptions purple = new DustParticleOptions(0x8A2BE2, 1.0f);
        DustParticleOptions gold = new DustParticleOptions(0xFFD75A, 0.9f);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 10; i++) {
            double dist = 1.0 + random.nextDouble() * length;
            Vec3 p = origin.add(direction.scale(dist)).add(
                    (random.nextDouble() - 0.5) * 0.8,
                    (random.nextDouble() - 0.5) * 0.8,
                    (random.nextDouble() - 0.5) * 0.8);
            level.sendParticles(random.nextBoolean() ? purple : gold, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 p = origin.add(direction.scale(1.5 + random.nextDouble() * length * 0.7));
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0f),
                    p.x, p.y, p.z, 1, 0.12, 0.12, 0.12, 0.0);
        }
    }

    /** Purple dust ring expanding from a Hypersphere implosion. */
    public static void hypersphereImplosion(ServerLevel level, Vec3 pos, double radius) {
        DustParticleOptions dust = new DustParticleOptions(0x9B4DFF, 1.1f);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2.0 / 8;
            level.sendParticles(dust,
                    pos.x + Math.cos(angle) * radius * 0.6, pos.y, pos.z + Math.sin(angle) * radius * 0.6,
                    1, 0.05, 0.05, 0.05, 0.0);
        }
        level.sendParticles(dust, pos.x, pos.y, pos.z, 4, 0.2, 0.2, 0.2, 0.02);
    }

    /** Tight purple freeze-burst where the Experimental Barrier annihilates a projectile. */
    public static void barrierAbsorbBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(new DustParticleOptions(0x9B4DFF, 0.9f), pos.x, pos.y, pos.z, 6, 0.12, 0.12, 0.12, 0.0);
    }

    /** Small purple shrink-burst when the Experimental Barrier is recalled. */
    public static void barrierRecallBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(new DustParticleOptions(0x9B4DFF, 1.0f), pos.x, pos.y, pos.z, 12, 0.6, 0.5, 0.15, 0.0);
    }

    /** Big purple shatter when the Experimental Barrier's durability breaks. */
    public static void barrierShatterBurst(ServerLevel level, Vec3 pos) {
        DustParticleOptions dust = new DustParticleOptions(0x9B4DFF, 1.2f);
        level.sendParticles(dust, pos.x, pos.y, pos.z, 40, 1.6, 1.0, 0.25, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10, 1.2, 0.8, 0.2, 0.06);
    }

    /** Stone debris and purple dust converging on the materializing Accretion boulder. */
    public static void accretionGather(ServerLevel level, Vec3 pos) {
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        DustParticleOptions dust = new DustParticleOptions(0x9B4DFF, 0.9f);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.7 + random.nextDouble() * 0.4;
            Vec3 offset = new Vec3(
                    Math.cos(angle) * radius, (random.nextDouble() - 0.5) * 0.6, Math.sin(angle) * radius);
            // count=0 turns the dist args into an exact velocity: drift inward, arriving in ~6 ticks.
            Vec3 velocity = offset.scale(-1.0 / 6.0);
            ParticleOptions option = random.nextInt(3) == 0 ? dust : debris;
            Vec3 p = pos.add(offset);
            level.sendParticles(option, p.x, p.y, p.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
        }
    }

    /** Stone-debris burst plus a purple flash where the Accretion boulder lands. */
    public static void accretionImpactBurst(ServerLevel level, Vec3 pos) {
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        level.sendParticles(debris, pos.x, pos.y, pos.z, 24, 0.4, 0.3, 0.4, 0.12);
        level.sendParticles(new DustParticleOptions(0x9B4DFF, 1.1f), pos.x, pos.y, pos.z, 8, 0.35, 0.25, 0.35, 0.02);
    }

    /** Small stone crumble when the Accretion boulder expires mid-flight. */
    public static void accretionCrumbleBurst(ServerLevel level, Vec3 pos) {
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        level.sendParticles(debris, pos.x, pos.y, pos.z, 8, 0.25, 0.2, 0.25, 0.06);
    }

    /** Rising purple spiral where a Gravitic Flux target is lifted. */
    public static void fluxLift(ServerLevel level, Vec3 pos, double liftHeight) {
        DustParticleOptions dust = new DustParticleOptions(0x9B4DFF, 1.0f);
        BlockParticleOption debris = surfaceDebris(level, pos);
        int points = 12;
        for (int i = 0; i < points; i++) {
            double t = (double) i / (points - 1);
            double angle = t * Math.PI * 4.0;
            level.sendParticles(dust,
                    pos.x + Math.cos(angle) * 0.5, pos.y + 0.2 + t * liftHeight, pos.z + Math.sin(angle) * 0.5,
                    1, 0.05, 0.05, 0.05, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(
                        debris,
                        pos.x + Math.cos(angle) * 0.65,
                        pos.y + 0.15 + t * liftHeight * 0.45,
                        pos.z + Math.sin(angle) * 0.65,
                        0,
                        -Math.cos(angle) * 0.025,
                        0.08,
                        -Math.sin(angle) * 0.025,
                        1.0);
            }
        }
    }

    /** Downward purple shock ring where Gravitic Flux slams its targets into the ground. */
    public static void fluxSlamShock(ServerLevel level, Vec3 pos, double radius) {
        DustParticleOptions dust = new DustParticleOptions(0x9B4DFF, 1.2f);
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2.0 / 24;
            Vec3 point = pos.add(Math.cos(angle) * radius * 0.8, 0.1, Math.sin(angle) * radius * 0.8);
            level.sendParticles(dust,
                    point.x, point.y + 0.3, point.z,
                    1, 0.05, 0.35, 0.05, 0.0);
            if (i % 2 == 0) {
                BlockParticleOption debris = surfaceDebris(level, point);
                level.sendParticles(
                        debris,
                        point.x,
                        point.y,
                        point.z,
                        0,
                        Math.cos(angle) * 0.16,
                        0.22,
                        Math.sin(angle) * 0.16,
                        1.0);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 2, radius * 0.3, 0.3, radius * 0.3, 0.0);
        level.sendParticles(dust, pos.x, pos.y + 0.5, pos.z, 16, radius * 0.5, 0.4, radius * 0.5, 0.02);
    }

    /** Compact terrain-colored burst for each target's individual Gravitic Flux landing. */
    public static void fluxTargetImpact(ServerLevel level, Vec3 pos) {
        BlockParticleOption debris = surfaceDebris(level, pos);
        level.sendParticles(debris, pos.x, pos.y + 0.08, pos.z, 18, 0.35, 0.08, 0.35, 0.18);
        level.sendParticles(
                new DustParticleOptions(0xD9CCFF, 1.0f),
                pos.x,
                pos.y + 0.18,
                pos.z,
                8,
                0.28,
                0.12,
                0.28,
                0.04);
    }

    private static BlockParticleOption surfaceDebris(ServerLevel level, Vec3 pos) {
        BlockPos origin = BlockPos.containing(pos.x, pos.y + 0.1, pos.z);
        for (int down = 0; down <= 4; down++) {
            BlockPos candidate = origin.below(down);
            BlockState state = level.getBlockState(candidate);
            if (!state.isAir()) return new BlockParticleOption(ParticleTypes.BLOCK, state);
        }
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    }
}
