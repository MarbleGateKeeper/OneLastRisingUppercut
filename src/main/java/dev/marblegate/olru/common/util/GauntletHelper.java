package dev.marblegate.olru.common.util;

import dev.marblegate.olru.common.registry.OLRUTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GauntletHelper {
    public static Optional<LivingEntity> raycastForMob(ServerPlayer player, double range) {
        return raycastForLiving(player, range, false);
    }

    public static Optional<LivingEntity> raycastForLiving(ServerPlayer player, double range, boolean includePlayers) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));

        AABB searchBox = new AABB(
                Math.min(eye.x, end.x) - 1, Math.min(eye.y, end.y) - 1, Math.min(eye.z, end.z) - 1,
                Math.max(eye.x, end.x) + 1, Math.max(eye.y, end.y) + 1, Math.max(eye.z, end.z) + 1);

        return player.level()
                .getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> isSelectableLiving(player, e, includePlayers))
                .stream()
                .filter(e -> {
                    AABB bb = e.getBoundingBox().inflate(0.3);
                    return bb.clip(eye, end).isPresent();
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
    }

    public static Optional<LivingEntity> raycastForSedativeTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));

        AABB searchBox = new AABB(
                Math.min(eye.x, end.x) - 1, Math.min(eye.y, end.y) - 1, Math.min(eye.z, end.z) - 1,
                Math.max(eye.x, end.x) + 1, Math.max(eye.y, end.y) + 1, Math.max(eye.z, end.z) + 1);

        List<RaycastCandidate> candidates = new ArrayList<>();
        for (LivingEntity entity : player.level()
                .getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> isSelectableLiving(player, e, true))) {
            addRaycastCandidate(candidates, entity, entity.getBoundingBox().inflate(0.3), eye, end);
        }

        for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof EnderDragonPart)) {
            EnderDragonPart part = (EnderDragonPart) entity;
            if (!part.parentMob.isAlive()) continue;
            addRaycastCandidate(candidates, part.parentMob, part.getBoundingBox().inflate(0.3), eye, end);
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(RaycastCandidate::distanceSqr))
                .map(RaycastCandidate::target);
    }

    public static List<LivingEntity> entitiesInFrontCone(
            ServerPlayer player, double range, double angleDegrees) {
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 lookH = new Vec3(look.x, 0, look.z).normalize();
        double cosHalfAngle = Math.cos(Math.toRadians(angleDegrees / 2.0));

        AABB box = player.getBoundingBox().inflate(range);
        return player.level()
                .getEntitiesOfClass(LivingEntity.class, box,
                        e -> isSelectableLiving(player, e, true))
                .stream()
                .filter(e -> {
                    double dist = e.distanceTo(player);
                    if (dist > range) return false;
                    Vec3 dir = new Vec3(e.getX() - pos.x, 0, e.getZ() - pos.z).normalize();
                    return dir.dot(lookH) >= cosHalfAngle;
                })
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> nearbyAllies(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        return player.level()
                .getEntitiesOfClass(LivingEntity.class, box,
                        e -> isSelectableAlly(player, e))
                .stream()
                .filter(e -> e.distanceTo(player) <= range)
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> alliesInFrontCone(
            ServerPlayer player, double range, double angleDegrees) {
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 lookH = new Vec3(look.x, 0, look.z);
        if (lookH.lengthSqr() < 1.0E-6) lookH = new Vec3(0, 0, 1);
        lookH = lookH.normalize();
        double cosHalfAngle = Math.cos(Math.toRadians(angleDegrees / 2.0));

        AABB box = player.getBoundingBox().inflate(range);
        Vec3 finalLookH = lookH;
        return player.level()
                .getEntitiesOfClass(LivingEntity.class, box,
                        e -> isSelectableAlly(player, e))
                .stream()
                .filter(e -> {
                    double dist = e.distanceTo(player);
                    if (dist > range) return false;
                    Vec3 dir = new Vec3(e.getX() - pos.x, 0, e.getZ() - pos.z);
                    if (dir.lengthSqr() < 1.0E-6) return true;
                    return dir.normalize().dot(finalLookH) >= cosHalfAngle;
                })
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .collect(Collectors.toList());
    }

    public static boolean isFriendly(ServerPlayer player, LivingEntity target) {
        if (target == player) return true;
        if (target instanceof Player) return true;
        if (target.is(OLRUTags.PLAYER_ALLIES)) return true;
        return false;
    }

    private static boolean isSelectableLiving(ServerPlayer player, LivingEntity target, boolean includePlayers) {
        if (target == player || !target.isAlive() || target.isSpectator()) return false;
        if (target instanceof Player) return includePlayers;
        return target.isPickable();
    }

    private static boolean isSelectableAlly(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.isSpectator()) return false;
        return isFriendly(player, target);
    }

    private static void addRaycastCandidate(
            List<RaycastCandidate> candidates, LivingEntity target, AABB box, Vec3 eye, Vec3 end) {
        box.clip(eye, end).ifPresent(hit -> candidates.add(new RaycastCandidate(target, hit.distanceToSqr(eye))));
    }

    private record RaycastCandidate(LivingEntity target, double distanceSqr) {}
}
