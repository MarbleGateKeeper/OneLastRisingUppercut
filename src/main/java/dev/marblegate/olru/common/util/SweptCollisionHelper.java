package dev.marblegate.olru.common.util;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SweptCollisionHelper {
    public record SweepResult(List<LivingEntity> entityHits, boolean blockHit, Vec3 snapOffset) {
        public boolean hasCollision() {
            return !entityHits.isEmpty() || blockHit;
        }
    }

    public static SweepResult sweep(Entity entity, Level level, Vec3 delta,
            Predicate<LivingEntity> entityFilter) {
        if (delta.lengthSqr() == 0) return new SweepResult(List.of(), false, Vec3.ZERO);
        AABB box = entity.getBoundingBox();
        int steps = stepsFor(box, delta);
        double inv = 1.0 / steps;
        Vec3 lastSafe = Vec3.ZERO;
        for (int i = 1; i <= steps; i++) {
            Vec3 offset = delta.scale(i * inv);
            AABB moved = box.move(offset);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, moved, entityFilter);
            if (!hits.isEmpty()) return new SweepResult(hits, false, lastSafe);
            if (!level.noCollision(entity, moved)) return new SweepResult(List.of(), true, lastSafe);
            lastSafe = offset;
        }
        return new SweepResult(List.of(), false, delta);
    }

    public static SweepResult sweepBlocks(Entity entity, Level level, Vec3 delta) {
        if (delta.lengthSqr() == 0) return new SweepResult(List.of(), false, Vec3.ZERO);
        AABB box = entity.getBoundingBox();
        int steps = stepsFor(box, delta);
        double inv = 1.0 / steps;
        Vec3 lastSafe = Vec3.ZERO;
        for (int i = 1; i <= steps; i++) {
            Vec3 offset = delta.scale(i * inv);
            if (!level.noCollision(entity, box.move(offset))) {
                return new SweepResult(List.of(), true, lastSafe);
            }
            lastSafe = offset;
        }
        return new SweepResult(List.of(), false, delta);
    }

    private static int stepsFor(AABB box, Vec3 delta) {
        double stepSize = Math.min(box.getXsize(), box.getZsize());
        return Math.max(1, (int) Math.ceil(delta.length() / stepSize));
    }
}
