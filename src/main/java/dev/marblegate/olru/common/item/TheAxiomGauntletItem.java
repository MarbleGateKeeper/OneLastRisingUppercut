package dev.marblegate.olru.common.item;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.ConditionalChargeState;
import dev.marblegate.olru.common.attachment.skill.CooldownSkillState;
import dev.marblegate.olru.common.attachment.skill.IncrementalChargeState;
import dev.marblegate.olru.common.attachment.skill.ResourceCooldownState;
import dev.marblegate.olru.common.core.AxiomBarrierState;
import dev.marblegate.olru.common.core.AxiomEffectTracker;
import dev.marblegate.olru.common.core.GauntletEffectBroadcaster;
import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.entity.AccretionBoulderEntity;
import dev.marblegate.olru.common.entity.AxiomBarrierEntity;
import dev.marblegate.olru.common.entity.HypersphereEntity;
import dev.marblegate.olru.common.item.tooltip.GauntletTooltipHelper;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TheAxiomGauntletItem extends AbstractGauntletItem {
    public TheAxiomGauntletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Identifier gauntletId() {
        return Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "the_axiom");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        GauntletTooltipHelper.appendTheAxiom(tooltip, flag.hasShiftDown());
    }

    @Override
    public GauntletSkillGroup createDefaultSkillGroup() {
        // Overwatch layout: LMB = Hyperspheres (pair ammo), RMB = Experimental Barrier
        // (durability resource with the recall/broken cooldown; starts full, no cooldown).
        return new GauntletSkillGroup(
                new IncrementalChargeState(
                        OLRUConfig.THE_AXIOM.HYPERSPHERES.cooldownTicks,
                        OLRUConfig.THE_AXIOM.HYPERSPHERES.maxCharges),
                new ResourceCooldownState(OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER.brokenCooldownTicks, 1f),
                new CooldownSkillState(OLRUConfig.THE_AXIOM.KINETIC_GRASP.cooldownTicks),
                new CooldownSkillState(OLRUConfig.THE_AXIOM.ACCRETION.cooldownTicks),
                new ConditionalChargeState());
    }

    @Override
    public void performNormalAttack(ServerPlayer player) {
        // While Gravitic Flux is aiming, LMB confirms the zone instead of firing Hyperspheres.
        if (AxiomEffectTracker.isAiming(player)) {
            AxiomEffectTracker.confirmAim(player);
            return;
        }
        if (!isSkillReady(player, SkillType.NORMAL_ATTACK)) return;
        var cfg = OLRUConfig.THE_AXIOM.HYPERSPHERES;
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();

        // The second sphere hovers at the muzzle for pairIntervalTicks before launching.
        HypersphereEntity.spawn(level, player, start, look, 0);
        HypersphereEntity.spawn(level, player, start, look, cfg.pairIntervalTicks.get());
        GauntletParticleHelper.muzzleFlash(level, start.add(look.scale(0.6)), HypersphereEntity.PURPLE);
        GauntletSoundHelper.hyperspheresFire(level, player.position());
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.HYPERSPHERES_FIRE, 0, 5);
        consumeSkill(player, SkillType.NORMAL_ATTACK);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        // Experimental Barrier is a free-movement channel: the base onUseTick pins movement, so it
        // is intentionally not called here.
        if (AxiomBarrierState.getDurabilityFraction(player) <= 0f) {
            player.stopUsingItem();
            return;
        }

        ServerLevel serverLevel = player.level();
        var cfg = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER;
        AxiomBarrierEntity barrier = AxiomEffectTracker.getActiveBarrier(player);
        Vec3 eyePos = player.getEyePosition();
        if (barrier == null) {
            // First channel tick: deploy at min distance, yaw frozen for the rest of the channel.
            float yaw = player.getYRot();
            barrier = AxiomBarrierEntity.spawn(serverLevel, player,
                    AxiomBarrierEntity.deployPosition(eyePos, yaw, cfg.minDeployDistance.get()), yaw);
            AxiomEffectTracker.setActiveBarrier(player, barrier);
            GauntletSoundHelper.barrierDeploy(serverLevel, barrier.position());
        } else {
            double distance = Math.min(barrier.getDeployDistance() + cfg.deploySpeedPerTick.get(),
                    cfg.maxDeployDistance.get());
            barrier.updateDeployment(eyePos, distance);
        }
        AxiomEffectTracker.noteBarrierActivity(player);
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.BARRIER_CHANNEL, 0, 4);
    }

    @Override
    public void performSkillOne(ServerPlayer player, float chargePercent) {
        // Experimental Barrier is a pure channel (see onUseTick); releasing only stops the use
        // state and the deployed wall recalls itself when its keepalive expires.
        if (!isSkillReady(player, SkillType.SKILL_ONE)) return;
    }

    @Override
    public void performSkillTwo(ServerPlayer player) {
        // Kinetic Grasp: a channeled absorb cone; the tracker accumulates credit and converts it
        // into an Absorption shield when the channel ends.
        if (!isSkillReady(player, SkillType.SKILL_TWO)) return;
        var cfg = OLRUConfig.THE_AXIOM.KINETIC_GRASP;
        AxiomEffectTracker.startKineticGrasp(player, cfg.durationTicks.get());
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.KINETIC_GRASP, 0, cfg.durationTicks.get());
        GauntletSoundHelper.kineticGrasp(player.level(), player.position());
        consumeSkill(player, SkillType.SKILL_TWO);
    }

    @Override
    public void performSkillThree(ServerPlayer player) {
        // Accretion: lob a debris boulder that knocks down its direct-hit target and splashes.
        if (!isSkillReady(player, SkillType.SKILL_THREE)) return;
        ServerLevel level = player.level();
        AccretionBoulderEntity.spawn(level, player, player.getEyePosition(), player.getLookAngle());
        GauntletSoundHelper.accretionThrow(level, player.position());
        GauntletEffectBroadcaster.pose(player, GauntletPoseType.ACCRETION_THROW, 0, 8);
        consumeSkill(player, SkillType.SKILL_THREE);
    }

    @Override
    public void performUltimate(ServerPlayer player) {
        // Gravitic Flux: the caster rises, aims a zone, lifts the enemies inside, then slams them down.
        if (!isSkillReady(player, SkillType.ULTIMATE)) return;
        AxiomEffectTracker.startFlux(player);
        consumeSkill(player, SkillType.ULTIMATE);
    }

    @Override
    public boolean isChargeProgressMeaningful() {
        // The barrier channel has no charge tiers; the durability bar is the meaningful display.
        return false;
    }

    @Override
    public int getMaxChargeTicks() {
        return 20;
    }
}
