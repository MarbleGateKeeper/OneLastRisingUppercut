package dev.marblegate.olru.common.entity;

import dev.marblegate.olru.common.core.GauntletEventHandlers;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BioticGrenade extends ThrowableItemProjectile {
    private UUID ownerUUID;

    public BioticGrenade(EntityType<? extends BioticGrenade> entityType, Level level) {
        super(entityType, level);
    }

    public BioticGrenade(Level level, LivingEntity owner) {
        super(OLRUEntityTypes.BIOTIC_GRENADE.get(), owner, level, new ItemStack(Items.SPLASH_POTION));
        if (owner instanceof ServerPlayer player) {
            ownerUUID = player.getUUID();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(new DustParticleOptions(0x31E8FF, 0.7f), getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.MISS) return;
        if (!(level() instanceof ServerLevel level)) return;

        explode(level, result.getLocation());
        discard();
    }

    private void explode(ServerLevel level, Vec3 center) {
        var cfg = OLRUConfig.HORUS.BIOTIC_GRENADE;
        GauntletSoundHelper.grenadeExplode(level, center);
        ServerPlayer owner = ownerPlayer(level);
        double radius = cfg.explosionRadius.get();
        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) <= radius * radius);

        for (LivingEntity target : targets) {
            if (owner != null && GauntletHelper.isFriendly(owner, target)) {
                healAlly(owner, target, cfg.healAmount.getAsDouble());
                target.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION,
                        cfg.regenerationTicks.get(),
                        cfg.regenerationAmplifier.get()));
                spawnAllyHitParticles(level, target);
            } else {
                target.hurt(OLRUDamageTypes.legacyOfHorusBioticGrenade(level, owner),
                        (float) cfg.damage.getAsDouble());
                spawnEnemyHitParticles(level, target);
            }
        }

        spawnExplosionField(level, center, radius);
    }

    private void spawnExplosionField(ServerLevel level, Vec3 center, double radius) {
        level.sendParticles(
                ParticleTypes.SPLASH,
                center.x, center.y, center.z,
                28, radius * 0.22, radius * 0.1, radius * 0.22, 0.2);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                center.x, center.y, center.z,
                18, radius * 0.18, radius * 0.08, radius * 0.18, 0.08);

        int ringSteps = Math.max(32, (int) Math.ceil(radius * 12.0));
        for (int i = 0; i < ringSteps; i++) {
            double angle = i * Math.PI * 2.0 / ringSteps;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 1, 0.035, 0.02, 0.035, 0.01);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 1, 0.03, 0.015, 0.03, 0.04);
            }
        }

        var random = level.getRandom();
        int cloudCount = Math.clamp((int) (radius * radius * 3.0), 28, 140);
        for (int i = 0; i < cloudCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            Vec3 point = center.add(
                    Math.cos(angle) * distance,
                    0.1 + random.nextDouble() * 1.15,
                    Math.sin(angle) * distance);
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.06, 0.04, 0.06, 0.015);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }
    }

    private void spawnAllyHitParticles(ServerLevel level, LivingEntity target) {
        Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 12, 0.25, 0.35, 0.25, 0.08);
        level.sendParticles(ParticleTypes.HEART, pos.x, pos.y + 0.15, pos.z, 2, 0.18, 0.18, 0.18, 0.02);
    }

    private void spawnEnemyHitParticles(ServerLevel level, LivingEntity target) {
        Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        level.sendParticles(ParticleTypes.SNEEZE, pos.x, pos.y, pos.z, 10, 0.24, 0.28, 0.24, 0.06);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 6, 0.18, 0.22, 0.18, 0.04);
    }

    private void healAlly(ServerPlayer owner, LivingEntity target, double amount) {
        if (amount <= 0.0) return;
        float before = target.getHealth();
        target.heal((float) amount);
        float healed = Math.max(0.0F, target.getHealth() - before);
        if (target == owner || healed <= 0.0F) return;
        GauntletEventHandlers.addHorusNanoCharge(
                owner,
                healed * (float) (OLRUConfig.HORUS.NANO_SURGE.chargePercentPerHealing.get() / 100.0));
    }

    private ServerPlayer ownerPlayer(ServerLevel level) {
        Entity owner = getOwner();
        if (owner instanceof ServerPlayer player) return player;
        if (ownerUUID == null) return null;
        return level.getServer().getPlayerList().getPlayer(ownerUUID);
    }
}
