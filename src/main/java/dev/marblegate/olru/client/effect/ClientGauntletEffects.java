package dev.marblegate.olru.client.effect;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.marblegate.olru.client.animation.ClientGauntletAnimations;
import dev.marblegate.olru.client.render.effect.NanoSurgeRenderData;
import dev.marblegate.olru.client.render.effect.RocketPunchChargeRenderData;
import dev.marblegate.olru.common.animation.GauntletPoseType;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload;
import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload.EffectType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fc;

public class ClientGauntletEffects {
    private static final Map<Integer, TimedFloat> ROCKET_CHARGES = new HashMap<>();
    private static final Map<Integer, MeteorTarget> METEOR_TARGETS = new HashMap<>();
    private static final Map<Long, TimedPair> EXTRACTION_BEAMS = new HashMap<>();
    private static final Map<Long, TimedOrbTether> ORB_TETHERS = new HashMap<>();
    private static final Map<Integer, Timed> SEDATED = new HashMap<>();
    private static final Map<Integer, Timed> NANO_SURGE = new HashMap<>();
    private static final Map<Integer, Timed> COALESCENCE_BEAMS = new HashMap<>();
    private static final Map<Integer, Timed> FADING = new HashMap<>();
    private static final List<OneShot> ONE_SHOTS = new ArrayList<>();

    public static final RenderType GAUNTLET_GLOW = RenderType.create(
            "olru_gauntlet_glow",
            RenderSetup.builder(RenderPipelines.LIGHTNING)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup());

    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int METEOR_BLOCK_SCAN_VERTICAL_BELOW = 2;
    private static final int METEOR_BLOCK_SCAN_VERTICAL_ABOVE = 4;
    private static final int METEOR_SURFACE_REBUILD_INTERVAL_TICKS = 5;
    private static final double IMPACT_SHAKE_RANGE = 12.0;

    private static int clientTicks = 0;

    public static void handle(ClientboundGauntletEffectPayload payload) {
        switch (payload.effectType()) {
            case ROCKET_CHARGE -> putOrRemove(
                    ROCKET_CHARGES, payload.sourceEntityId(),
                    new TimedFloat(payload.durationTicks(), payload.primaryValue()),
                    payload.active());
            case METEOR_TARGET -> putOrRemove(
                    METEOR_TARGETS, payload.sourceEntityId(),
                    new MeteorTarget(payload.durationTicks(), payload.position(), payload.primaryValue(), payload.secondaryValue()),
                    payload.active());
            case FIELD_EXTRACTION_BEAM -> putOrRemove(
                    EXTRACTION_BEAMS, pairKey(payload.sourceEntityId(), payload.targetEntityId()),
                    new TimedPair(payload.durationTicks(), payload.sourceEntityId(), payload.targetEntityId()),
                    payload.active());
            case ORB_TETHER -> putOrRemove(
                    ORB_TETHERS, pairKey(payload.sourceEntityId(), payload.targetEntityId()),
                    new TimedOrbTether(payload.durationTicks(), payload.sourceEntityId(), payload.targetEntityId(), payload.position()),
                    payload.active());
            case SEDATED -> putOrRemove(
                    SEDATED, payload.sourceEntityId(),
                    new Timed(payload.durationTicks()),
                    payload.active());
            case NANO_SURGE -> putOrRemove(
                    NANO_SURGE, payload.sourceEntityId(),
                    new Timed(payload.durationTicks()),
                    payload.active());
            case COALESCENCE_BEAM -> putOrRemove(
                    COALESCENCE_BEAMS, payload.sourceEntityId(),
                    new Timed(payload.durationTicks()),
                    payload.active());
            case FADE -> putOrRemove(
                    FADING, payload.sourceEntityId(),
                    new Timed(payload.durationTicks()),
                    payload.active());
            case ROCKET_PUNCH_IMPACT -> {
                addOneShot(payload);
                shakeFromImpact(payload.position(), 0f);
            }
            case SEISMIC_SLAM_RING -> {
                addOneShot(payload);
                shakeFromImpact(payload.position(), 0f);
            }
            case METEOR_IMPACT -> {
                addOneShot(payload);
                shakeFromImpact(payload.position(), 0.5f);
            }
            case UPPERCUT_BURST -> addOneShot(payload);
            case NANO_SURGE_CAST -> addOneShot(payload);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            clear();
            return;
        }

        clientTicks++;
        tickMap(ROCKET_CHARGES);
        tickMap(METEOR_TARGETS);
        tickMap(EXTRACTION_BEAMS);
        tickMap(ORB_TETHERS);
        tickMap(SEDATED);
        tickMap(NANO_SURGE);
        tickMap(COALESCENCE_BEAMS);
        tickMap(FADING);
        ONE_SHOTS.removeIf(shot -> clientTicks - shot.startTick >= shot.duration);

        SEDATED.keySet().forEach(id -> renderSleepZ(level, id));
        spawnFlightTrails(level);
    }

