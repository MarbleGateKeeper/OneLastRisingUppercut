package dev.marblegate.olru.common.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class HorusEffectTracker {
    private static final Map<UUID, Sedation> SEDATED_ENTITIES = new HashMap<>();
    private static final Map<UUID, TimedMultiplier> HEALING_BOOSTS = new HashMap<>();
    private static final Map<UUID, TimedVisual> NANO_SURGE_VISUALS = new HashMap<>();

    public static void sedate(LivingEntity target, int ticks, double flyingDropSpeed) {
        sedate(target, ticks, flyingDropSpeed, true);
    }

    public static void sedate(LivingEntity target, int ticks, double flyingDropSpeed, boolean suppressAi) {
        if (ticks <= 0) return;
        Sedation previous = SEDATED_ENTITIES.get(target.getUUID());
        boolean controlsAi = target instanceof Mob && (suppressAi || previous != null && previous.controlsAi);
        boolean wasNoAi = previous != null ? previous.wasNoAi : controlsAi && ((Mob) target).isNoAi();
        SEDATED_ENTITIES.put(target.getUUID(), new Sedation(ticks, flyingDropSpeed, wasNoAi, controlsAi));

        if (controlsAi) {
            ((Mob) target).setNoAi(true);
        }

        GauntletEffectBroadcaster.sedated(target, ticks, true);
        applySedationFall(target, flyingDropSpeed, controlsAi);
    }

    public static void addHealingBoost(LivingEntity target, int ticks, double multiplier) {
        if (ticks <= 0 || multiplier <= 1.0) return;
        HEALING_BOOSTS.put(target.getUUID(), new TimedMultiplier(ticks, multiplier));
    }

    public static void addNanoSurgeVisual(LivingEntity target, int ticks) {
        if (ticks <= 0) return;
        NANO_SURGE_VISUALS.put(target.getUUID(), new TimedVisual(ticks));
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        tickHealingBoost(entity);
        tickNanoSurgeVisual(entity);
        if (entity instanceof LivingEntity living) {
            tickSedation(living);
        }
    }

    public static void onLivingHeal(net.neoforged.neoforge.event.entity.living.LivingHealEvent event) {
        TimedMultiplier boost = HEALING_BOOSTS.get(event.getEntity().getUUID());
        if (boost == null) return;
        event.setAmount((float) (event.getAmount() * boost.multiplier));
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        cleanup(entity);
    }

    public static void onEntityDeath(LivingDeathEvent event) {
        cleanup(event.getEntity());
    }

    private static void tickSedation(LivingEntity target) {
        Sedation sedation = SEDATED_ENTITIES.get(target.getUUID());
        if (sedation == null) return;

        if (!target.isAlive()) {
            endSedation(target, sedation);
            return;
        }

        if (sedation.controlsAi && target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
        }

        applySedationFall(target, sedation.flyingDropSpeed, sedation.controlsAi);
        if (--sedation.ticksRemaining <= 0) {
            endSedation(target, sedation);
        }
    }

    private static void endSedation(LivingEntity target, Sedation sedation) {
        SEDATED_ENTITIES.remove(target.getUUID());
        if (sedation.controlsAi && target instanceof Mob mob) {
            mob.setNoAi(sedation.wasNoAi);
        }
        GauntletEffectBroadcaster.sedated(target, 0, false);
    }

    private static void cleanup(Entity entity) {
        Sedation sedation = SEDATED_ENTITIES.remove(entity.getUUID());
        if (sedation != null && entity instanceof LivingEntity living && entity.level() instanceof ServerLevel) {
            if (sedation.controlsAi && living instanceof Mob mob) {
                mob.setNoAi(sedation.wasNoAi);
            }
            GauntletEffectBroadcaster.sedated(living, 0, false);
        }
        HEALING_BOOSTS.remove(entity.getUUID());
        if (NANO_SURGE_VISUALS.remove(entity.getUUID()) != null) {
            GauntletEffectBroadcaster.stopNanoSurge(entity);
        }
    }

    private static void tickHealingBoost(Entity entity) {
        TimedMultiplier boost = HEALING_BOOSTS.get(entity.getUUID());
        if (boost == null) return;
        if (--boost.ticksRemaining <= 0 || !entity.isAlive()) {
            HEALING_BOOSTS.remove(entity.getUUID());
        }
    }

    private static void tickNanoSurgeVisual(Entity entity) {
        TimedVisual visual = NANO_SURGE_VISUALS.get(entity.getUUID());
        if (visual == null) return;
        if (--visual.ticksRemaining <= 0 || !entity.isAlive()) {
            NANO_SURGE_VISUALS.remove(entity.getUUID());
            GauntletEffectBroadcaster.stopNanoSurge(entity);
        }
    }

    private static void applySedationFall(LivingEntity target, double flyingDropSpeed, boolean forcedMovement) {
        if (flyingDropSpeed <= 0 || target.onGround()) return;
        if (target instanceof EnderDragon dragon) {
            applyDragonSedationFall(dragon, flyingDropSpeed);
            return;
        }

        double dropSpeed = Math.abs(flyingDropSpeed);
        Vec3 current = target.getDeltaMovement();
        Vec3 adjusted = new Vec3(current.x * 0.35, Math.min(current.y, -dropSpeed), current.z * 0.35);
        target.setNoGravity(false);
        target.setDeltaMovement(adjusted);
        target.hurtMarked = true;

        if (forcedMovement) {
            Vec3 fallStep = new Vec3(0.0, adjusted.y, 0.0);
            target.move(MoverType.SELF, fallStep);
            target.hurtMarked = true;
        }
    }

    private static void applyDragonSedationFall(EnderDragon dragon, double flyingDropSpeed) {
        double dropSpeed = Math.abs(flyingDropSpeed);
        double allowedFall = Math.min(dropSpeed, dragon.getAvailableSpaceBelow(dropSpeed));
        Vec3 current = dragon.getDeltaMovement();
        dragon.setNoGravity(false);

        if (allowedFall <= 1.0E-4) {
            dragon.setDeltaMovement(current.x * 0.35, 0.0, current.z * 0.35);
            dragon.hurtMarked = true;
            return;
        }

        Vec3 fallStep = new Vec3(0.0, -allowedFall, 0.0);
        dragon.move(MoverType.SELF, fallStep);
        moveDragonParts(dragon, fallStep);
        dragon.setDeltaMovement(current.x * 0.35, 0.0, current.z * 0.35);
        dragon.hurtMarked = true;
    }

    private static void moveDragonParts(EnderDragon dragon, Vec3 movement) {
        for (EnderDragonPart part : dragon.getSubEntities()) {
            part.setOldPosAndRot();
            part.setPos(part.getX() + movement.x, part.getY() + movement.y, part.getZ() + movement.z);
        }
    }

    private static class Sedation {
        int ticksRemaining;
        final double flyingDropSpeed;
        final boolean wasNoAi;
        final boolean controlsAi;

        Sedation(int ticksRemaining, double flyingDropSpeed, boolean wasNoAi, boolean controlsAi) {
            this.ticksRemaining = ticksRemaining;
            this.flyingDropSpeed = flyingDropSpeed;
            this.wasNoAi = wasNoAi;
            this.controlsAi = controlsAi;
        }
    }

    private static class TimedMultiplier {
        int ticksRemaining;
        final double multiplier;

        TimedMultiplier(int ticksRemaining, double multiplier) {
            this.ticksRemaining = ticksRemaining;
            this.multiplier = multiplier;
        }
    }

    private static class TimedVisual {
        int ticksRemaining;

        TimedVisual(int ticksRemaining) {
            this.ticksRemaining = ticksRemaining;
        }
    }
}
