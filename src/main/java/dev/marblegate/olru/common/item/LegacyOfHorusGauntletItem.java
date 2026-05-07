package dev.marblegate.olru.common.item;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.ConditionalChargeState;
import dev.marblegate.olru.common.attachment.skill.CooldownSkillState;
import dev.marblegate.olru.common.attachment.skill.FullChargeState;
import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletEventHandlers;
import dev.marblegate.olru.common.core.HorusEffectTracker;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.core.movement.MovementTaskAssignmentResult;
import dev.marblegate.olru.common.core.movement.MovementTaskProperties;
import dev.marblegate.olru.common.core.movement.task.EntityPushTask;
import dev.marblegate.olru.common.item.tooltip.GauntletTooltipHelper;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LegacyOfHorusGauntletItem extends AbstractGauntletItem {
    public LegacyOfHorusGauntletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Identifier gauntletId() {
        return Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "legacy_of_horus");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        GauntletTooltipHelper.appendLegacyOfHorus(tooltip, flag.hasShiftDown());
    }

    @Override
    public GauntletSkillGroup createDefaultSkillGroup() {
        return new GauntletSkillGroup(
                new CooldownSkillState(OLRUConfig.HORUS.FIELD_EXTRACTION.cooldownTicks),
                new CooldownSkillState(OLRUConfig.HORUS.SEDATIVE_DART.cooldownTicks),
                new ConditionalChargeState(),
                new FullChargeState(
                        OLRUConfig.HORUS.BIOTIC_ROUND.cooldownTicks,
                        OLRUConfig.HORUS.BIOTIC_ROUND.maxCharges));
    }

    @Override
    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (!isSkillReady(player, SkillType.SKILL_ONE)) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            consumeSkill(serverPlayer, SkillType.SKILL_ONE);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        var cfg = OLRUConfig.HORUS.FIELD_EXTRACTION;
        int ticksHeld = getUseDuration(stack, entity) - remainingUseDuration;
        float chargePercent = Math.min(1f, (float) ticksHeld / getMaxChargeTicks());
        for (LivingEntity target : GauntletHelper.alliesInFrontCone(
                player, cfg.allyLockRange.get(), cfg.allyConeAngleDegrees.get())) {
            Vec3 toPlayer = player.position().add(0, player.getBbHeight() * 0.35, 0)
                    .subtract(target.position());
            if (toPlayer.lengthSqr() < 1.0) continue;
            double speed = cfg.pullSpeed.get() * Math.max(0.25, chargePercent);
            target.setDeltaMovement(toPlayer.normalize().scale(speed));
            target.hurtMarked = true;
            applyProtection(target, 10, 0);
            GauntletEffectBroadcaster.fieldExtractionBeam(player, target, 4);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return false;
        int ticksHeld = getUseDuration(stack, entity) - timeCharged;
        float chargePercent = Math.min(1f, (float) ticksHeld / getMaxChargeTicks());
        performSkillOne(player, chargePercent);
        return true;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {}

    @Override
    public void performSkillOne(ServerPlayer player, float chargePercent) {
        var cfg = OLRUConfig.HORUS.FIELD_EXTRACTION;

        List<LivingEntity> allies = GauntletHelper.alliesInFrontCone(
                player, cfg.allyLockRange.get(), cfg.allyConeAngleDegrees.get());
        if (!allies.isEmpty()) {
            for (LivingEntity target : allies) {
                if (target.distanceTo(player) > cfg.allyLockRange.get()) continue;
                healAllyAndChargeNano(player, target, cfg.healAmount.get() * Math.max(0.35, chargePercent));
                applyProtection(target, cfg.protectionTicks.get(), 1);
                spawnBioticBurst(player.level(), target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.HAPPY_VILLAGER);
            }
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(0, 0, 1);
        MovementTaskAssignmentResult result = MovementManager.assign(player, new EntityPushTask(
                horizontal.normalize().scale(cfg.selfDashSpeed.get()),
                cfg.selfDashDistance.get() * Math.max(0.35, chargePercent),
                true), MovementTaskProperties.playerActive(player.getUUID()));
        if (!result.accepted()) return;
    }

    @Override
    public void performSkillTwo(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.SKILL_TWO)) return;
        var cfg = OLRUConfig.HORUS.SEDATIVE_DART;
        ServerLevel level = player.level();

        var targetOpt = GauntletHelper.raycastForSedativeTarget(player, cfg.range.get());
        Vec3 hitCenter = targetOpt.map(t -> t.position().add(0, t.getBbHeight() * 0.5, 0)).orElse(null);
        GauntletEventHandlers.spawnBulletTrail(player, hitCenter, cfg.range.get(), cfg.particleCount.getAsInt());

        if (targetOpt.isPresent()) {
            LivingEntity target = targetOpt.get();
            if (!GauntletHelper.isFriendly(player, target)) {
                target.hurt(OLRUDamageTypes.legacyOfHorusSedativeDart(level, player), (float) cfg.damage.getAsDouble());
                applySedative(target, cfg.sleepTicks.get(), cfg.bossSleepTicks.get(), cfg.flyingDropSpeed.get());
            }
        }

        consumeSkill(player, SkillType.SKILL_TWO);
    }

    @Override
    public void performUltimate(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.ULTIMATE)) return;
        var cfg = OLRUConfig.HORUS.NANO_SURGE;
        ServerLevel level = player.level();

        List<LivingEntity> targets = GauntletHelper.nearbyAllies(player, cfg.range.get());
        targets.add(player);
        for (LivingEntity target : targets) {
            target.heal((float) cfg.healAmount.getAsDouble());
            if (target.getHealth() <= target.getMaxHealth() * cfg.emergencyHealthFraction.get()) {
                target.heal((float) cfg.emergencyHealAmount.getAsDouble());
                target.addEffect(new MobEffectInstance(
                        MobEffects.ABSORPTION,
                        cfg.buffTicks.get(),
                        cfg.absorptionAmplifier.get()));
            }
            target.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    cfg.buffTicks.get(),
                    cfg.regenerationAmplifier.get()));
            HorusEffectTracker.addHealingBoost(target, cfg.buffTicks.get(), cfg.healingMultiplier.get());
            HorusEffectTracker.addNanoSurgeVisual(target, cfg.buffTicks.get());
            GauntletEffectBroadcaster.nanoSurge(target, cfg.buffTicks.get());
            target.addEffect(new MobEffectInstance(
                    MobEffects.STRENGTH,
                    cfg.buffTicks.get(),
                    cfg.damageAmplifier.get()));
            target.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    cfg.buffTicks.get(),
                    cfg.resistanceAmplifier.get()));
            spawnBioticBurst(level, target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.END_ROD);
        }

        consumeSkill(player, SkillType.ULTIMATE);
    }

    @Override
    public void performNormalAttack(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.NORMAL_ATTACK)) return;
        var cfg = OLRUConfig.HORUS.BIOTIC_ROUND;
        ServerLevel level = player.level();

        var targetOpt = GauntletHelper.raycastForLiving(player, cfg.effectiveRange.get(), true);
        Vec3 hitCenter = targetOpt.map(t -> t.position().add(0, t.getBbHeight() * 0.5, 0)).orElse(null);
        GauntletEventHandlers.spawnBulletTrail(player, hitCenter, cfg.effectiveRange.get(), cfg.particleCount.getAsInt());

        if (targetOpt.isPresent()) {
            LivingEntity target = targetOpt.get();
            if (GauntletHelper.isFriendly(player, target)) {
                healAllyAndChargeNano(player, target, cfg.healAmount.getAsDouble());
                spawnBioticBurst(level, hitCenter, ParticleTypes.HAPPY_VILLAGER);
            } else {
                target.hurt(OLRUDamageTypes.legacyOfHorusBioticRound(level, player), (float) cfg.damage.getAsDouble());
                spawnBioticBurst(level, hitCenter, ParticleTypes.SNEEZE);
            }
        }

        consumeSkill(player, SkillType.NORMAL_ATTACK);
    }

    @Override
    public int getMaxChargeTicks() {
        return OLRUConfig.HORUS.FIELD_EXTRACTION.maxChargeTicks.getAsInt();
    }

    private static void applyProtection(LivingEntity target, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, amplifier));
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 0));
    }

    private static void healAllyAndChargeNano(ServerPlayer player, LivingEntity target, double amount) {
        if (amount <= 0) return;
        float before = target.getHealth();
        target.heal((float) amount);
        float healed = Math.max(0f, target.getHealth() - before);
        if (target == player || healed <= 0f || !GauntletHelper.isFriendly(player, target)) return;
        GauntletEventHandlers.addHorusNanoCharge(
                player,
                healed * (float) (OLRUConfig.HORUS.NANO_SURGE.chargePercentPerHealing.get() / 100.0));
    }

    private static void applySedative(LivingEntity target, int normalTicks, int bossTicks, double flyingDropSpeed) {
        boolean bossLike = target.getMaxHealth() >= 100.0f;
        int ticks = bossLike ? bossTicks : normalTicks;
        if (ticks <= 0) return;

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, bossLike ? 1 : 4));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, bossLike ? 0 : 2));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, bossLike ? 0 : 2));
        HorusEffectTracker.sedate(target, ticks, flyingDropSpeed, !bossLike);
    }

    private static void spawnBioticBurst(ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions particle) {
        if (pos == null) return;
        level.sendParticles(particle, pos.x, pos.y, pos.z, 10, 0.25, 0.25, 0.25, 0.04);
    }
}
