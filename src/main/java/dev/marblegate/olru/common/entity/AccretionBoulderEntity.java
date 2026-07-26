package dev.marblegate.olru.common.entity;

import dev.marblegate.olru.common.core.AxiomEffectTracker;
import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.Comparator;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sigma's Accretion: a lobbed debris boulder with an explicit gravity arc. The direct-hit target
 * is knocked down (NoAI for mobs, heavy slowness for players) and the impact splashes nearby
 * enemies.
 *
 * <p>After spawning, the boulder first materializes: for {@code windupTicks} it hovers weightless
 * in front of the caster's face (re-anchored to the caster's current eye position and look every
 * tick) while debris converges, then it is thrown along the caster's look and the arc begins.
 */
public class AccretionBoulderEntity extends Projectile {
    private static final double LAUNCH_SPEED = 1.2;
    private static final double LAUNCH_LOFT = 0.25;
    private static final double GRAVITY_PER_TICK = 0.045;
    private static final int MAX_AGE_TICKS = 100;
    private static final double HOVER_DISTANCE = 1.2;
    private static final double HOVER_DROP = 0.25;

    private int age;
    private int windupTicksRemaining;
    /** Fallback throw direction, used when the owner is gone by the time the windup ends. */
    private Vec3 launchDirection = Vec3.ZERO;

    public AccretionBoulderEntity(EntityType<AccretionBoulderEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AccretionBoulderEntity spawn(ServerLevel level, ServerPlayer owner, Vec3 startPos, Vec3 direction) {
        AccretionBoulderEntity boulder = new AccretionBoulderEntity(OLRUEntityTypes.ACCRETION_BOULDER.get(), level);
        boulder.setPos(startPos);
        boulder.setOwner(owner);
        boulder.launchDirection = direction.normalize();
        boulder.windupTicksRemaining = OLRUConfig.THE_AXIOM.ACCRETION.windupTicks.get();
        if (boulder.windupTicksRemaining <= 0) {
            boulder.launch(level, owner);
        } else {
            GauntletSoundHelper.accretionGather(level, startPos, 0f);
        }
        level.addFreshEntity(boulder);
        return boulder;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        if (++age > MAX_AGE_TICKS) {
            GauntletParticleHelper.accretionCrumbleBurst(level, centerPos());
            discard();
            return;
        }

        if (windupTicksRemaining > 0) {
            tickWindup(level);
            return;
        }

        // Projectile has no gravity of its own; the boulder arcs via an explicit per-tick pull.
        Vec3 delta = getDeltaMovement().add(0.0, -GRAVITY_PER_TICK, 0.0);
        setDeltaMovement(delta);
        Vec3 before = position();
        move(MoverType.SELF, delta);

        LivingEntity hit = firstHitEntity(level, before);
        if (hit != null || horizontalCollision || verticalCollision) {
            impact(level, hit);
        }
    }

    /** Windup hover: pinned to the owner's current eye/look, no gravity, no hits, no collisions. */
    private void tickWindup(ServerLevel level) {
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            // Owner is gone: skip straight to the throw along the spawn direction.
            windupTicksRemaining = 0;
            launch(level, null);
            return;
        }
        windupTicksRemaining--;
        setDeltaMovement(Vec3.ZERO);
        setPos(hoverPosition(player));
        GauntletParticleHelper.accretionGather(level, centerPos());
        var cfg = OLRUConfig.THE_AXIOM.ACCRETION;
        if (windupTicksRemaining <= 0) {
            launch(level, player);
        } else if (windupTicksRemaining % 2 == 0) {
            float progress = 1f - (float) windupTicksRemaining / cfg.windupTicks.get();
            GauntletSoundHelper.accretionGather(level, centerPos(), progress);
        }
    }

    private static Vec3 hoverPosition(ServerPlayer owner) {
        return owner.getEyePosition().add(owner.getLookAngle().scale(HOVER_DISTANCE)).subtract(0.0, HOVER_DROP, 0.0);
    }

    /** Throws the boulder along the owner's current look (or the spawn direction as a fallback). */
    private void launch(ServerLevel level, @Nullable ServerPlayer owner) {
        Vec3 direction = owner != null ? owner.getLookAngle().normalize() : launchDirection;
        setDeltaMovement(direction.scale(LAUNCH_SPEED).add(0.0, LAUNCH_LOFT, 0.0));
        GauntletSoundHelper.accretionThrow(level, centerPos());
    }

    private Vec3 centerPos() {
        return position().add(0.0, getBbHeight() * 0.5, 0.0);
    }

    private LivingEntity firstHitEntity(ServerLevel level, Vec3 before) {
        AABB sweepBox = getBoundingBox().expandTowards(before.subtract(position())).inflate(0.3);
        Entity owner = getOwner();
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        return level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                e -> e.isAlive()
                        && !e.isSpectator()
                        && e != owner
                        && e.isPickable()
                        && (ownerPlayer == null || !GauntletHelper.isFriendly(ownerPlayer, e)))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(before)))
                .orElse(null);
    }

    private void impact(ServerLevel level, @Nullable LivingEntity directHit) {
        var cfg = OLRUConfig.THE_AXIOM.ACCRETION;
        Vec3 pos = position();
        Entity owner = getOwner();
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;

        if (directHit != null) {
            directHit.hurt(OLRUDamageTypes.axiomAccretion(level, ownerPlayer), (float) cfg.damage.getAsDouble());
            knockdown(directHit, cfg.knockdownTicks.get());
        }

        double radius = cfg.splashRadius.get();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e.isAlive()
                        && !e.isSpectator()
                        && e != owner
                        && e != directHit
                        && e.distanceToSqr(pos) <= radius * radius
                        && (ownerPlayer == null || !GauntletHelper.isFriendly(ownerPlayer, e)))) {
            target.hurt(OLRUDamageTypes.axiomAccretion(level, ownerPlayer), (float) cfg.splashDamage.getAsDouble());
        }

        GauntletEffectBroadcaster.seismicSlamRing(level, pos, (float) radius);
        GauntletParticleHelper.accretionImpactBurst(level, centerPos());
        GauntletSoundHelper.accretionImpact(level, pos);
        discard();
    }

    /** Mobs lose their AI for the duration; players get a crippling slowness instead. */
    private void knockdown(LivingEntity target, int ticks) {
        if (target instanceof Mob mob) {
            AxiomEffectTracker.knockdown(mob, ticks);
        } else if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4, false, true));
        }
    }
}
