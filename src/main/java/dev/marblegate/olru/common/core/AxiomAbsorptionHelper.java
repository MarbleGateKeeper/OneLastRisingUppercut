package dev.marblegate.olru.common.core;

import dev.marblegate.olru.common.entity.AccretionBoulderEntity;
import dev.marblegate.olru.common.entity.BioticGrenade;
import dev.marblegate.olru.common.entity.BioticOrbEntity;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.KineticGraspConfig;
import dev.marblegate.olru.config.OLRUConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared projectile-annihilation logic for The Axiom. Used by the Experimental Barrier wall and
 * Kinetic Grasp, which consumes the returned shield credit.
 */
public final class AxiomAbsorptionHelper {
    private AxiomAbsorptionHelper() {}

    /**
     * A projectile is absorbable when it is alive and hostile to the defender: never the defender's
     * own projectile, and never one fired by a friendly living entity.
     */
    public static boolean isAbsorbableProjectile(Entity e, ServerPlayer defender) {
        if (!(e instanceof Projectile projectile) || !e.isAlive()) return false;
        Entity owner = projectile.getOwner();
        if (owner == defender) return false;
        if (owner instanceof LivingEntity living && GauntletHelper.isFriendly(defender, living)) return false;
        return true;
    }

    /** Heavy projectiles grant more Kinetic Grasp credit. */
    public static boolean isHeavyProjectile(Entity e) {
        return e instanceof BioticGrenade || e instanceof BioticOrbEntity || e instanceof AccretionBoulderEntity;
    }

    public static int absorbCreditFor(Entity e, KineticGraspConfig cfg) {
        return (int) (isHeavyProjectile(e) ? cfg.creditPerHeavyProjectile.getAsDouble() : cfg.creditPerProjectile.getAsDouble());
    }

    /**
     * Destroys every absorbable projectile intersecting {@code zone} with a purple freeze-burst and
     * returns the total Kinetic Grasp credit (ignored by the Experimental Barrier).
     */
    public static int absorbZone(ServerLevel level, AABB zone, ServerPlayer defender) {
        int credit = 0;
        var cfg = OLRUConfig.THE_AXIOM.KINETIC_GRASP;
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, zone,
                e -> isAbsorbableProjectile(e, defender))) {
            Vec3 pos = projectile.position().add(0, projectile.getBbHeight() * 0.5, 0);
            GauntletParticleHelper.barrierAbsorbBurst(level, pos);
            GauntletSoundHelper.barrierAbsorb(level, pos);
            projectile.discard();
            credit += absorbCreditFor(projectile, cfg);
        }
        return credit;
    }
}
