package dev.marblegate.olru.common.entity;

import dev.marblegate.olru.common.core.AxiomAbsorptionHelper;
import dev.marblegate.olru.common.core.AxiomBarrierState;
import dev.marblegate.olru.common.core.AxiomEffectTracker;
import dev.marblegate.olru.common.core.GauntletParticleHelper;
import dev.marblegate.olru.common.core.GauntletSoundHelper;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.common.util.GauntletHelper;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Axiom's Experimental Barrier: a floating wall that annihilates hostile projectiles passing
 * through it and soaks direct damage into the owner's durability resource. The wall can be walked
 * through by everyone (allies and enemies alike, matching Overwatch) — it only stops projectiles
 * and attacks, never movement.
 *
 * <p>The entity's bounding box is a 4x2.5x4 axis-aligned box (entity sizes are square-footprinted),
 * so it intentionally does NOT match the rotated wall plane; melee/attack hits use it as a generous
 * target, while projectile absorption uses the thin yaw-oriented slab computed in
 * {@link #absorbZoneBox()}.
 */
public class AxiomBarrierEntity extends Entity {
    /** Ticks without a channel refresh before the barrier recalls itself. */
    private static final int KEEPALIVE_TICKS = 5;
    private static final EntityDataAccessor<Float> DATA_DURABILITY = SynchedEntityData.defineId(AxiomBarrierEntity.class, EntityDataSerializers.FLOAT);

    private @Nullable UUID ownerUUID;
    private int keepaliveTicks = KEEPALIVE_TICKS;
    private float deployYaw;
    private double deployDistance;
    /** Server game time of the last durability damage; bookkeeping for later phases. */
    private long lastDamageTick = -1;

    public AxiomBarrierEntity(EntityType<AxiomBarrierEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public static AxiomBarrierEntity spawn(ServerLevel level, ServerPlayer owner, Vec3 pos, float yaw) {
        AxiomBarrierEntity barrier = new AxiomBarrierEntity(OLRUEntityTypes.AXIOM_BARRIER.get(), level);
        barrier.setPos(pos);
        barrier.setRot(yaw, 0);
        barrier.ownerUUID = owner.getUUID();
        barrier.deployYaw = yaw;
        barrier.deployDistance = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER.minDeployDistance.get();
        barrier.setDurabilityShadow(AxiomBarrierState.getDurabilityFraction(owner));
        level.addFreshEntity(barrier);
        return barrier;
    }

    /** Bottom-center position of the wall deployed {@code distance} blocks from the owner's eyes. */
    public static Vec3 deployPosition(Vec3 eyePos, float yaw, double distance) {
        double yawRad = Math.toRadians(yaw);
        double height = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER.height.get();
        return eyePos.add(-Math.sin(yawRad) * distance, -height * 0.3, Math.cos(yawRad) * distance);
    }

    /** Advances the wall along its fixed deploy yaw and refreshes the recall keepalive. */
    public void updateDeployment(Vec3 eyePos, double newDistance) {
        this.deployDistance = newDistance;
        Vec3 target = deployPosition(eyePos, deployYaw, newDistance);
        teleportTo(target.x, target.y, target.z);
        this.keepaliveTicks = KEEPALIVE_TICKS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DURABILITY, 1.0f);
    }

    /** Owner durability fraction, synced for the renderer's damage flash. */
    public float getDurabilityFraction() {
        return getEntityData().get(DATA_DURABILITY);
    }

    private void setDurabilityShadow(float fraction) {
        getEntityData().set(DATA_DURABILITY, fraction);
    }

    public @Nullable UUID getOwnerUUID() {
        return ownerUUID;
    }

    public double getDeployDistance() {
        return deployDistance;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return; // client visual state comes from sync
        if (--keepaliveTicks <= 0) {
            recall(level);
            return;
        }
        ServerPlayer owner = resolveOwner(level);
        if (owner == null) {
            recall(level);
            return;
        }
        AxiomAbsorptionHelper.absorbZone(level, absorbZoneBox(), owner);
    }

    /**
     * Thin slab around the rotated wall plane: the axis-aligned box of the wall's four corners
     * (center +- tangent * width/2 +- up * height/2), padded by half the configured thickness.
     * Exact for axis-aligned walls and a conservative superset for diagonal ones; very fast
     * projectiles can still tunnel through between ticks, which is accepted for now.
     */
    private AABB absorbZoneBox() {
        var cfg = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER;
        double halfWidth = cfg.width.get() * 0.5;
        double halfHeight = cfg.height.get() * 0.5;
        double pad = cfg.absorbZoneThickness.get() * 0.5 + 0.05;
        double yawRad = Math.toRadians(deployYaw);
        // Wall tangent: perpendicular to the horizontal deploy direction (-sin(yaw), 0, cos(yaw)).
        double tangentX = Math.cos(yawRad);
        double tangentZ = Math.sin(yawRad);
        double dx = Math.abs(tangentX) * halfWidth + pad;
        double dz = Math.abs(tangentZ) * halfWidth + pad;
        double cy = getY() + halfHeight;
        return new AABB(
                getX() - dx, cy - halfHeight - pad, getZ() - dz,
                getX() + dx, cy + halfHeight + pad, getZ() + dz);
    }

    private @Nullable ServerPlayer resolveOwner(ServerLevel level) {
        if (ownerUUID == null) return null;
        return level.getEntity(ownerUUID) instanceof ServerPlayer player ? player : null;
    }

    private Vec3 center() {
        var cfg = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER;
        return position().add(0, cfg.height.get() * 0.5, 0);
    }

    private void recall(ServerLevel level) {
        GauntletParticleHelper.barrierRecallBurst(level, center());
        ServerPlayer owner = resolveOwner(level);
        if (owner != null) {
            AxiomBarrierState.startRecallCooldown(owner,
                    OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER.recallCooldownTicks.get());
        }
        AxiomEffectTracker.clearActiveBarrier(this);
        discard();
    }

    private void shatter(ServerLevel level, ServerPlayer owner) {
        Vec3 center = center();
        GauntletParticleHelper.barrierShatterBurst(level, center);
        GauntletSoundHelper.barrierShatter(level, center);
        AxiomBarrierState.startBrokenCooldown(owner);
        AxiomEffectTracker.clearActiveBarrier(this);
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        ServerPlayer owner = resolveOwner(level);
        if (owner == null) return false;
        // Friendly fire (including, per GauntletHelper's current convention, every player) is ignored.
        if (source.getEntity() instanceof LivingEntity living && GauntletHelper.isFriendly(owner, living)) return false;
        lastDamageTick = level.getGameTime();
        AxiomEffectTracker.noteBarrierActivity(owner);
        float fraction = AxiomBarrierState.drainDurability(owner, damage);
        setDurabilityShadow(fraction);
        if (fraction <= 0f) {
            shatter(level, owner);
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true; // enemies can attack the wall; canBeCollidedWith stays false (walk-through)
    }

    // The barrier is transient: nothing is persisted, and a reloaded copy recalls itself within
    // KEEPALIVE_TICKS because no channel refresh arrives.
    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
