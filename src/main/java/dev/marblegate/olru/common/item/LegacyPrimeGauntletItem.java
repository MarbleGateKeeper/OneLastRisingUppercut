package dev.marblegate.olru.common.item;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.ConditionalChargeState;
import dev.marblegate.olru.common.attachment.skill.CooldownSkillState;
import dev.marblegate.olru.common.attachment.skill.IncrementalChargeState;
import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletEventHandlers;
import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.core.movement.MovementTaskAssignmentResult;
import dev.marblegate.olru.common.core.movement.MovementTaskProperties;
import dev.marblegate.olru.common.core.movement.task.EntityPushTask;
import dev.marblegate.olru.common.core.movement.task.MeteorStrikeTask;
import dev.marblegate.olru.common.core.movement.task.RocketPunchTask;
import dev.marblegate.olru.common.core.movement.task.SeismicSlamTask;
import dev.marblegate.olru.common.item.tooltip.GauntletTooltipHelper;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.MeteorStrikeConfig;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LegacyPrimeGauntletItem extends AbstractGauntletItem {
    public LegacyPrimeGauntletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Identifier gauntletId() {
        return Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "legacy_prime");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        GauntletTooltipHelper.appendLegacyPrime(tooltip, flag.hasShiftDown());
    }

    @Override
    public GauntletSkillGroup createDefaultSkillGroup() {
        return new GauntletSkillGroup(
                new IncrementalChargeState(
                        OLRUConfig.LEGACY_PRIME.HAND_CANNON.cooldownTicks,
                        OLRUConfig.LEGACY_PRIME.HAND_CANNON.maxCharges),
                new CooldownSkillState(OLRUConfig.LEGACY_PRIME.ROCKET_PUNCH.cooldownTicks),
                new CooldownSkillState(OLRUConfig.LEGACY_PRIME.RISING_UPPERCUT.cooldownTicks),
                new CooldownSkillState(OLRUConfig.LEGACY_PRIME.SEISMIC_SLAM.cooldownTicks),
                new ConditionalChargeState());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        int ticksHeld = getUseDuration(stack, entity) - remainingUseDuration;
        float chargePercent = Math.min(1f, (float) ticksHeld / getMaxChargeTicks());
        if (ticksHeld == 0) {
            GauntletSoundHelper.rocketChargeStart(player.level(), player.position());
        } else if (ticksHeld % 5 == 0) {
            GauntletSoundHelper.rocketChargeTick(player.level(), player.position(), chargePercent);
        }
        GauntletEffectBroadcaster.rocketCharge(player, chargePercent, 4);
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.ROCKET_PUNCH_CHARGE, chargePercent, 4);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        boolean result = super.releaseUsing(stack, level, entity, timeCharged);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            GauntletEffectBroadcaster.stopRocketCharge(player);
            GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.ROCKET_PUNCH_CHARGE);
        }
        return result;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        super.onStopUsing(stack, entity, count);
        if (!entity.level().isClientSide() && entity instanceof ServerPlayer player) {
            GauntletEffectBroadcaster.stopRocketCharge(player);
            GauntletEffectBroadcaster.stopPose(player, GauntletPoseType.ROCKET_PUNCH_CHARGE);
        }
    }

    @Override
    public void performSkillOne(ServerPlayer player, float chargePercent) {
        if (!isSkillReady(player, SkillType.SKILL_ONE)) return;
        var cfg = OLRUConfig.LEGACY_PRIME.ROCKET_PUNCH;
        UUID uuid = player.getUUID();
        Vec3 look = player.getLookAngle();
        Vec3 launchDir = new Vec3(look.x, 0, look.z).normalize();
        double speed = cfg.maxLaunchSpeed.get() * chargePercent;
        double distance = cfg.maxLaunchDistance.get() * chargePercent;
        float damage = (float) (cfg.mobDamageMin.get()
                + (cfg.mobDamageMax.get() - cfg.mobDamageMin.get()) * chargePercent);
        MovementTaskAssignmentResult result = MovementManager.assign(player, new RocketPunchTask(
                launchDir.scale(speed), distance,
                damage, (float) cfg.wallBonusDamage.getAsDouble() * chargePercent,
                (float) cfg.maxMobLaunchSpeed.getAsDouble() * chargePercent,
                uuid), MovementTaskProperties.playerActive(uuid));
        if (!result.accepted()) return;
        consumeSkill(player, SkillType.SKILL_ONE);
    }

    @Override
    public void performSkillTwo(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.SKILL_TWO)) return;
        var cfg = OLRUConfig.LEGACY_PRIME.RISING_UPPERCUT;
        ServerLevel level = player.level();
        DamageSource src = OLRUDamageTypes.legacyPrimeRisingUppercut(level, player);
        MovementTaskAssignmentResult result = MovementManager.assign(player, new EntityPushTask(
                new Vec3(0, cfg.riseSpeedPlayer.get(), 0), cfg.riseHeight.get(), true),
                MovementTaskProperties.playerActive(player.getUUID()));
        if (!result.accepted()) return;

        List<LivingEntity> targets = GauntletHelper.entitiesInFrontCone(
                player, cfg.frontConeRange.get(), cfg.frontConeAngleDegrees.get());
        for (LivingEntity mob : targets) {
            mob.hurt(src, (float) cfg.mobDamage.getAsDouble());
            EntityPushTask task = new EntityPushTask(new Vec3(0, cfg.riseSpeedMob.get(), 0), cfg.riseHeight.get(), mob instanceof ServerPlayer);
            MovementManager.assign(mob, task, MovementTaskProperties.externalKnockback(player.getUUID()));
        }
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.RISING_UPPERCUT, 0, 12);
        GauntletParticleHelper.uppercutLaunch(level, player.position());
        GauntletEffectBroadcaster.uppercutBurst(level, player.position());
        GauntletSoundHelper.risingUppercut(level, player.position());
        consumeSkill(player, SkillType.SKILL_TWO);
    }

    @Override
    public void performSkillThree(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.SKILL_THREE)) return;
        var cfg = OLRUConfig.LEGACY_PRIME.SEISMIC_SLAM;

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(0.0, 0.0, 1.0);
        Vec3 initialVelocity = horizontal.normalize()
                .scale(cfg.leapForwardSpeed.get())
                .add(0.0, cfg.leapUpSpeed.get(), 0.0);

        MovementTaskAssignmentResult result = MovementManager.assign(player, new SeismicSlamTask(
                initialVelocity,
                cfg.gravity.get(),
                cfg.maxTravelTicks.get(),
                cfg.impactRange.get(),
                cfg.impactConeAngleDegrees.get(),
                (float) cfg.minDamage.getAsDouble(),
                (float) cfg.maxDamage.getAsDouble(),
                cfg.fullDamageAirTicks.get(),
                cfg.slowTicks.get(),
                cfg.slowAmplifier.get(),
                player.getUUID()),
                MovementTaskProperties.playerActive(player.getUUID()));
        if (!result.accepted()) return;

        consumeSkill(player, SkillType.SKILL_THREE);
    }

    @Override
    public void performUltimate(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.ULTIMATE)) return;
        var cfg = OLRUConfig.LEGACY_PRIME.METEOR_STRIKE;
        ServerLevel level = player.level();
        UUID playerUUID = player.getUUID();
        double startY = level.getMaxY() - cfg.teleportHeightOffset.get();
        double maxFallDistance = meteorFallDistanceToVoid(level, startY);
        MovementTaskAssignmentResult result = MovementManager.assign(player,
                new MeteorStrikeTask(
                        cfg.hoverTicks.get(), cfg.fallSpeed.get(), maxFallDistance,
                        (float) cfg.innerRadius.getAsDouble(), (float) cfg.outerRadius.getAsDouble(), () -> {
                            ServerPlayer p = level.getServer().getPlayerList().getPlayer(playerUUID);
                            if (p != null) triggerMeteorLanding(p, cfg);
                        }),
                MovementTaskProperties.meteorStrike(playerUUID));
        if (!result.accepted()) return;

        player.teleportTo(player.getX(), startY, player.getZ());
        consumeSkill(player, SkillType.ULTIMATE);
    }

    private static double meteorFallDistanceToVoid(ServerLevel level, double startY) {
        return Math.max(0.0, startY - (level.getMinY() - 128.0));
    }

    private static void triggerMeteorLanding(ServerPlayer player, MeteorStrikeConfig cfg) {
        ServerLevel level = player.level();
        Vec3 pos = player.position();
        double inner = cfg.innerRadius.get();
        double outer = cfg.outerRadius.get();
        AABB searchBox = new AABB(pos.x - outer, pos.y - outer, pos.z - outer,
                pos.x + outer, pos.y + outer, pos.z + outer);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.distanceTo(player) <= outer);
        DamageSource src = OLRUDamageTypes.legacyPrimeMeteorStrike(level, player);
        for (LivingEntity target : targets) {
            double dist = target.distanceTo(player);
            float dmg = (dist <= inner)
                    ? target.getMaxHealth() * (float) cfg.innerDamageMultiplier.getAsDouble()
                    : (float) Mth.lerp((dist - inner) / (outer - inner),
                            cfg.outerDamageMax.get(), cfg.outerDamageMin.get());
            target.hurt(src, dmg);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 3, 1.0, 0, 1.0, 0);
        GauntletParticleHelper.meteorLandingExtras(level, pos);
        GauntletEffectBroadcaster.meteorImpact(player, pos, (float) outer);
        GauntletSoundHelper.meteorLand(level, pos);
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.METEOR_LAND, 0, 14);
    }

    @Override
    public void performNormalAttack(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.NORMAL_ATTACK)) return;
        var cfg = OLRUConfig.LEGACY_PRIME.HAND_CANNON;
        ServerLevel level = player.level();

        var hitOpt = GauntletHelper.raycastForLiving(player, cfg.range.get(), true);
        Vec3 hitCenter = hitOpt.map(t -> t.position().add(0, t.getBbHeight() * 0.5, 0)).orElse(null);

        // Bullet trail fires regardless of whether a mob was hit
        GauntletEventHandlers.spawnBulletTrail(player, hitCenter, cfg.range.get(), cfg.particleCount.getAsInt());
        GauntletParticleHelper.muzzleFlash(level, player.getEyePosition().add(player.getLookAngle().scale(0.6)), 0xFFA028);
        GauntletSoundHelper.handCannon(level, player.position());

        if (hitOpt.isPresent()) {
            hitOpt.get().hurt(OLRUDamageTypes.legacyPrimeHandCannon(level, player), (float) cfg.damage.getAsDouble());
        }

        GauntletEffectBroadcaster.pose(player, GauntletPoseType.PRIME_HAND_CANNON_RECOIL, 0, 6);
        consumeSkill(player, SkillType.NORMAL_ATTACK);
    }

    @Override
    public int getMaxChargeTicks() {
        return OLRUConfig.LEGACY_PRIME.ROCKET_PUNCH.maxChargeTicks.getAsInt();
    }
}
