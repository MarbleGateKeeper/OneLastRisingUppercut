package dev.marblegate.olru.common.entity;

import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.BioticOrbConfig;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BioticOrbEntity extends Projectile {
    private static final int PURPLE = 0xB04AD8;

    private int age;
    private double remainingDamagePool = -1.0;
    private Vec3 lastDirection = Vec3.ZERO;

    public BioticOrbEntity(EntityType<BioticOrbEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static BioticOrbEntity spawn(ServerLevel level, ServerPlayer owner, Vec3 startPos, Vec3 direction) {
        BioticOrbEntity orb = new BioticOrbEntity(OLRUEntityTypes.BIOTIC_ORB.get(), level);
        orb.setPos(startPos);
        orb.lastDirection = direction.normalize();
        orb.setDeltaMovement(orb.lastDirection.scale(OLRUConfig.FINAL_ANSWER.BIOTIC_ORB.speed.get()));
        orb.setOwner(owner);
        orb.remainingDamagePool = OLRUConfig.FINAL_ANSWER.BIOTIC_ORB.damagePool.get();
        level.addFreshEntity(orb);
        return orb;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        var cfg = OLRUConfig.FINAL_ANSWER.BIOTIC_ORB;
        if (remainingDamagePool < 0.0) {
            remainingDamagePool = cfg.damagePool.get();
        }
        age++;
        if (age > cfg.lifeTicks.get()) {
            burst(level);
            return;
        }

        updateVelocity(level, cfg);

        Vec3 delta = getDeltaMovement();
        Vec3 before = position();
        move(MoverType.SELF, delta);
        if (horizontalCollision || verticalCollision) {
            GauntletEffectBroadcaster.bioticOrbBounce(this, position());
            Vec3 moved = position().subtract(before);
            setDeltaMovement(
                    Math.abs(moved.x - delta.x) > 1.0E-7 ? -delta.x : delta.x,
                    Math.abs(moved.y - delta.y) > 1.0E-7 ? -delta.y : delta.y,
                    Math.abs(moved.z - delta.z) > 1.0E-7 ? -delta.z : delta.z);
        }

        spawnTrail(level);

        if (age % cfg.pulseIntervalTicks.get() == 0) {
            pulse(level, cfg);
        }
    }

    /** Slows down while enemies are in range so the orb lingers on them; keeps the current heading. */
    private void updateVelocity(ServerLevel level, BioticOrbConfig cfg) {
        Vec3 delta = getDeltaMovement();
        if (delta.lengthSqr() > 1.0E-8) {
            lastDirection = delta.normalize();
        }
        if (lastDirection.lengthSqr() < 1.0E-8) return;
        double speed = validEnemiesInRange(level, cfg).isEmpty() ? cfg.speed.get() : cfg.tetheredSpeed.get();
        setDeltaMovement(lastDirection.scale(speed));
    }

    private void spawnTrail(ServerLevel level) {
        if (age % 3 != 0) return;
        RandomSource random = level.getRandom();
        level.sendParticles(new DustParticleOptions(PURPLE, 0.9f),
                getX(), getY() + getBbHeight() * 0.5, getZ(),
                1, 0.05, 0.05, 0.05, 0.0);
    }

    private void pulse(ServerLevel level, BioticOrbConfig cfg) {
        List<LivingEntity> targets = validEnemiesInRange(level, cfg);
        if (targets.isEmpty()) return;

        Entity owner = getOwner();
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        float damage = (float) cfg.damagePerPulse.getAsDouble();
        for (LivingEntity target : targets) {
            target.hurt(OLRUDamageTypes.finalAnswerBioticOrb(level, ownerPlayer), damage);
            remainingDamagePool -= damage;
            GauntletEffectBroadcaster.orbTether(this, target, cfg.pulseIntervalTicks.get() + 2);
            Vec3 hit = target.position().add(0, target.getBbHeight() * 0.5, 0);
            level.sendParticles(new DustParticleOptions(PURPLE, 1.0f), hit.x, hit.y, hit.z, 6, 0.25, 0.3, 0.25, 0.02);
        }
        Vec3 soundAt = targets.get(0).position().add(0, targets.get(0).getBbHeight() * 0.5, 0);
        GauntletSoundHelper.orbPulse(level, soundAt);
        if (remainingDamagePool <= 0.0) {
            burst(level);
        }
    }

    private List<LivingEntity> validEnemiesInRange(ServerLevel level, BioticOrbConfig cfg) {
        double radius = cfg.radius.get();
        Entity owner = getOwner();
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        return level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e.isAlive()
                        && e != owner
                        && !e.isSpectator()
                        && e.distanceToSqr(this) <= radius * radius
                        && (ownerPlayer == null || !GauntletHelper.isFriendly(ownerPlayer, e)));
    }

    private void burst(ServerLevel level) {
        Vec3 pos = position();
        GauntletEffectBroadcaster.bioticOrbBurst(this, pos);
        level.sendParticles(new DustParticleOptions(PURPLE, 1.2f), pos.x, pos.y, pos.z, 8, 0.2, 0.2, 0.2, 0.04);
        discard();
    }
}