    public static void renderWorld(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (ROCKET_CHARGES.isEmpty()
                && METEOR_TARGETS.isEmpty()
                && EXTRACTION_BEAMS.isEmpty()
                && ORB_TETHERS.isEmpty()
                && NANO_SURGE.isEmpty()
                && COALESCENCE_BEAMS.isEmpty()
                && ONE_SHOTS.isEmpty())
            return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(GAUNTLET_GLOW.bufferSize());
        poseStack.pushPose();
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            BufferBuilder buffer = new BufferBuilder(byteBuffer, GAUNTLET_GLOW.mode(), GAUNTLET_GLOW.format());
            DrawState drawState = new DrawState(buffer, poseStack.last().pose(), camera);

            ROCKET_CHARGES.forEach((id, effect) -> renderRocketCharge(level, drawState, id, effect.value));
            METEOR_TARGETS.values().forEach(effect -> renderMeteorTarget(level, drawState, effect));
            EXTRACTION_BEAMS.values().forEach(effect -> renderExtractionBeam(level, drawState, effect.sourceId, effect.targetId));
            ORB_TETHERS.values().forEach(effect -> renderOrbTether(level, drawState, effect));
            NANO_SURGE.keySet().forEach(id -> renderNanoSurge(level, drawState, id));
            COALESCENCE_BEAMS.keySet().forEach(id -> renderCoalescenceBeam(level, drawState, id));
            ONE_SHOTS.forEach(shot -> renderOneShot(drawState, shot));

            MeshData mesh = buffer.build();
            if (drawState.hasVertices && mesh != null) {
                GAUNTLET_GLOW.draw(mesh);
            }
        } finally {
            poseStack.popPose();
            byteBuffer.close();
        }
    }

    public static boolean isLocalPlayerSedated() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && SEDATED.containsKey(mc.player.getId());
    }

    public static boolean isSedated(int entityId) {
        return SEDATED.containsKey(entityId);
    }

    public static boolean isFading(int entityId) {
        return FADING.containsKey(entityId);
    }

    public static boolean isNanoSurgeActive(int entityId) {
        return NANO_SURGE.containsKey(entityId);
    }

    public static NanoSurgeRenderData nanoSurgeRenderData(int entityId) {
        Timed effect = NANO_SURGE.get(entityId);
        if (effect == null) return null;
        return new NanoSurgeRenderData(entityId, effect.ticksRemaining);
    }

    public static RocketPunchChargeRenderData rocketPunchChargeRenderData(int entityId) {
        TimedFloat effect = ROCKET_CHARGES.get(entityId);
        if (effect == null) return null;
        return new RocketPunchChargeRenderData(entityId, effect.value);
    }

    private static void clear() {
        ROCKET_CHARGES.clear();
        METEOR_TARGETS.clear();
        EXTRACTION_BEAMS.clear();
        ORB_TETHERS.clear();
        SEDATED.clear();
        NANO_SURGE.clear();
        COALESCENCE_BEAMS.clear();
        FADING.clear();
        ONE_SHOTS.clear();
    }

    private static <K, V extends Timed> void putOrRemove(Map<K, V> map, K key, V value, boolean active) {
        if (active) map.put(key, value);
        else map.remove(key);
    }

    private static void tickMap(Map<?, ? extends Timed> map) {
        Iterator<? extends Timed> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            if (--iterator.next().ticksRemaining <= 0) iterator.remove();
        }
    }

    private static void addOneShot(ClientboundGauntletEffectPayload payload) {
        // The caster may receive the same one-shot twice (direct send + range broadcast); ignore duplicates
        for (OneShot shot : ONE_SHOTS) {
            if (shot.type == payload.effectType()
                    && shot.startTick == clientTicks
                    && shot.pos.distanceToSqr(payload.position()) < 1.0E-6)
                return;
        }
        ONE_SHOTS.add(new OneShot(
                payload.effectType(), payload.position(), payload.primaryValue(),
                clientTicks, Math.max(1, payload.durationTicks())));
    }

    private static void shakeFromImpact(Vec3 pos, float minIntensity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double dist = mc.player.position().distanceTo(pos);
        if (dist > IMPACT_SHAKE_RANGE) return;
        float intensity = (float) (0.9 * (1.0 - dist / IMPACT_SHAKE_RANGE));
        ClientCameraEffects.shake(Math.max(intensity, minIntensity));
    }

    private static void renderRocketCharge(ClientLevel level, DrawState draw, int entityId, float charge) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;

        charge = Math.clamp(charge, 0f, 1f);
        HandPlacement hand = rocketPunchHandPlacement(entity, draw.camera, charge);

        int baseColor = chargeColor(charge);
        double pulse = 0.72 + 0.28 * Math.sin((clientTicks + charge * 16.0) * 0.28);
        float size = hand.scale;

        addBillboard(draw, hand.position.add(entity.getLookAngle().scale(0.04)), size, mixColor(baseColor, 0xFFFFFF, 0.35), (float) (0.42 * pulse), true);
        addRingBillboards(draw, hand.position, size * 1.25, baseColor, (float) (0.28 + charge * 0.24), 8, clientTicks * 0.16);
    }

    private static void renderMeteorTarget(ClientLevel level, DrawState draw, MeteorTarget effect) {
        double outer = Math.max(effect.outerRadius, effect.innerRadius);
        if (effect.shouldRebuildSurfaces(clientTicks)) {
            rebuildMeteorTargetSurfaces(level, effect, outer);
        }

        double pulse = 0.78 + 0.22 * Math.sin(clientTicks * 0.22);
        for (MeteorSurfaceFace face : effect.surfaceFaces) {
            if (face.direction == Direction.UP) continue;
            renderMeteorSurfaceFace(draw, effect, outer, pulse, face);
        }
        for (MeteorSurfaceFace face : effect.surfaceFaces) {
            if (face.direction != Direction.UP) continue;
            renderMeteorSurfaceFace(draw, effect, outer, pulse, face);
        }

        renderMeteorTargetBeacon(draw, effect, outer);
    }

    private static void renderMeteorSurfaceFace(
            DrawState draw,
            MeteorTarget effect,
            double outer,
            double pulse,
            MeteorSurfaceFace face) {
        boolean inner = face.horizontalDist <= effect.innerRadius;
        double fade = inner
                ? 1.0
                : 1.0 - (face.horizontalDist - effect.innerRadius) / Math.max(0.01, outer - effect.innerRadius);
        int color = inner ? 0xFF2424 : mixColor(0xFF7600, 0xFFD45A, fade * 0.72);
        float alpha = (float) ((inner ? 0.45 : 0.26 * fade) * pulse * face.alphaMultiplier);
        addShapeBoxFace(draw, face.pos, face.box, face.direction, color, alpha);
    }

    private static void rebuildMeteorTargetSurfaces(ClientLevel level, MeteorTarget effect, double outer) {
        effect.surfaceFaces.clear();
        effect.lastSurfaceBuildTick = clientTicks;
        int minX = (int) Math.floor(effect.position.x - outer);
        int maxX = (int) Math.ceil(effect.position.x + outer);
        int minY = (int) Math.floor(effect.position.y - METEOR_BLOCK_SCAN_VERTICAL_BELOW);
        int maxY = (int) Math.ceil(effect.position.y + METEOR_BLOCK_SCAN_VERTICAL_ABOVE);
        int minZ = (int) Math.floor(effect.position.z - outer);
        int maxZ = (int) Math.ceil(effect.position.z + outer);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            collectMeteorBlockCoating(level, effect, outer, pos, state);
        }
    }

    private static void renderExtractionBeam(ClientLevel level, DrawState draw, int sourceId, int targetId) {
        Entity source = level.getEntity(sourceId);
        Entity target = level.getEntity(targetId);
        if (source == null || target == null) return;

        Vec3 start = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 end = source.position().add(0, source.getBbHeight() * 0.58, 0);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.1) return;

        int steps = Math.max(2, Math.min(32, (int) (length * 2.2)));
        for (int i = 0; i < steps; i++) {
            double t0 = (double) i / steps;
            double t1 = (double) (i + 1) / steps;
            Vec3 p0 = start.add(delta.scale(t0));
            Vec3 p1 = start.add(delta.scale(t1));
            double pulse = 0.5 + 0.5 * Math.sin(clientTicks * 0.42 - ((t0 + t1) * 0.5) * 10.0);
            int core = mixColor(0x31E8FF, 0xFFFFFF, pulse);
            addBeamSegment(draw, p0, p1, 0.22f, 0x20E4FF, 0.20f);
            addBeamSegment(draw, p0, p1, 0.08f, core, (float) (0.52 + pulse * 0.28));
        }
    }

    private static void renderOrbTether(ClientLevel level, DrawState draw, TimedOrbTether effect) {
        Entity orb = level.getEntity(effect.sourceId);
        Entity target = level.getEntity(effect.targetId);
        if (target == null) return;

        Vec3 start = orb != null ? orb.position() : effect.orbFallback;
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.1) return;

        int steps = Math.max(2, Math.min(32, (int) (length * 2.2)));
        for (int i = 0; i < steps; i++) {
            double t0 = (double) i / steps;
            double t1 = (double) (i + 1) / steps;
            Vec3 p0 = start.add(delta.scale(t0));
            Vec3 p1 = start.add(delta.scale(t1));
            double pulse = 0.5 + 0.5 * Math.sin(clientTicks * 0.42 - ((t0 + t1) * 0.5) * 10.0);
            int core = mixColor(0xB04AD8, 0xFFFFFF, pulse);
            addBeamSegment(draw, p0, p1, 0.20f, 0xB04AD8, 0.18f);
            addBeamSegment(draw, p0, p1, 0.07f, core, (float) (0.50 + pulse * 0.28));
        }
    }

    private static void renderNanoSurge(ClientLevel level, DrawState draw, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;

        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        Vec3 look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        Vec3 right = look.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 forward = right.cross(UP).normalize();
        float width = (float) Math.max(0.28, entity.getBbWidth() * 0.46);
        float height = entity.getBbHeight();
        double phase = clientTicks * 0.24 + entityId * 0.61;
        int color = mixColor(0x14DFFF, 0xBDFBFF, 0.5 + 0.5 * Math.sin(phase));

        for (int i = 0; i < 5; i++) {
            double t0 = (i + 0.15 + 0.10 * Math.sin(phase + i)) / 5.4;
            double t1 = Math.min(0.96, t0 + 0.18 + 0.04 * Math.sin(phase * 1.3 + i));
            double side0 = Math.sin(phase + i * 1.9) * width;
            double side1 = Math.sin(phase + i * 1.9 + 1.15) * width;
            double front0 = Math.cos(phase * 0.7 + i) * width * 0.35;
            double front1 = Math.cos(phase * 0.7 + i + 0.8) * width * 0.35;
            Vec3 p0 = center.add(right.scale(side0)).add(forward.scale(front0)).add(0, (t0 - 0.5) * height, 0);
            Vec3 p1 = center.add(right.scale(side1)).add(forward.scale(front1)).add(0, (t1 - 0.5) * height, 0);
            addBeamSegment(draw, p0, p1, 0.035f, color, 0.58f);
            addBillboard(draw, p1, 0.055f, 0xD8FFFF, 0.42f, true);
        }

        addBillboard(draw, center.add(0, height * 0.10, 0), width * 1.25f, 0x54EFFF, 0.10f, true);
    }

    private static void renderCoalescenceBeam(ClientLevel level, DrawState draw, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;

        // Beam starts slightly forward and below the eyes; kept in sync with the server hit test.
        Vec3 direction = entity.getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0E-6) direction = new Vec3(0, 0, 1);
        Vec3 origin = entity.getEyePosition().add(direction.scale(0.5)).subtract(0, 0.45, 0);
        double length = OLRUConfig.FINAL_ANSWER.COALESCENCE.length.get();

        Vec3 right = direction.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(direction).normalize();

        int steps = Math.max(4, (int) (length / 2.0));
        for (int i = 0; i < steps; i++) {
            Vec3 p0 = origin.add(direction.scale(length * i / steps));
            Vec3 p1 = origin.add(direction.scale(length * (i + 1) / steps));
            double pulse = 0.75 + 0.25 * Math.sin(clientTicks * 0.42 - i * 0.9);
            addBeamSegment(draw, p0, p1, 0.75f, 0x8A2BE2, (float) (0.5 * pulse));
            addBeamSegment(draw, p0, p1, 0.28f, 0xB04AD8, (float) (0.95 * pulse));
        }

        int helixPoints = steps * 3;
        Vec3 prev = null;
        for (int i = 0; i <= helixPoints; i++) {
            double t = (double) i / helixPoints;
            double angle = clientTicks * 0.25 + t * length * 1.4;
            Vec3 offset = right.scale(Math.cos(angle) * 0.35).add(up.scale(Math.sin(angle) * 0.35));
            Vec3 p = origin.add(direction.scale(length * t)).add(offset);
            if (prev != null) {
                addBeamSegment(draw, prev, p, 0.05f, 0xFFD75A, 0.9f);
            }
            prev = p;
        }

        Vec3 end = origin.add(direction.scale(length));
        addBillboard(draw, end, 0.9f, 0xB04AD8, 0.55f, true);
        addBillboard(draw, end, 0.4f, 0xFFD75A, 0.85f, true);
        addBillboard(draw, origin.add(direction.scale(0.3)), 0.45f, 0xB04AD8, 0.3f, true);
    }

    private static void renderOneShot(DrawState draw, OneShot shot) {
        float progress = Math.clamp((float) (clientTicks - shot.startTick) / shot.duration, 0f, 1f);
        switch (shot.type) {
            case ROCKET_PUNCH_IMPACT -> renderRocketPunchImpact(draw, shot, progress);
            case SEISMIC_SLAM_RING -> renderSeismicSlamRing(draw, shot, progress);
            case METEOR_IMPACT -> renderMeteorImpact(draw, shot, progress);
            case UPPERCUT_BURST -> renderUppercutBurst(draw, shot, progress);
            case NANO_SURGE_CAST -> renderNanoSurgeCast(draw, shot, progress);
            default -> {}
        }
    }

    private static void renderRocketPunchImpact(DrawState draw, OneShot shot, float progress) {
        double radius = progress * 1.8 * Math.max(0.2f, shot.param);
        int color = mixColor(0xFFD75A, 0xFFFFFF, progress);
        float alpha = 0.85f * (1.0f - progress);
        addHorizontalRing(draw, shot.pos.add(0, 0.1, 0), radius, 0.14f, color, alpha, 40);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            Vec3 spike = shot.pos.add(Math.cos(angle) * (radius + 0.25), 0.18, Math.sin(angle) * (radius + 0.25));
            addBillboard(draw, spike, 0.11f * (1.0f - progress * 0.5f), color, alpha * 0.9f, true);
        }
    }

    private static void renderSeismicSlamRing(DrawState draw, OneShot shot, float progress) {
        double radius = Math.max(0.5f, shot.param);
        addHorizontalRing(draw, shot.pos.add(0, 0.09, 0), progress * radius, 0.16f, 0xFF9A30, 0.55f * (1.0f - progress), 56);
        float delayed = (float) Math.pow(progress, 0.8);
        addHorizontalRing(draw, shot.pos.add(0, 0.15, 0), delayed * radius * 1.15, 0.11f, 0xFFC94A, 0.45f * (1.0f - delayed), 56);
    }

    private static void renderMeteorImpact(DrawState draw, OneShot shot, float progress) {
        double radius = progress * Math.max(0.5f, shot.param);
        int ringColor = mixColor(0xFF5A1E, 0xFF9A30, progress);
        addHorizontalRing(draw, shot.pos.add(0, 0.12, 0), radius, 0.22f, ringColor, 0.65f * (1.0f - progress), 64);

        float width = 0.7f * (1.0f - progress * 0.5f);
        float alpha = 0.5f * (1.0f - progress);
        Vec3 bottom = shot.pos;
        Vec3 top = shot.pos.add(0, 12, 0);
        addQuad(draw, bottom.subtract(width, 0, 0), bottom.add(width, 0, 0), top.add(width, 0, 0), top.subtract(width, 0, 0), 0xFF6A20, alpha);
        addQuad(draw, bottom.subtract(0, 0, width), bottom.add(0, 0, width), top.add(0, 0, width), top.subtract(0, 0, width), 0xFF6A20, alpha);
    }

    private static void renderUppercutBurst(DrawState draw, OneShot shot, float progress) {
        float alpha = 0.75f * (1.0f - progress);
        for (int i = 0; i < 10; i++) {
            double t = i / 9.0;
            if (progress < t * 0.7) continue;
            double angle = t * Math.PI * 4.0 + clientTicks * 0.05;
            Vec3 p = shot.pos.add(Math.cos(angle) * 0.45, 0.15 + t * 2.0, Math.sin(angle) * 0.45);
            addBillboard(draw, p, 0.09f, mixColor(0xFFF6C8, 0xFFFFFF, t), alpha, true);
        }
    }

    private static void renderNanoSurgeCast(DrawState draw, OneShot shot, float progress) {
        addHorizontalRing(draw, shot.pos.add(0, 0.1, 0), progress * 3.0, 0.14f, 0x31E8FF, 0.55f * (1.0f - progress), 48);
        for (int i = 0; i < 6; i++) {
            double y = 0.25 + i * 0.35 + progress * 0.8;
            float fade = (1.0f - progress) * (1.0f - i / 6.0f * 0.5f);
            addBillboard(draw, shot.pos.add(0, y, 0), 0.35f - i * 0.03f, 0x31E8FF, 0.28f * fade, true);
        }
    }

    private static void renderSleepZ(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        Vec3 origin = entity.position().add(
                Math.sin(clientTicks * 0.08 + entityId) * 0.18,
                entity.getBbHeight() + 0.45 + (clientTicks % 20) * 0.015,
                Math.cos(clientTicks * 0.08 + entityId) * 0.18);
        drawZ(level, origin, 0.18, 0x8DDCFF);
    }

    private static void spawnFlightTrails(ClientLevel level) {
        for (Player player : level.players()) {
            GauntletPoseType pose = ClientGauntletAnimations.getActivePoseType(player.getId());
            if (pose == null) continue;
            Vec3 behind = player.position()
                    .add(0, player.getBbHeight() * 0.45, 0)
                    .subtract(player.getLookAngle().scale(0.45));
            if (pose == GauntletPoseType.ROCKET_PUNCH_FLIGHT) {
                level.addParticle(ParticleTypes.FLAME, behind.x, behind.y, behind.z, 0.0, 0.0, 0.0);
                if (clientTicks % 3 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, behind.x, behind.y + 0.1, behind.z, 0.0, 0.01, 0.0);
                }
            } else if (pose == GauntletPoseType.METEOR_DIVE) {
                level.addParticle(ParticleTypes.FLAME, behind.x, behind.y, behind.z, 0.0, 0.02, 0.0);
                level.addParticle(ParticleTypes.FIREWORK, behind.x, behind.y, behind.z,
                        (level.getRandom().nextDouble() - 0.5) * 0.15, 0.05, (level.getRandom().nextDouble() - 0.5) * 0.15);
            }
        }
    }

    private static HandPlacement rocketPunchHandPlacement(Entity entity, Vec3 camera, float charge) {
        Minecraft mc = Minecraft.getInstance();
        int side = mainHandSide(entity);
        Vec3 look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        Vec3 right = look.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);

        if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Vec3 up = right.cross(look).normalize();
            Vec3 position = camera
                    .add(look.scale(0.58))
                    .add(right.scale(0.34 * side))
                    .subtract(up.scale(0.30));
            return new HandPlacement(position, (float) (0.12 + charge * 0.12));
        }

        Vec3 position = entity.position()
                .add(0, entity.getBbHeight() * 0.58, 0)
                .add(right.scale(0.38 * side))
                .add(look.scale(0.35));
        return new HandPlacement(position, (float) (0.26 + charge * 0.24));
    }

    private static int mainHandSide(Entity entity) {
        if (entity instanceof LivingEntity living && living.getMainArm() == HumanoidArm.LEFT) {
            return -1;
        }
        return 1;
    }

    private static void renderMeteorTargetBeacon(DrawState draw, MeteorTarget effect, double outer) {
        double ringY = effect.position.y + 0.12;
        Vec3 center = new Vec3(effect.position.x, ringY, effect.position.z);
        int segments = 72;
        double pulse = 0.78 + 0.22 * Math.sin(clientTicks * 0.28);
        addHorizontalRing(draw, center, effect.innerRadius, 0.10f, 0xFF2626, (float) (0.56 * pulse), segments);
        addHorizontalRing(draw, center.add(0, 0.08, 0), outer, 0.12f, 0xFF8A00, (float) (0.34 * pulse), segments);
        addVerticalBeacon(draw, center, (float) Math.max(0.25, outer * 0.13), 4.5f, 0xFF3A20, 0.15f);
        addVerticalBeacon(draw, center, (float) Math.max(0.12, effect.innerRadius * 0.10), 5.5f, 0xFFD050, 0.20f);
    }

    private static void collectMeteorBlockCoating(
            ClientLevel level,
            MeteorTarget effect,
            double outer,
            BlockPos pos,
            BlockState state) {
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) return;
        List<AABB> boxes = shape.toAabbs();

        for (AABB box : boxes) {
            for (Direction direction : Direction.values()) {
                if (direction == Direction.DOWN) continue;
                if (isMeteorFaceOccluded(level, pos, box, boxes, direction)) continue;

                Vec3 faceCenter = meteorFaceCenter(pos, box, direction);
                double horizontalDist = new Vec3(faceCenter.x - effect.position.x, 0, faceCenter.z - effect.position.z).length();
                if (horizontalDist > outer) continue;

                float alphaMultiplier = direction == Direction.UP ? 1.0f : 0.38f;
                effect.surfaceFaces.add(new MeteorSurfaceFace(pos.immutable(), box, direction, horizontalDist, alphaMultiplier));
            }
        }
    }

    private static void addBillboard(DrawState draw, Vec3 center, float size, int color, float alpha, boolean faceCamera) {
        Vec3 toCamera = faceCamera ? draw.camera.subtract(center).normalize() : new Vec3(0, 0, 1);
        if (toCamera.lengthSqr() < 1.0E-6) toCamera = new Vec3(0, 0, 1);
        Vec3 right = UP.cross(toCamera).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = toCamera.cross(right).normalize();
        Vec3 r = right.scale(size);
        Vec3 u = up.scale(size);
        addQuad(draw, center.subtract(r).subtract(u), center.add(r).subtract(u), center.add(r).add(u), center.subtract(r).add(u), color, alpha);
    }

    private static void addHorizontalRing(DrawState draw, Vec3 center, double radius, float width, int color, float alpha, int segments) {
        double inner = Math.max(0.05, radius - width * 0.5);
        double outer = radius + width * 0.5;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            Vec3 p0 = center.add(Math.cos(a0) * inner, 0, Math.sin(a0) * inner);
            Vec3 p1 = center.add(Math.cos(a1) * inner, 0, Math.sin(a1) * inner);
            Vec3 p2 = center.add(Math.cos(a1) * outer, 0, Math.sin(a1) * outer);
            Vec3 p3 = center.add(Math.cos(a0) * outer, 0, Math.sin(a0) * outer);
            addQuad(draw, p0, p1, p2, p3, color, alpha);
        }
    }

    private static void addVerticalBeacon(DrawState draw, Vec3 center, float radius, float height, int color, float alpha) {
        Vec3 bottom = center;
        Vec3 top = center.add(0, height, 0);
        for (int i = 0; i < 4; i++) {
            double angle = clientTicks * 0.035 + i * Math.PI / 4.0;
            Vec3 side = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(radius);
            addQuad(draw, bottom.subtract(side), bottom.add(side), top.add(side), top.subtract(side), color, alpha);
        }
    }

    private static void addRingBillboards(DrawState draw, Vec3 center, double radius, int color, float alpha, int points, double spin) {
        for (int i = 0; i < points; i++) {
            double a = spin + i * Math.PI * 2.0 / points;
            Vec3 p = center.add(Math.cos(a) * radius, Math.sin(a * 1.7 + clientTicks * 0.12) * 0.08, Math.sin(a) * radius);
            addBillboard(draw, p, 0.07f, color, alpha, true);
        }
    }

    private static void addBeamSegment(DrawState draw, Vec3 p0, Vec3 p1, float width, int color, float alpha) {
        Vec3 dir = p1.subtract(p0).normalize();
        if (dir.lengthSqr() < 1.0E-6) return;
        Vec3 mid = p0.add(p1).scale(0.5);
        Vec3 toCamera = draw.camera.subtract(mid).normalize();
        Vec3 side = dir.cross(toCamera).normalize();
        if (side.lengthSqr() < 1.0E-6) side = dir.cross(UP).normalize();
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        side = side.scale(width);
        addQuad(draw, p0.subtract(side), p0.add(side), p1.add(side), p1.subtract(side), color, alpha);
    }

    private static void addShapeBoxFace(DrawState draw, BlockPos pos, AABB box, Direction direction, int color, float alpha) {
        double x0 = pos.getX() + box.minX;
        double y0 = pos.getY() + box.minY;
        double z0 = pos.getZ() + box.minZ;
        double x1 = pos.getX() + box.maxX;
        double y1 = pos.getY() + box.maxY;
        double z1 = pos.getZ() + box.maxZ;
        Vec3 n = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()).scale(0.006);

        switch (direction) {
            case UP -> addQuad(draw, new Vec3(x0, y1, z1).add(n), new Vec3(x1, y1, z1).add(n), new Vec3(x1, y1, z0).add(n), new Vec3(x0, y1, z0).add(n), color, alpha);
            case DOWN -> addQuad(draw, new Vec3(x0, y0, z0).add(n), new Vec3(x1, y0, z0).add(n), new Vec3(x1, y0, z1).add(n), new Vec3(x0, y0, z1).add(n), color, alpha);
            case NORTH -> addQuad(draw, new Vec3(x1, y0, z0).add(n), new Vec3(x0, y0, z0).add(n), new Vec3(x0, y1, z0).add(n), new Vec3(x1, y1, z0).add(n), color, alpha);
            case SOUTH -> addQuad(draw, new Vec3(x0, y0, z1).add(n), new Vec3(x1, y0, z1).add(n), new Vec3(x1, y1, z1).add(n), new Vec3(x0, y1, z1).add(n), color, alpha);
            case WEST -> addQuad(draw, new Vec3(x0, y0, z0).add(n), new Vec3(x0, y0, z1).add(n), new Vec3(x0, y1, z1).add(n), new Vec3(x0, y1, z0).add(n), color, alpha);
            case EAST -> addQuad(draw, new Vec3(x1, y0, z1).add(n), new Vec3(x1, y0, z0).add(n), new Vec3(x1, y1, z0).add(n), new Vec3(x1, y1, z1).add(n), color, alpha);
        }
    }

    private static boolean isMeteorFaceOccluded(ClientLevel level, BlockPos pos, AABB box, List<AABB> sameBlockBoxes, Direction direction) {
        if (isMeteorFaceCoveredBySameBlockShape(box, sameBlockBoxes, direction)) return true;

        double edge = switch (direction) {
            case DOWN -> box.minY;
            case UP -> box.maxY;
            case NORTH -> box.minZ;
            case SOUTH -> box.maxZ;
            case WEST -> box.minX;
            case EAST -> box.maxX;
        };
        boolean atBlockBoundary = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? edge >= 0.999
                : edge <= 0.001;
        if (!atBlockBoundary) return false;

        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return !neighbor.isAir();
    }

    private static Vec3 meteorFaceCenter(BlockPos pos, AABB box, Direction direction) {
        double x = pos.getX() + (box.minX + box.maxX) * 0.5;
        double y = pos.getY() + (box.minY + box.maxY) * 0.5;
        double z = pos.getZ() + (box.minZ + box.maxZ) * 0.5;

        switch (direction) {
            case DOWN -> y = pos.getY() + box.minY;
            case UP -> y = pos.getY() + box.maxY;
            case NORTH -> z = pos.getZ() + box.minZ;
            case SOUTH -> z = pos.getZ() + box.maxZ;
            case WEST -> x = pos.getX() + box.minX;
            case EAST -> x = pos.getX() + box.maxX;
        }

        return new Vec3(x, y, z);
    }

    private static boolean isMeteorFaceCoveredBySameBlockShape(AABB box, List<AABB> boxes, Direction direction) {
        double faceArea = meteorFaceArea(box, direction);
        if (faceArea <= 1.0E-7) return true;

        double coveredArea = 0.0;
        for (AABB other : boxes) {
            if (other == box || !meteorBoxesTouchOnFace(box, other, direction)) continue;
            coveredArea += meteorFaceOverlapArea(box, other, direction);
            if (coveredArea >= faceArea * 0.98) return true;
        }
        return false;
    }

    private static boolean meteorBoxesTouchOnFace(AABB box, AABB other, Direction direction) {
        return switch (direction) {
            case DOWN -> nearlyEqual(box.minY, other.maxY);
            case UP -> nearlyEqual(box.maxY, other.minY);
            case NORTH -> nearlyEqual(box.minZ, other.maxZ);
            case SOUTH -> nearlyEqual(box.maxZ, other.minZ);
            case WEST -> nearlyEqual(box.minX, other.maxX);
            case EAST -> nearlyEqual(box.maxX, other.minX);
        };
    }

    private static double meteorFaceArea(AABB box, Direction direction) {
        return switch (direction.getAxis()) {
            case Y -> Math.max(0.0, box.maxX - box.minX) * Math.max(0.0, box.maxZ - box.minZ);
            case Z -> Math.max(0.0, box.maxX - box.minX) * Math.max(0.0, box.maxY - box.minY);
            case X -> Math.max(0.0, box.maxZ - box.minZ) * Math.max(0.0, box.maxY - box.minY);
        };
    }

    private static double meteorFaceOverlapArea(AABB box, AABB other, Direction direction) {
        return switch (direction.getAxis()) {
            case Y -> overlap(box.minX, box.maxX, other.minX, other.maxX) * overlap(box.minZ, box.maxZ, other.minZ, other.maxZ);
            case Z -> overlap(box.minX, box.maxX, other.minX, other.maxX) * overlap(box.minY, box.maxY, other.minY, other.maxY);
            case X -> overlap(box.minZ, box.maxZ, other.minZ, other.maxZ) * overlap(box.minY, box.maxY, other.minY, other.maxY);
        };
    }

    private static double overlap(double a0, double a1, double b0, double b1) {
        return Math.max(0.0, Math.min(a1, b1) - Math.max(a0, b0));
    }

    private static boolean nearlyEqual(double a, double b) {
        return Math.abs(a - b) <= 1.0E-5;
    }

    private static void addQuad(DrawState draw, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        addVertex(draw, a, color, alpha);
        addVertex(draw, b, color, alpha);
        addVertex(draw, c, color, alpha);
        addVertex(draw, d, color, alpha);
    }

    private static void addVertex(DrawState draw, Vec3 pos, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        draw.buffer.addVertex(draw.matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, Math.clamp(alpha, 0f, 1f));
        draw.hasVertices = true;
    }

    private static void drawZ(ClientLevel level, Vec3 origin, double scale, int color) {
        for (int i = 0; i < 5; i++) {
            double t = i / 4.0;
            addDust(level, origin.add((t - 0.5) * scale, 0, 0), color);
            addDust(level, origin.add((0.5 - t) * scale, -t * scale, 0), color);
            addDust(level, origin.add((t - 0.5) * scale, -scale, 0), color);
        }
    }

    private static void addDust(ClientLevel level, Vec3 pos, int color) {
        level.addParticle(new DustParticleOptions(color, 0.7f), pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    private static int chargeColor(float charge) {
        charge = Math.clamp(charge, 0f, 1f);
        if (charge < 0.45f) return mixColor(0xFFF2A0, 0xFFD200, charge / 0.45f);
        return mixColor(0xFFD200, 0xFF2020, (charge - 0.45f) / 0.55f);
    }

    private static int mixColor(int a, int b, double t) {
        t = Math.clamp((float) t, 0f, 1f);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (rr << 16) | (rg << 8) | rb;
    }

    private static long pairKey(int a, int b) {
        return ((long) a << 32) ^ (b & 0xffffffffL);
    }

    private static class DrawState {
        final BufferBuilder buffer;
        final Matrix4fc matrix;
        final Vec3 camera;
        boolean hasVertices = false;

        DrawState(BufferBuilder buffer, Matrix4fc matrix, Vec3 camera) {
            this.buffer = buffer;
            this.matrix = matrix;
            this.camera = camera;
        }
    }

    private static class HandPlacement {
        final Vec3 position;
        final float scale;

        HandPlacement(Vec3 position, float scale) {
            this.position = position;
            this.scale = scale;
        }
    }

    private static class Timed {
        int ticksRemaining;

        Timed(int ticksRemaining) {
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static class OneShot {
        final EffectType type;
        final Vec3 pos;
        final float param;
        final int startTick;
        final int duration;

        OneShot(EffectType type, Vec3 pos, float param, int startTick, int duration) {
            this.type = type;
            this.pos = pos;
            this.param = param;
            this.startTick = startTick;
            this.duration = duration;
        }
    }

    private static class TimedFloat extends Timed {
        final float value;

        TimedFloat(int ticksRemaining, float value) {
            super(ticksRemaining);
            this.value = value;
        }
    }

    private static class TimedPair extends Timed {
        final int sourceId;
        final int targetId;

        TimedPair(int ticksRemaining, int sourceId, int targetId) {
            super(ticksRemaining);
            this.sourceId = sourceId;
            this.targetId = targetId;
        }
    }

    private static class TimedOrbTether extends TimedPair {
        final Vec3 orbFallback;

        TimedOrbTether(int ticksRemaining, int sourceId, int targetId, Vec3 orbFallback) {
            super(ticksRemaining, sourceId, targetId);
            this.orbFallback = orbFallback;
        }
    }

    private static class MeteorTarget extends Timed {
        final Vec3 position;
        final float innerRadius;
        final float outerRadius;
        final List<MeteorSurfaceFace> surfaceFaces = new ArrayList<>();
        int lastSurfaceBuildTick = Integer.MIN_VALUE;

        MeteorTarget(int ticksRemaining, Vec3 position, float innerRadius, float outerRadius) {
            super(ticksRemaining);
            this.position = position;
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
        }

        boolean shouldRebuildSurfaces(int tick) {
            return lastSurfaceBuildTick == Integer.MIN_VALUE || tick - lastSurfaceBuildTick >= METEOR_SURFACE_REBUILD_INTERVAL_TICKS;
        }
    }

    private record MeteorSurfaceFace(BlockPos pos, AABB box, Direction direction, double horizontalDist,
            float alphaMultiplier) {}
}
