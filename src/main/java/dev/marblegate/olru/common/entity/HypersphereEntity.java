package dev.marblegate.olru.common.entity;

import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HypersphereEntity extends Projectile {
    public static final int PURPLE = 0x9B4DFF;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(HypersphereEntity.class, EntityDataSerializers.ITEM_STACK);

    private Vec3 launchDirection = Vec3.ZERO;
    private int launchDelayTicks;
    private boolean bounced;
    private double distanceTraveled;
    private int age;
    private int maxLifetimeTicks = Integer.MAX_VALUE;

    public HypersphereEntity(EntityType<HypersphereEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static HypersphereEntity spawn(ServerLevel level, ServerPlayer owner, Vec3 startPos, Vec3 direction, int launchDelayTicks) {
        var cfg = OLRUConfig.THE_AXIOM.HYPERSPHERES;
        HypersphereEntity sphere = new HypersphereEntity(OLRUEntityTypes.HYPERSPHERE.get(), level);
        sphere.setPos(startPos);
        sphere.launchDirection = direction.normalize();
        sphere.launchDelayTicks = launchDelayTicks;
        // Safety net: even a sphere that somehow never moved is discarded after outliving its
        // expected flight time.
        sphere.maxLifetimeTicks = (int) (cfg.range.get() / cfg.speed.get()) + 40;
        if (launchDelayTicks == 0) {
            sphere.setDeltaMovement(sphere.launchDirection.scale(cfg.speed.get()));
        }
        sphere.setOwner(owner);
        sphere.setItem(pickDebrisItem(owner));
        level.addFreshEntity(sphere);
        return sphere;
    }

    /** The sphere is rendered as a random piece of debris lifted from the owner's inventory. */
    private static ItemStack pickDebrisItem(ServerPlayer owner) {
        List<ItemStack> candidates = new ArrayList<>();
        for (ItemStack stack : owner.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) candidates.add(stack);
        }
        if (!owner.getOffhandItem().isEmpty()) candidates.add(owner.getOffhandItem());
        if (candidates.isEmpty()) return new ItemStack(Items.STONE);
        return candidates.get(owner.getRandom().nextInt(candidates.size())).copy();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItem() {
        return getEntityData().get(DATA_ITEM);
    }

    private void setItem(ItemStack stack) {
        getEntityData().set(DATA_ITEM, stack);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        var cfg = OLRUConfig.THE_AXIOM.HYPERSPHERES;

        if (++age > maxLifetimeTicks) {
            // Nothing else killed it (e.g. a sphere that never launched): tiny purple puff, gone.
            level.sendParticles(new DustParticleOptions(PURPLE, 0.8f),
                    getX(), getY() + getBbHeight() * 0.5, getZ(),
                    6, 0.1, 0.1, 0.1, 0.0);
            discard();
            return;
        }

        if (launchDelayTicks > 0) {
            // Second sphere of a pair: hover in place until its launch tick.
            launchDelayTicks--;
            setDeltaMovement(Vec3.ZERO);
            if (launchDelayTicks == 0) {
                setDeltaMovement(launchDirection.scale(cfg.speed.get()));
            }
            return;
        }

        Vec3 delta = getDeltaMovement();
        if (delta.lengthSqr() < 1.0E-8) return;
        Vec3 before = position();
        move(MoverType.SELF, delta);
        distanceTraveled += delta.length();

        LivingEntity hit = firstHitEntity(level, before);
        if (hit != null) {
            Entity owner = getOwner();
            hit.hurt(
                    OLRUDamageTypes.axiomHyperspheres(level, owner instanceof ServerPlayer player ? player : null),
                    (float) cfg.directDamage.getAsDouble());
            implode(level);
            return;
        }

        if (horizontalCollision || verticalCollision) {
            if (bounced) {
                implode(level);
                return;
            }
            bounced = true;
            Vec3 moved = position().subtract(before);
            setDeltaMovement(
                    Math.abs(moved.x - delta.x) > 1.0E-7 ? -delta.x : delta.x,
                    Math.abs(moved.y - delta.y) > 1.0E-7 ? -delta.y : delta.y,
                    Math.abs(moved.z - delta.z) > 1.0E-7 ? -delta.z : delta.z);
        }

        spawnTrail(level);

        if (distanceTraveled > cfg.range.get()) {
            implode(level);
        }
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

    private void spawnTrail(ServerLevel level) {
        level.sendParticles(new DustParticleOptions(PURPLE, 0.8f),
                getX(), getY() + getBbHeight() * 0.5, getZ(),
                1, 0.05, 0.05, 0.05, 0.0);
    }

    private void implode(ServerLevel level) {
        var cfg = OLRUConfig.THE_AXIOM.HYPERSPHERES;
        Vec3 pos = position();
        double radius = cfg.implosionRadius.get();
        Entity owner = getOwner();
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e.isAlive()
                        && !e.isSpectator()
                        && e != owner
                        && e.distanceToSqr(pos) <= radius * radius
                        && (ownerPlayer == null || !GauntletHelper.isFriendly(ownerPlayer, e)));
        for (LivingEntity target : targets) {
            target.hurt(OLRUDamageTypes.axiomHypersphereImplosion(level, ownerPlayer),
                    (float) cfg.implosionDamage.getAsDouble());
        }
        GauntletParticleHelper.hypersphereImplosion(level, pos, radius);
        GauntletSoundHelper.hypersphereImplode(level, pos);
        discard();
    }
}
