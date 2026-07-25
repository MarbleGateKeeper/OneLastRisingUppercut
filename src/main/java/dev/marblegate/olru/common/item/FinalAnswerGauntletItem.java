package dev.marblegate.olru.common.item;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.common.attachment.GauntletEntityState;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.ConditionalChargeState;
import dev.marblegate.olru.common.attachment.skill.CooldownSkillState;
import dev.marblegate.olru.common.core.FinalAnswerEffectTracker;
import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.entity.BioticOrbEntity;
import dev.marblegate.olru.common.item.tooltip.GauntletTooltipHelper;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.common.registry.OLRUDamageTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.Comparator;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FinalAnswerGauntletItem extends AbstractGauntletItem {
    public FinalAnswerGauntletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Identifier gauntletId() {
        return Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "final_answer");
    }

    @Override
    public boolean isNormalAttackContinuous() {
        // Biotic Spray (LMB) is a hold-to-channel ability.
        return true;
    }

    @Override
    public boolean isChargeProgressMeaningful() {
        // Biotic Grasp is a no-cooldown channel; its charge percent carries no meaning.
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        GauntletTooltipHelper.appendFinalAnswer(tooltip, flag.hasShiftDown());
    }

    @Override
    public GauntletSkillGroup createDefaultSkillGroup() {
        // Overwatch layout: LMB = Biotic Spray (holds the biotic energy bar), RMB = Biotic Grasp
        // (channeled, no cooldown).
        return new GauntletSkillGroup(
                ConditionalChargeState.resource(),
                new CooldownSkillState(OLRUConfig.FINAL_ANSWER.BIOTIC_GRASP.swingCooldownTicks),
                new CooldownSkillState(OLRUConfig.FINAL_ANSWER.FADE.cooldownTicks),
                new CooldownSkillState(OLRUConfig.FINAL_ANSWER.BIOTIC_ORB.cooldownTicks),
                new ConditionalChargeState());
    }

    @Override
    protected boolean isSkillReady(Player player, SkillType type) {
        // While Coalescence is channeled only Fade remains usable.
        return super.isSkillReady(player, type)
                && (type == SkillType.SKILL_TWO || !FinalAnswerEffectTracker.isCoalescing(player));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        // Biotic Grasp is a free-movement channel: the base onUseTick pins movement, so it is
        // intentionally not called here.
        var cfg = OLRUConfig.FINAL_ANSWER.BIOTIC_GRASP;
        int interval = Math.max(1, cfg.pulseIntervalTicks.get());
        int ticksHeld = getUseDuration(stack, entity) - remainingUseDuration;
        if (ticksHeld % interval != 0) return;

        ServerLevel serverLevel = player.level();
        LivingEntity target = GauntletHelper.entitiesInFrontCone(player, cfg.range.get(), cfg.coneAngleDegrees.get())
                .stream()
                .filter(e -> !GauntletHelper.isFriendly(player, e))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target != null) {
            Vec3 hitCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
            target.hurt(OLRUDamageTypes.finalAnswerBioticGrasp(serverLevel, player), (float) cfg.damage.getAsDouble());
            player.heal((float) cfg.selfHeal.getAsDouble());
            addEnergy(player, (float) cfg.energyPerHit.getAsDouble());
            GauntletParticleHelper.coloredTrail(player, hitCenter, cfg.range.get(), 0xB04AD8);
            serverLevel.sendParticles(ParticleTypes.WITCH, hitCenter.x, hitCenter.y, hitCenter.z, 3, 0.2, 0.25, 0.2, 0.02);
            if (ticksHeld % (interval * 2) == 0) {
                GauntletSoundHelper.graspHit(serverLevel, hitCenter);
            }
        }
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.GRASP_FIRE, 0, 4);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {}

    @Override
    public void performSkillOne(ServerPlayer player, float chargePercent) {
        // Biotic Grasp is a pure channel; releasing only stops the use state.
    }

    @Override
    public void performSkillTwo(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.SKILL_TWO)) return;
        var cfg = OLRUConfig.FINAL_ANSWER.FADE;
        ServerLevel level = player.level();

        FinalAnswerEffectTracker.startFade(player, cfg.durationTicks.get());
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                24, 0.3, 0.5, 0.3, 0.4);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 0.2, player.getZ(),
                10, 0.3, 0.2, 0.3, 0.02);
        GauntletSoundHelper.fade(level, player.position());
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.FADE, 0, 6);
        consumeSkill(player, SkillType.SKILL_TWO);
    }

    @Override
    public void performSkillThree(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.SKILL_THREE)) return;
        ServerLevel level = player.level();
        BioticOrbEntity.spawn(level, player, player.getEyePosition(), player.getLookAngle());
        GauntletSoundHelper.orbLaunch(level, player.position());
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.ORB_THROW, 0, 8);
        consumeSkill(player, SkillType.SKILL_THREE);
    }

    @Override
    public void performUltimate(ServerPlayer player) {
        if (!isSkillReady(player, SkillType.ULTIMATE)) return;
        FinalAnswerEffectTracker.startCoalescence(player, OLRUConfig.FINAL_ANSWER.COALESCENCE.durationTicks.get());
        GauntletSoundHelper.coalescenceStart(player.level(), player.position());
        consumeSkill(player, SkillType.ULTIMATE);
    }

    @Override
    public void performNormalAttack(ServerPlayer player) {
        // Biotic Spray pulse, driven by the client while LMB is held.
        if (!isSkillReady(player, SkillType.NORMAL_ATTACK)) return;
        var cfg = OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY;
        int interval = Math.max(1, cfg.pulseIntervalTicks.get());

        // The energy drain IS the cost: consumeSkill is NOT called here because
        // ConditionalChargeState.consume() would zero the whole bar. If the drain empties the
        // bar this pulse still applies; later pulses gate out on isSkillReady above.
        float energy = addEnergy(player, (float) -(cfg.energyDrainPerTick.getAsDouble() * interval));

        float healed = 0f;
        for (LivingEntity target : GauntletHelper.alliesInFrontCone(player, cfg.range.get(), cfg.coneAngleDegrees.get())) {
            if (target == player) continue;
            float before = target.getHealth();
            target.heal((float) (cfg.healPerTick.getAsDouble() * interval));
            healed += Math.max(0f, target.getHealth() - before);
            target.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION, cfg.lingerTicks.get(), cfg.lingerAmplifier.get(), false, false, false));
        }
        if (healed > 0f) {
            addCoalescenceCharge(player, healed * (float) (cfg.ultChargePercentPerHeal.get() / 100.0));
        }

        spawnSprayCone(player, cfg.range.get());
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.SPRAY_CHANNEL, energy, 4);
        if (player.tickCount % (interval * 4) == 0) {
            GauntletSoundHelper.sprayChannel(player.level(), player.position());
        }
    }

    @Override
    public int getMaxChargeTicks() {
        // Grasp has no charge tiers; 20 ticks is only the reference for a "full channel" display percent.
        return 20;
    }

    /** Adds to the biotic energy bar (NORMAL_ATTACK resource state) and syncs; returns the new progress. */
    private float addEnergy(ServerPlayer player, float delta) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GauntletSkillGroup group = state.getOrCreate(gauntletId(), this::createDefaultSkillGroup).group();
        if (!(group.get(SkillType.NORMAL_ATTACK) instanceof ConditionalChargeState energy)) return 0f;
        energy.addProgress(delta);
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
        return energy.getProgress();
    }

    private void addCoalescenceCharge(ServerPlayer player, float progress) {
        if (progress <= 0f) return;
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        GauntletSkillGroup group = state.getOrCreate(gauntletId(), this::createDefaultSkillGroup).group();
        if (!(group.get(SkillType.ULTIMATE) instanceof ConditionalChargeState ccs)) return;
        if (ccs.isUsable()) return;
        ccs.addProgress(progress);
        player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
    }

    private static void spawnSprayCone(ServerPlayer player, double range) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(look.scale(0.4));
        DustParticleOptions dust = new DustParticleOptions(0xFFD75A, 0.8f);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 10; i++) {
            double dist = 0.6 + random.nextDouble() * range * 0.6;
            Vec3 p = origin.add(look.scale(dist)).add(
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.4,
                    (random.nextDouble() - 0.5) * 0.5);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        if (random.nextInt(3) == 0) {
            Vec3 p = origin.add(look.scale(1.0 + random.nextDouble() * range * 0.4));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.0);
        }
    }
}
