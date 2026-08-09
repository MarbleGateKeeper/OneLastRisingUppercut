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
    private static final Map<Integer, GraviticZone> GRAVITIC_ZONES = new HashMap<>();
    private static final Map<Long, TimedPair> EXTRACTION_BEAMS = new HashMap<>();
    private static final Map<Long, TimedOrbTether> ORB_TETHERS = new HashMap<>();
    private static final Map<Integer, Timed> SEDATED = new HashMap<>();
    private static final Map<Integer, Timed> NANO_SURGE = new HashMap<>();
    private static final Map<Integer, Timed> COALESCENCE_BEAMS = new HashMap<>();
    private static final Map<Integer, Timed> FADING = new HashMap<>();
    private static final Map<Integer, Timed> KINETIC_GRASP_FIELDS = new HashMap<>();
    private static final Map<Integer, TimedTargetPos> GRASP_TETHERS = new HashMap<>();
    private static final Map<Long, TimedPair> BIOTIC_SPRAY_CONTACTS = new HashMap<>();
    private static final Map<Long, TimedFloatPair> COALESCENCE_CONTACTS = new HashMap<>();
    private static final Map<Integer, FluxField> FLUX_FIELDS = new HashMap<>();
    private static final Map<Integer, TimedFloat> FLUX_TARGETS = new HashMap<>();
    private static final List<AbsorbTrail> ABSORB_TRAILS = new ArrayList<>();
    private static final List<OneShot> ONE_SHOTS = new ArrayList<>();

    public static final RenderType GAUNTLET_GLOW = RenderType.create(
            "olru_gauntlet_glow",
            RenderSetup.builder(GauntletVfx.GLOW_PIPELINE)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup());

    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int METEOR_BLOCK_SCAN_VERTICAL_BELOW = 2;
    private static final int METEOR_BLOCK_SCAN_VERTICAL_ABOVE = 4;
    private static final int METEOR_SURFACE_REBUILD_INTERVAL_TICKS = 5;
    private static final double IMPACT_SHAKE_RANGE = 12.0;
    /** Kinetic Grasp black-hole anchor: ahead of the eyes, offset to the main-hand side and down. */
    private static final double GRASP_ANCHOR_DISTANCE = 1.55;
    private static final double GRASP_ANCHOR_SIDE = 0.82;
    private static final double GRASP_ANCHOR_DOWN = 0.32;
    private static final float GRASP_HORIZON_RADIUS = 0.145f;
    private static final double EFFECT_RENDER_DISTANCE_SQR = 96.0 * 96.0;
    private static final double DETAIL_RENDER_DISTANCE_SQR = 36.0 * 36.0;
    private static final int MAX_CONTACT_EFFECTS = 96;
    private static final int MAX_TRACKED_TARGETS = 64;
    private static final int MAX_ABSORB_TRAILS = 48;
    private static final int MAX_ONE_SHOTS = 64;
    private static final int MAX_TERRAIN_FACES = 4096;

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
            case GRAVITIC_ZONE -> {
                if (payload.active()) {
                    GraviticZone zone = GRAVITIC_ZONES.get(payload.sourceEntityId());
                    if (zone == null) {
                        GRAVITIC_ZONES.put(
                                payload.sourceEntityId(),
                                new GraviticZone(payload.durationTicks(), payload.position(), payload.primaryValue()));
                    } else {
                        zone.refresh(payload.durationTicks(), payload.position(), payload.primaryValue());
                    }
                } else {
                    GRAVITIC_ZONES.remove(payload.sourceEntityId());
                }
            }
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
            case KINETIC_GRASP_FIELD -> putOrRemove(
                    KINETIC_GRASP_FIELDS, payload.sourceEntityId(),
                    new Timed(payload.durationTicks()),
                    payload.active());
            case GRASP_TETHER -> putOrRemove(
                    GRASP_TETHERS, payload.sourceEntityId(),
                    new TimedTargetPos(payload.durationTicks(), payload.targetEntityId(), payload.position()),
                    payload.active());
            case FLUX_TARGET -> putOrRemoveCapped(
                    FLUX_TARGETS, payload.targetEntityId(),
                    new TimedFloat(payload.durationTicks(), payload.primaryValue()),
                    payload.active(), MAX_TRACKED_TARGETS);
            case BIOTIC_SPRAY_CONTACT -> putOrRemoveCapped(
                    BIOTIC_SPRAY_CONTACTS, pairKey(payload.sourceEntityId(), payload.targetEntityId()),
                    new TimedPair(payload.durationTicks(), payload.sourceEntityId(), payload.targetEntityId()),
                    payload.active(), MAX_CONTACT_EFFECTS);
            case COALESCENCE_CONTACT -> putOrRemoveCapped(
                    COALESCENCE_CONTACTS, pairKey(payload.sourceEntityId(), payload.targetEntityId()),
                    new TimedFloatPair(
                            payload.durationTicks(), payload.sourceEntityId(), payload.targetEntityId(), payload.primaryValue()),
                    payload.active(), MAX_CONTACT_EFFECTS);
            case KINETIC_GRASP_ABSORB -> addAbsorbTrail(payload);
            case BIOTIC_ORB_BOUNCE, BIOTIC_ORB_BURST -> addOneShot(payload);
            case FLUX_TARGET_IMPACT -> {
                addOneShot(payload);
                ClientGravityLensing.addPulse(
                        payload.position(), Math.max(0.5f, payload.primaryValue()) * 0.75f, 0.32f, 9);
            }
            case FLUX_FIELD -> {
                if ((int) payload.primaryValue() == 3) {
                    // Slam: one-shot lensing pulse at the zone center
                    ClientGravityLensing.addPulse(
                            payload.position(), Math.max(1.0f, payload.secondaryValue()) * 0.8f, 0.45f, 10);
                } else {
                    putOrRemove(
                            FLUX_FIELDS, payload.sourceEntityId(),
                            new FluxField(payload.durationTicks(), (int) payload.primaryValue(), payload.position(), payload.secondaryValue()),
                            payload.active());
                }
            }
            case HYPERSPHERE_PULSE -> {
                ClientGravityLensing.addPulse(
                        payload.position(), Math.max(0.5f, payload.primaryValue()), 0.65f,
                        Math.max(1, payload.durationTicks()));
                shakeFromImpact(payload.position(), 0.2f);
            }
            case FLUX_SLAM_RING -> {
                addOneShot(payload);
                shakeFromImpact(payload.position(), 0.5f);
            }
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
        tickMap(GRAVITIC_ZONES);
        tickMap(EXTRACTION_BEAMS);
        tickMap(ORB_TETHERS);
        tickMap(SEDATED);
        tickMap(NANO_SURGE);
        tickMap(COALESCENCE_BEAMS);
        tickMap(FADING);
        tickMap(KINETIC_GRASP_FIELDS);
        tickMap(GRASP_TETHERS);
        tickMap(BIOTIC_SPRAY_CONTACTS);
        tickMap(COALESCENCE_CONTACTS);
        tickMap(FLUX_FIELDS);
        tickMap(FLUX_TARGETS);
        ABSORB_TRAILS.removeIf(trail -> clientTicks - trail.startTick >= trail.duration);
        ONE_SHOTS.removeIf(shot -> clientTicks - shot.startTick >= shot.duration);

        SEDATED.keySet().forEach(id -> renderSleepZ(level, id));
        spawnFlightTrails(level);
        spawnGraspMotes(level);
        spawnSprayMotes(level);
    }

    public static void renderWorld(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (ROCKET_CHARGES.isEmpty()
                && METEOR_TARGETS.isEmpty()
                && GRAVITIC_ZONES.isEmpty()
                && EXTRACTION_BEAMS.isEmpty()
                && ORB_TETHERS.isEmpty()
                && NANO_SURGE.isEmpty()
                && COALESCENCE_BEAMS.isEmpty()
                && KINETIC_GRASP_FIELDS.isEmpty()
                && GRASP_TETHERS.isEmpty()
                && BIOTIC_SPRAY_CONTACTS.isEmpty()
                && COALESCENCE_CONTACTS.isEmpty()
                && FLUX_FIELDS.isEmpty()
                && FLUX_TARGETS.isEmpty()
                && ABSORB_TRAILS.isEmpty()
                && ONE_SHOTS.isEmpty()
                && level.players().stream().noneMatch(
                        p -> ClientGauntletAnimations.getActivePoseType(p.getId()) == GauntletPoseType.SPRAY_CHANNEL))
            return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(GAUNTLET_GLOW.bufferSize());
        ByteBufferBuilder energyBytes = new ByteBufferBuilder(GauntletVfx.GAUNTLET_ENERGY.bufferSize());
        ByteBufferBuilder bioticBytes = new ByteBufferBuilder(GauntletVfx.GAUNTLET_BIOTIC.bufferSize());
        ByteBufferBuilder gravityBytes = new ByteBufferBuilder(GauntletVfx.GAUNTLET_GRAVITY.bufferSize());
        ByteBufferBuilder darkBytes = new ByteBufferBuilder(GauntletVfx.GAUNTLET_DARK.bufferSize());
        poseStack.pushPose();
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            Matrix4fc matrix = poseStack.last().pose();
            DrawState drawState = new DrawState(new BufferBuilder(byteBuffer, GAUNTLET_GLOW.mode(), GAUNTLET_GLOW.format()), matrix, camera);
            DrawState energyDraw = new DrawState(
                    new BufferBuilder(energyBytes, GauntletVfx.GAUNTLET_ENERGY.mode(), GauntletVfx.GAUNTLET_ENERGY.format()),
                    matrix, camera);
            DrawState bioticDraw = new DrawState(
                    new BufferBuilder(bioticBytes, GauntletVfx.GAUNTLET_BIOTIC.mode(), GauntletVfx.GAUNTLET_BIOTIC.format()),
                    matrix, camera);
            DrawState gravityDraw = new DrawState(
                    new BufferBuilder(gravityBytes, GauntletVfx.GAUNTLET_GRAVITY.mode(), GauntletVfx.GAUNTLET_GRAVITY.format()),
                    matrix, camera);
            DrawState darkDraw = new DrawState(
                    new BufferBuilder(darkBytes, GauntletVfx.GAUNTLET_DARK.mode(), GauntletVfx.GAUNTLET_DARK.format()),
                    matrix, camera);

            ROCKET_CHARGES.forEach((id, effect) -> renderRocketCharge(level, drawState, id, effect.value));
            METEOR_TARGETS.values().forEach(effect -> renderMeteorTarget(level, drawState, effect));
            GRAVITIC_ZONES.values().forEach(effect -> renderGraviticZone(level, drawState, gravityDraw, effect));
            EXTRACTION_BEAMS.values().forEach(effect -> renderExtractionBeam(level, drawState, effect.sourceId, effect.targetId));
            ORB_TETHERS.values().forEach(effect -> renderOrbTether(level, drawState, bioticDraw, effect));
            NANO_SURGE.keySet().forEach(id -> renderNanoSurge(level, drawState, id));
            COALESCENCE_BEAMS.keySet().forEach(id -> renderCoalescenceBeam(level, drawState, bioticDraw, id));
            GRASP_TETHERS.forEach((id, effect) -> renderGraspBeam(level, drawState, bioticDraw, id, effect));
            renderSprayCones(level, bioticDraw);
            BIOTIC_SPRAY_CONTACTS.values().forEach(effect -> renderBioticContact(level, drawState, bioticDraw, effect, true));
            COALESCENCE_CONTACTS.values().forEach(effect -> renderCoalescenceContact(level, drawState, bioticDraw, effect));
            KINETIC_GRASP_FIELDS.keySet().forEach(
                    id -> renderKineticGraspVortex(level, drawState, gravityDraw, darkDraw, id));
            FLUX_FIELDS.values().forEach(effect -> renderFluxField(level, drawState, gravityDraw, effect));
            FLUX_TARGETS.forEach((id, effect) -> renderFluxTarget(level, drawState, gravityDraw, id, effect.value));
            ABSORB_TRAILS.forEach(trail -> renderAbsorbTrail(level, drawState, gravityDraw, trail));
            ONE_SHOTS.forEach(shot -> renderOneShot(drawState, bioticDraw, gravityDraw, shot));

            // Additive halos are drawn first; the event horizon is composited last so the black
            // core stays genuinely dark instead of being washed into a glowing portal.
            MeshData mesh = drawState.buffer.build();
            if (drawState.hasVertices && mesh != null) {
                GAUNTLET_GLOW.draw(mesh);
            }
            MeshData energyMesh = energyDraw.buffer.build();
            if (energyDraw.hasVertices && energyMesh != null) {
                GauntletVfx.GAUNTLET_ENERGY.draw(energyMesh);
            }
            MeshData bioticMesh = bioticDraw.buffer.build();
            if (bioticDraw.hasVertices && bioticMesh != null) {
                GauntletVfx.GAUNTLET_BIOTIC.draw(bioticMesh);
            }
            MeshData gravityMesh = gravityDraw.buffer.build();
            if (gravityDraw.hasVertices && gravityMesh != null) {
                GauntletVfx.GAUNTLET_GRAVITY.draw(gravityMesh);
            }
            MeshData darkMesh = darkDraw.buffer.build();
            if (darkDraw.hasVertices && darkMesh != null) {
                GauntletVfx.GAUNTLET_DARK.draw(darkMesh);
            }
        } finally {
            poseStack.popPose();
            byteBuffer.close();
            energyBytes.close();
            bioticBytes.close();
            gravityBytes.close();
            darkBytes.close();
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

    public static boolean isCoalescenceBeamActive(int entityId) {
        return COALESCENCE_BEAMS.containsKey(entityId);
    }

    public static boolean isKineticGraspActive(int entityId) {
        return KINETIC_GRASP_FIELDS.containsKey(entityId);
    }

    public static boolean isGraviticFluxActive(int entityId) {
        return FLUX_FIELDS.containsKey(entityId) || GRAVITIC_ZONES.containsKey(entityId);
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
        GRAVITIC_ZONES.clear();
        EXTRACTION_BEAMS.clear();
        ORB_TETHERS.clear();
        SEDATED.clear();
        NANO_SURGE.clear();
        COALESCENCE_BEAMS.clear();
        FADING.clear();
        KINETIC_GRASP_FIELDS.clear();
        GRASP_TETHERS.clear();
        BIOTIC_SPRAY_CONTACTS.clear();
        COALESCENCE_CONTACTS.clear();
        FLUX_FIELDS.clear();
        FLUX_TARGETS.clear();
        ABSORB_TRAILS.clear();
        ONE_SHOTS.clear();
    }

    private static <K, V extends Timed> void putOrRemove(Map<K, V> map, K key, V value, boolean active) {
        if (active) map.put(key, value);
        else map.remove(key);
    }

    private static <K, V extends Timed> void putOrRemoveCapped(
            Map<K, V> map, K key, V value, boolean active, int maximumSize) {
        if (!active) {
            map.remove(key);
            return;
        }
        if (!map.containsKey(key) && map.size() >= maximumSize) {
            Iterator<K> oldest = map.keySet().iterator();
            if (oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
        map.put(key, value);
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
        if (ONE_SHOTS.size() >= MAX_ONE_SHOTS) ONE_SHOTS.removeFirst();
        ONE_SHOTS.add(new OneShot(
                payload.effectType(), payload.position(), payload.primaryValue(),
                clientTicks, Math.max(1, payload.durationTicks())));
    }

    private static void addAbsorbTrail(ClientboundGauntletEffectPayload payload) {
        for (AbsorbTrail trail : ABSORB_TRAILS) {
            if (trail.casterId == payload.sourceEntityId()
                    && trail.startTick == clientTicks
                    && trail.start.distanceToSqr(payload.position()) < 1.0E-6)
                return;
        }
        if (ABSORB_TRAILS.size() >= MAX_ABSORB_TRAILS) ABSORB_TRAILS.removeFirst();
        ABSORB_TRAILS.add(new AbsorbTrail(
                payload.sourceEntityId(), payload.position(), payload.primaryValue() > 0.5f,
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
        if (!withinEffectRange(draw, entity.position())) return;
        boolean detailed = withinDetailRange(draw, entity.position());

        charge = Math.clamp(charge, 0f, 1f);
        HandPlacement hand = rocketPunchHandPlacement(entity, draw.camera, charge);
        Vec3 look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        Vec3 right = look.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(look).normalize();
        int baseColor = chargeColor(charge);
        double pulse = 0.72 + 0.28 * Math.sin((clientTicks + charge * 18.0) * (0.25 + charge * 0.15));
        float radius = hand.scale * (0.72f + charge * 0.30f);

        int ringCount = detailed ? 4 : 2;
        for (int ring = 0; ring < ringCount; ring++) {
            Vec3 center = hand.position.subtract(look.scale(ring * hand.scale * 0.48));
            addEllipticalRingGlow(
                    draw, center, right, up,
                    radius * (1.0f - ring * 0.07f), radius * (0.78f - ring * 0.04f),
                    0.018f + charge * 0.012f,
                    ring == 0 ? mixColor(baseColor, 0xFFFFFF, 0.58) : baseColor,
                    (float) ((0.40 + charge * 0.35) * pulse), 24,
                    clientTicks * (0.07 + ring * 0.012));
        }

        int arcs = detailed ? 4 + (int) (charge * 5.0f) : 3;
        for (int arc = 0; arc < arcs; arc++) {
            double angle = arc * 2.399 + clientTicks * (0.13 + charge * 0.10);
            Vec3 start = hand.position.subtract(look.scale(0.42 + (arc % 3) * 0.11))
                    .add(right.scale(Math.cos(angle) * radius * 0.82))
                    .add(up.scale(Math.sin(angle) * radius * 0.62));
            Vec3 control = start.add(look.scale(0.24))
                    .add(right.scale(Math.sin(angle * 1.7) * 0.10));
            Vec3 previous = start;
            for (int i = 1; i <= 5; i++) {
                float t = i / 5.0f;
                Vec3 next = bezier(start, control, hand.position.add(look.scale(0.08)), t);
                addBeamSegment(
                        draw, previous, next, 0.012f + charge * 0.010f,
                        arc % 3 == 0 ? 0xE8FBFF : baseColor,
                        (float) ((0.58 + charge * 0.30) * pulse));
                previous = next;
            }
        }

        int moteCount = detailed ? 7 : 3;
        for (int i = 0; i < moteCount; i++) {
            double t = Math.floorMod(clientTicks * 3 + i * 7, 24) / 24.0;
            double angle = i * 2.399;
            Vec3 mote = hand.position.subtract(look.scale(0.16 + t * (0.70 + charge * 0.55)))
                    .add(right.scale(Math.cos(angle) * radius * (0.55 + t * 0.45)))
                    .add(up.scale(Math.sin(angle) * radius * 0.45));
            addBillboard(
                    draw, mote, 0.025f + charge * 0.018f,
                    i % 3 == 0 ? 0xE8FBFF : baseColor,
                    (float) ((0.40 + charge * 0.35) * (1.0 - t)), true);
        }
        addBillboard(
                draw, hand.position.add(look.scale(0.08)), radius * (0.40f + charge * 0.16f),
                mixColor(baseColor, 0xFFFFFF, 0.72), (float) ((0.20 + charge * 0.26) * pulse), true);
    }

    private static void renderMeteorTarget(ClientLevel level, DrawState draw, MeteorTarget effect) {
        if (!withinEffectRange(draw, effect.position)) return;
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
        if (!withinEffectRange(draw, source.position()) && !withinEffectRange(draw, target.position())) return;

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

    private static void renderOrbTether(
            ClientLevel level, DrawState glow, DrawState biotic, TimedOrbTether effect) {
        Entity orb = level.getEntity(effect.sourceId);
        Entity target = level.getEntity(effect.targetId);
        if (target == null) return;

        Vec3 orbCenter = orb != null
                ? orb.position().add(0, orb.getBbHeight() * 0.5, 0)
                : effect.orbFallback;
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.55, 0);
        if (!withinEffectRange(glow, orbCenter) && !withinEffectRange(glow, targetCenter)) return;
        boolean detailed = withinDetailRange(glow, orbCenter) || withinDetailRange(glow, targetCenter);
        // Damage energy must visibly travel from the victim back into the orb.
        Vec3 delta = orbCenter.subtract(targetCenter);
        double length = delta.length();
        if (length < 0.1) return;

        Vec3 direction = delta.normalize();
        Vec3 right = direction.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(direction).normalize();
        int steps = detailed
                ? Math.max(10, Math.min(24, (int) (length * 3.0)))
                : Math.max(7, Math.min(14, (int) (length * 1.7)));
        int strandCount = detailed ? 3 : 2;
        for (int strand = 0; strand < strandCount; strand++) {
            double phase = clientTicks * (0.16 + strand * 0.025) + effect.targetId * 0.41 + strand * 2.1;
            Vec3 control = targetCenter
                    .add(delta.scale(0.48))
                    .add(right.scale(Math.sin(phase) * (0.18 + strand * 0.07)))
                    .add(up.scale(Math.cos(phase * 0.83) * (0.14 + strand * 0.05)));
            Vec3 previous = targetCenter;
            for (int i = 1; i <= steps; i++) {
                float t1 = (float) i / steps;
                float t0 = (float) (i - 1) / steps;
                Vec3 next = bezier(targetCenter, control, orbCenter, t1);
                float width = (0.035f + strand * 0.012f) * (0.55f + 0.45f * (float) Math.sin(Math.PI * t1));
                int color = strand == 1 ? GauntletVfx.MOIRA_PALE : GauntletVfx.MOIRA_PURPLE;
                addBeamSegmentUV(biotic, previous, next, width, color, strand == 1 ? 0.78f : 0.52f, t0, t1);
                previous = next;
            }
        }
        addBioticBurst(glow, targetCenter, 0.30f, GauntletVfx.MOIRA_PURPLE, 0.55f, effect.targetId);
        addBioticBurst(glow, orbCenter, 0.18f, GauntletVfx.MOIRA_PALE, 0.65f, effect.sourceId);
    }

    private static void renderNanoSurge(ClientLevel level, DrawState draw, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        if (!withinEffectRange(draw, entity.position())) return;

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

    private static void renderCoalescenceBeam(ClientLevel level, DrawState glow, DrawState biotic, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        if (!withinEffectRange(glow, entity.position())) return;
        boolean detailed = withinDetailRange(glow, entity.position());

        Vec3 direction = entity.getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0E-6) direction = new Vec3(0, 0, 1);
        Vec3 origin = entity.getEyePosition().add(direction.scale(0.5)).subtract(0, 0.45, 0);
        double length = OLRUConfig.FINAL_ANSWER.COALESCENCE.length.get();

        Vec3 axisRight = direction.cross(UP).normalize();
        if (axisRight.lengthSqr() < 1.0E-6) axisRight = new Vec3(1, 0, 0);
        Vec3 axisUp = axisRight.cross(direction).normalize();
        HandPlacement leftHand = bioticHandPlacement(entity, glow.camera, -1, 0.12f);
        HandPlacement rightHand = bioticHandPlacement(entity, glow.camera, 1, 0.12f);
        double mergeDistance = 0.72;
        Vec3 merge = origin.add(direction.scale(mergeDistance));
        renderConvergingBioticStream(biotic, leftHand.position, merge, GauntletVfx.MOIRA_GOLD, -1.0);
        renderConvergingBioticStream(biotic, rightHand.position, merge, GauntletVfx.MOIRA_PURPLE, 1.0);

        int steps = detailed
                ? Math.max(30, Math.min(52, (int) (length * 2.5)))
                : Math.max(18, Math.min(30, (int) (length * 1.35)));
        double beamLength = Math.max(0.5, length - mergeDistance);
        Vec3[] axisPoints = new Vec3[steps + 1];
        Vec3[] goldPoints = new Vec3[steps + 1];
        Vec3[] purplePoints = new Vec3[steps + 1];
        float[] coreWidths = new float[steps + 1];
        float[] helixWidths = new float[steps + 1];
        float[] sheathWidths = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float entry = Math.clamp(t / 0.055f, 0f, 1f);
            float endFade = Math.clamp((1.0f - t) / 0.12f, 0f, 1f);
            float envelope = entry * endFade;
            double angle = clientTicks * 0.28 + t * Math.PI * 10.0;
            double helixRadius = (0.14 + 0.06 * Math.sin(t * Math.PI)) * envelope;
            Vec3 axis = merge.add(direction.scale(beamLength * t));
            axisPoints[i] = axis;
            goldPoints[i] = axis.add(axisRight.scale(Math.cos(angle) * helixRadius))
                    .add(axisUp.scale(Math.sin(angle) * helixRadius));
            purplePoints[i] = axis.add(axisRight.scale(Math.cos(angle + Math.PI) * helixRadius))
                    .add(axisUp.scale(Math.sin(angle + Math.PI) * helixRadius));
            helixWidths[i] = 0.15f * Math.max(0.07f, envelope)
                    * (0.86f + 0.14f * (float) Math.sin(t * Math.PI));
            coreWidths[i] = 0.12f * Math.max(0.07f, endFade);
            sheathWidths[i] = 0.40f * Math.max(0.05f, endFade)
                    * (0.82f + 0.18f * (float) Math.sin(t * Math.PI * 2.0 + clientTicks * 0.08));
        }
        // Each layer is one continuous ribbon with shared edge vertices. This removes the visible
        // rectangular seams produced when every short segment independently faced the camera.
        addSmoothRibbonUV(
                biotic, axisPoints, sheathWidths,
                GauntletVfx.mix(GauntletVfx.MOIRA_GOLD, GauntletVfx.MOIRA_PURPLE, 0.48f), 0.16f);
        addSmoothRibbonUV(biotic, goldPoints, helixWidths, GauntletVfx.MOIRA_GOLD, 0.72f);
        addSmoothRibbonUV(biotic, purplePoints, helixWidths, GauntletVfx.MOIRA_PURPLE, 0.76f);
        addSmoothRibbonUV(biotic, axisPoints, coreWidths, GauntletVfx.MOIRA_PALE, 0.92f);

        int moteCount = detailed ? 12 : 4;
        for (int i = 0; i < moteCount; i++) {
            double t = (i + 0.35 + 0.18 * Math.sin(clientTicks * 0.21 + i)) / moteCount;
            double angle = i * 2.399 + clientTicks * 0.13;
            Vec3 mote = origin.add(direction.scale(length * t))
                    .add(axisRight.scale(Math.cos(angle) * 0.42))
                    .add(axisUp.scale(Math.sin(angle) * 0.42));
            addBillboardUV(
                    biotic, mote, 0.055f + (i % 3) * 0.012f,
                    i % 2 == 0 ? GauntletVfx.MOIRA_GOLD : GauntletVfx.MOIRA_PURPLE, 0.55f);
        }
        addBioticBurst(glow, merge, 0.34f, GauntletVfx.MOIRA_PALE, 0.72f, entityId);
    }

    /** Biotic Grasp: three target-to-palm siphon filaments with a moving, tracked endpoint. */
    private static void renderGraspBeam(
            ClientLevel level, DrawState glow, DrawState biotic, int entityId, TimedTargetPos effect) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        Entity target = level.getEntity(effect.targetId);
        Vec3 hit = target != null
                ? target.position().add(0, target.getBbHeight() * 0.52, 0)
                : effect.pos;
        if (!withinEffectRange(glow, entity.position()) && !withinEffectRange(glow, hit)) return;
        boolean detailed = withinDetailRange(glow, entity.position()) || withinDetailRange(glow, hit);
        Vec3 origin = bioticHandPlacement(entity, glow.camera, 1, 0.10f).position;
        Vec3 delta = origin.subtract(hit);
        double length = delta.length();
        if (length < 0.15) return;
        Vec3 dir = delta.normalize();
        Vec3 right = dir.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(dir).normalize();

        int points = detailed
                ? Math.max(14, Math.min(26, (int) (length * 3.5)))
                : Math.max(8, Math.min(14, (int) (length * 1.8)));
        int strandCount = detailed ? 3 : 2;
        for (int strand = 0; strand < strandCount; strand++) {
            double phase = clientTicks * (0.19 + strand * 0.025) + entityId * 0.73 + strand * 2.2;
            Vec3 control = hit.add(delta.scale(0.50))
                    .add(right.scale(Math.sin(phase) * (0.18 + length * 0.025)))
                    .add(up.scale(Math.cos(phase * 0.77) * (0.12 + strand * 0.04)));
            Vec3 previous = hit;
            for (int i = 1; i <= points; i++) {
                float t1 = (float) i / points;
                float t0 = (float) (i - 1) / points;
                Vec3 next = bezier(hit, control, origin, t1);
                float width = (strand == 1 ? 0.075f : 0.045f)
                        * (0.55f + 0.45f * (float) Math.sin(Math.PI * t1));
                int color = strand == 1
                        ? GauntletVfx.mix(GauntletVfx.MOIRA_PALE, GauntletVfx.MOIRA_PURPLE, 0.28f)
                        : GauntletVfx.MOIRA_DEEP;
                addBeamSegmentUV(biotic, previous, next, width, color, strand == 1 ? 0.90f : 0.58f, t0, t1);
                previous = next;
            }
        }

        addBillboardUV(biotic, hit, 0.30f, GauntletVfx.MOIRA_DEEP, 0.32f);
        addBillboardUV(biotic, origin, 0.16f, GauntletVfx.MOIRA_PURPLE, 0.78f);
        addBillboard(
                glow, origin, 0.065f,
                GauntletVfx.MOIRA_PALE, 0.88f, true);
        addBioticBurst(glow, hit, 0.33f, GauntletVfx.MOIRA_DEEP, 0.60f, effect.targetId);
        addBioticBurst(glow, origin, 0.20f, GauntletVfx.MOIRA_PALE, 0.72f, entityId);
    }

    private static void renderConvergingBioticStream(
            DrawState biotic, Vec3 start, Vec3 end, int color, double bendSign) {
        Vec3 delta = end.subtract(start);
        Vec3 side = delta.normalize().cross(UP).normalize();
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        Vec3 control = start.add(delta.scale(0.55)).add(side.scale(0.18 * bendSign)).add(0, 0.12, 0);
        Vec3[] points = new Vec3[9];
        float[] widths = new float[9];
        for (int i = 0; i < points.length; i++) {
            float t = (float) i / (points.length - 1);
            points[i] = bezier(start, control, end, t);
            widths[i] = 0.075f + 0.035f * (float) Math.sin(Math.PI * t);
        }
        addSmoothRibbonUV(biotic, points, widths, color, 0.78f);
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, double t) {
        double u = 1.0 - t;
        return p0.scale(u * u).add(p1.scale(2 * u * t)).add(p2.scale(t * t));
    }

    /** Biotic Spray: six hand-bound liquid ribbons surrounded by dissolving golden mist. */
    private static void renderSprayCones(ClientLevel level, DrawState biotic) {
        for (Player player : level.players()) {
            if (withinEffectRange(biotic, player.position())
                    && ClientGauntletAnimations.getActivePoseType(player.getId()) == GauntletPoseType.SPRAY_CHANNEL) {
                renderSprayCone(player, biotic);
            }
        }
    }

    private static void renderSprayCone(Player player, DrawState biotic) {
        boolean detailed = withinDetailRange(biotic, player.position());
        Vec3 direction = player.getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0E-6) direction = new Vec3(0, 0, 1);
        Vec3 origin = bioticHandPlacement(player, biotic.camera, -1, 0.08f).position;
        Vec3 right = direction.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(direction).normalize();
        double range = OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY.range.get();
        double slope = Math.tan(Math.toRadians(OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY.coneAngleDegrees.get() * 0.5)) * 0.8;

        int strandCount = detailed ? 6 : 3;
        int strandSteps = detailed ? 15 : 9;
        for (int strand = 0; strand < strandCount; strand++) {
            double angle = strand * Math.PI * 2.0 / strandCount + Math.sin(clientTicks * 0.045 + strand) * 0.18;
            Vec3 spread = right.scale(Math.cos(angle) * range * slope * 0.68)
                    .add(up.scale(Math.sin(angle) * range * slope * 0.45));
            Vec3 end = origin.add(direction.scale(range)).add(spread);
            Vec3 control = origin.add(direction.scale(range * 0.48))
                    .add(spread.scale(0.25))
                    .add(right.scale(Math.sin(clientTicks * 0.12 + strand * 1.7) * 0.12));
            Vec3[] points = new Vec3[strandSteps + 1];
            float[] widths = new float[strandSteps + 1];
            for (int i = 0; i <= strandSteps; i++) {
                float t = (float) i / strandSteps;
                points[i] = bezier(origin, control, end, t);
                widths[i] = 0.040f + 0.16f * t;
            }
            int color = strand % 3 == 0
                    ? GauntletVfx.mix(GauntletVfx.MOIRA_GOLD, 0xFFFFFF, 0.32f)
                    : GauntletVfx.MOIRA_GOLD;
            addSmoothRibbonUV(biotic, points, widths, color, 0.56f);
        }

        // Low-opacity interstitial streams overlap the six hero ribbons. They close the large
        // triangular holes in the cone without turning it back into one flat translucent sheet.
        int fillCount = detailed ? 6 : 3;
        int fillSteps = detailed ? 12 : 7;
        for (int fill = 0; fill < fillCount; fill++) {
            double angle = (fill + 0.5) * Math.PI * 2.0 / fillCount
                    + Math.sin(clientTicks * 0.038 + fill * 1.3) * 0.12;
            Vec3 spread = right.scale(Math.cos(angle) * range * slope * 0.52)
                    .add(up.scale(Math.sin(angle) * range * slope * 0.34));
            Vec3 end = origin.add(direction.scale(range * 0.94)).add(spread);
            Vec3 control = origin.add(direction.scale(range * 0.50))
                    .add(spread.scale(0.20))
                    .add(up.scale(Math.sin(clientTicks * 0.07 + fill) * 0.08));
            Vec3[] points = new Vec3[fillSteps + 1];
            float[] widths = new float[fillSteps + 1];
            for (int i = 0; i <= fillSteps; i++) {
                float t = (float) i / fillSteps;
                points[i] = bezier(origin, control, end, t);
                widths[i] = 0.055f + 0.15f * t;
            }
            addSmoothRibbonUV(biotic, points, widths, GauntletVfx.MOIRA_GOLD, 0.18f);
        }

        int mistCount = detailed ? 16 : 6;
        for (int i = 0; i < mistCount; i++) {
            double t = (i + 0.55) / (mistCount + 0.5);
            double angle = i * 2.399 + clientTicks * 0.055;
            double radial = range * t * slope * (0.22 + 0.24 * Math.sin(i * 1.7 + clientTicks * 0.08));
            Vec3 center = origin.add(direction.scale(range * t))
                    .add(right.scale(Math.cos(angle) * radial))
                    .add(up.scale(Math.sin(angle) * radial * 0.65));
            float size = (float) (0.12 + t * range * slope * 0.36);
            float alpha = (float) (0.12 + 0.18 * Math.sin(Math.PI * t));
            addBillboardUV(biotic, center, size, GauntletVfx.MOIRA_GOLD, alpha);
            if (i % 2 == 0) {
                Vec3 voxel = center.add(right.scale(0.08 * Math.sin(clientTicks * 0.17 + i)));
                addBillboardUV(
                        biotic, voxel, 0.045f + i * 0.002f,
                        GauntletVfx.mix(GauntletVfx.MOIRA_GOLD, 0xFFFFFF, 0.45f), 0.60f);
            }
        }
    }

    /** Drain motes streaming from the drained target back to the caster — the life-steal direction cue. */
    private static void renderBioticContact(
            ClientLevel level,
            DrawState glow,
            DrawState biotic,
            TimedPair effect,
            boolean healing) {
        Entity target = level.getEntity(effect.targetId);
        if (target == null) return;
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.52, 0);
        if (!withinEffectRange(glow, center)) return;
        boolean detailed = withinDetailRange(glow, center);
        double radius = Math.max(0.24, target.getBbWidth() * 0.62);
        int color = healing ? GauntletVfx.MOIRA_GOLD : GauntletVfx.MOIRA_PURPLE;
        int strandCount = detailed ? 3 : 1;
        int strandSteps = detailed ? 12 : 7;
        for (int strand = 0; strand < strandCount; strand++) {
            Vec3 previous = null;
            for (int i = 0; i <= strandSteps; i++) {
                double t = (double) i / strandSteps;
                double angle = clientTicks * (healing ? 0.18 : -0.22)
                        + strand * Math.PI * 2.0 / strandCount
                        + t * Math.PI * 2.2;
                Vec3 point = center.add(
                        Math.cos(angle) * radius,
                        (t - 0.5) * target.getBbHeight() * 0.72,
                        Math.sin(angle) * radius);
                if (previous != null) {
                    addBeamSegmentUV(
                            biotic, previous, point, 0.025f,
                            strand == 1 ? GauntletVfx.mix(color, 0xFFFFFF, 0.45f) : color,
                            healing ? 0.55f : 0.48f,
                            (float) (t - 1.0 / strandSteps), (float) t);
                }
                previous = point;
            }
        }
        addBioticBurst(glow, center, (float) radius * 0.72f, color, healing ? 0.42f : 0.52f, effect.targetId);
    }

    private static void renderCoalescenceContact(
            ClientLevel level, DrawState glow, DrawState biotic, TimedFloatPair effect) {
        renderBioticContact(level, glow, biotic, effect, effect.value > 0f);
    }

    private static void addBioticBurst(
            DrawState glow, Vec3 center, float radius, int color, float alpha, int seed) {
        for (int i = 0; i < 7; i++) {
            double angle = i * 2.399 + seed * 0.37 + clientTicks * 0.08;
            double lift = Math.sin(angle * 1.7 + seed) * radius * 0.42;
            Vec3 point = center.add(Math.cos(angle) * radius, lift, Math.sin(angle) * radius);
            float size = 0.032f + (i % 3) * 0.012f;
            addBillboard(
                    glow, point, size,
                    i % 3 == 0 ? mixColor(color, 0xFFFFFF, 0.55) : color,
                    alpha, true);
        }
    }

    private static void spawnGraspMotes(ClientLevel level) {
        for (Map.Entry<Integer, TimedTargetPos> entry : GRASP_TETHERS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) continue;
            Entity target = level.getEntity(entry.getValue().targetId);
            Vec3 hit = target != null
                    ? target.position().add(0, target.getBbHeight() * 0.52, 0)
                    : entry.getValue().pos;
            if (!withinLocalDetailRange(hit)) continue;
            Vec3 origin = entity.getEyePosition().add(entity.getLookAngle().scale(0.35)).subtract(0, 0.4, 0);
            Vec3 pull = origin.subtract(hit);
            double length = pull.length();
            if (length < 0.3) continue;
            Vec3 velocity = pull.normalize().scale(0.45);
            var random = level.getRandom();
            Vec3 spawn = hit.add(
                    (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.7);
            level.addParticle(
                    new DustParticleOptions(GauntletVfx.MOIRA_PURPLE, 0.65f),
                    spawn.x, spawn.y, spawn.z, velocity.x, velocity.y, velocity.z);
        }
    }

    /** Sparse forward-drifting gold motes inside the spray cone, complementing the server-side dust. */
    private static void spawnSprayMotes(ClientLevel level) {
        if (clientTicks % 2 != 0) return;
        for (Player player : level.players()) {
            if (ClientGauntletAnimations.getActivePoseType(player.getId()) != GauntletPoseType.SPRAY_CHANNEL) continue;
            if (!withinLocalDetailRange(player.position())) continue;
            Vec3 direction = player.getLookAngle().normalize();
            if (direction.lengthSqr() < 1.0E-6) direction = new Vec3(0, 0, 1);
            var random = level.getRandom();
            double range = OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY.range.get();
            double dist = 0.6 + random.nextDouble() * range * 0.5;
            double spread = dist * 0.12;
            Vec3 pos = player.getEyePosition()
                    .add(direction.scale(dist))
                    .subtract(0, 0.4, 0)
                    .add(random.nextGaussian() * spread, random.nextGaussian() * spread * 0.6, random.nextGaussian() * spread);
            level.addParticle(
                    new DustParticleOptions(GauntletVfx.MOIRA_GOLD, 0.6f), pos.x, pos.y, pos.z,
                    direction.x * 0.25, direction.y * 0.25 + 0.03, direction.z * 0.25);
        }
    }

    /**
     * Hand-bound black hole: an opaque event horizon, a narrow photon ring, a tilted lopsided
     * accretion disk and only a few dim infall wisps. Screen-space lensing supplies the main sense
     * of gravity; geometry stays restrained so it reads as a black hole rather than a portal.
     */
    private static void renderKineticGraspVortex(
            ClientLevel level, DrawState glow, DrawState gravity, DrawState dark, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;
        Vec3 anchor = graspAnchor(entity);
        if (!withinEffectRange(glow, anchor)) return;
        boolean detailed = withinDetailRange(glow, anchor);
        Vec3 look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        Vec3 right = look.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(look).normalize();
        double pulse = 0.88 + 0.12 * Math.sin(clientTicks * 0.21 + entityId * 0.7);
        double tilt = 0.52 + 0.06 * Math.sin(clientTicks * 0.025 + entityId);
        Vec3 diskRight = right;
        Vec3 diskUp = up.scale(Math.cos(tilt)).add(look.scale(Math.sin(tilt))).normalize();
        double spin = -clientTicks * 0.035 + entityId * 0.43;

        // Dim, broad material orbit plus a brighter inner stream. Their directional brightness
        // gives the disk the Doppler-lopsided silhouette associated with a black hole.
        addAccretionRingGlow(
                glow, anchor, diskRight, diskUp,
                0.38f, 0.125f, 0.040f,
                GauntletVfx.SIGMA_DEEP, (float) (0.18 * pulse), detailed ? 64 : 36, spin);
        addAccretionRingGlow(
                glow, anchor.add(look.scale(0.006)), diskRight, diskUp,
                0.30f, 0.090f, 0.025f,
                GauntletVfx.SIGMA_VIOLET, (float) (0.34 * pulse), detailed ? 56 : 32, spin + 0.65);

        // The photon ring remains almost circular around the event horizon and is intentionally
        // narrow; no stacked concentric portal rings.
        addAccretionRingGlow(
                glow, anchor.add(look.scale(0.012)), right, up,
                0.205f, 0.174f, 0.012f,
                GauntletVfx.SIGMA_PALE, (float) (0.62 * pulse), detailed ? 56 : 32, spin * 0.35);
        addAccretionRingGlow(
                glow, anchor.add(look.scale(0.009)), right, up,
                0.225f, 0.192f, 0.010f,
                GauntletVfx.SIGMA_VIOLET, (float) (0.22 * pulse), detailed ? 48 : 28, -spin * 0.24);

        int wispCount = detailed ? 3 : 1;
        int wispSteps = detailed ? 9 : 6;
        for (int wisp = 0; wisp < wispCount; wisp++) {
            Vec3[] points = new Vec3[wispSteps + 1];
            float[] widths = new float[wispSteps + 1];
            for (int i = 0; i <= wispSteps; i++) {
                float t = (float) i / wispSteps;
                double radius = 0.54 - t * 0.32;
                double angle = wisp * Math.PI * 2.0 / wispCount - clientTicks * 0.045 - t * 2.6;
                points[i] = anchor
                        .add(diskRight.scale(Math.cos(angle) * radius))
                        .add(diskUp.scale(Math.sin(angle) * radius * 0.43))
                        .subtract(look.scale((1.0 - t) * 0.16));
                widths[i] = 0.016f - t * 0.006f;
            }
            addSmoothRibbonUV(
                    gravity, points, widths,
                    wisp == 0 ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_VIOLET, 0.20f);
        }

        int moteCount = detailed ? 6 : 2;
        for (int i = 0; i < moteCount; i++) {
            double inward = Math.floorMod(clientTicks * 2 + i * 7, 24) / 24.0;
            double angle = i * 2.399 - clientTicks * 0.055 - inward * 1.7;
            double radius = 0.46 - inward * 0.23;
            Vec3 mote = anchor.add(diskRight.scale(Math.cos(angle) * radius))
                    .add(diskUp.scale(Math.sin(angle) * radius * 0.42));
            addBillboard(
                    glow, mote, 0.022f + (i % 2) * 0.010f,
                    i % 3 == 0 ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_VIOLET,
                    0.38f, true);
        }

        // Submitted to the alpha-blended dark pass and composited after every glow layer.
        addEllipticalDisc(
                dark, anchor.add(look.scale(0.015)), right, up,
                GRASP_HORIZON_RADIUS * 1.22f, GRASP_HORIZON_RADIUS, 0.995f,
                detailed);
    }

    /** Black-hole anchor: ahead of the eyes, offset down and to the main-hand side so it never blocks the view. */
    private static Vec3 graspAnchor(Entity entity) {
        Vec3 look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        Vec3 right = look.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(look).normalize();
        return entity.getEyePosition()
                .add(look.scale(GRASP_ANCHOR_DISTANCE))
                .add(right.scale(GRASP_ANCHOR_SIDE * mainHandSide(entity)))
                .subtract(up.scale(GRASP_ANCHOR_DOWN));
    }

    private static void addEllipticalDisc(
            DrawState draw,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            float radiusX,
            float radiusY,
            float alpha,
            boolean detailed) {
        int rings = detailed ? 4 : 2;
        int segments = detailed ? 36 : 20;
        for (int ring = 0; ring < rings; ring++) {
            float innerScale = (float) ring / rings;
            float outerScale = (float) (ring + 1) / rings;
            float innerAlpha = alpha * (1.0f - innerScale * 0.18f);
            float outerAlpha = alpha * (1.0f - outerScale * 0.18f);
            for (int i = 0; i < segments; i++) {
                double a0 = i * Math.PI * 2.0 / segments;
                double a1 = (i + 1) * Math.PI * 2.0 / segments;
                Vec3 p0 = center.add(right.scale(Math.cos(a0) * radiusX * innerScale))
                        .add(up.scale(Math.sin(a0) * radiusY * innerScale));
                Vec3 p1 = center.add(right.scale(Math.cos(a1) * radiusX * innerScale))
                        .add(up.scale(Math.sin(a1) * radiusY * innerScale));
                Vec3 p2 = center.add(right.scale(Math.cos(a1) * radiusX * outerScale))
                        .add(up.scale(Math.sin(a1) * radiusY * outerScale));
                Vec3 p3 = center.add(right.scale(Math.cos(a0) * radiusX * outerScale))
                        .add(up.scale(Math.sin(a0) * radiusY * outerScale));
                addQuadVA(draw, p0, p1, p2, p3, GauntletVfx.SIGMA_CORE, innerAlpha, outerAlpha);
            }
        }
    }

    private static void addEllipticalRingUV(
            DrawState draw,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            float radiusX,
            float radiusY,
            float width,
            int color,
            float alpha,
            int segments,
            double rotation) {
        for (int i = 0; i < segments; i++) {
            double a0 = rotation + i * Math.PI * 2.0 / segments;
            double a1 = rotation + (i + 1) * Math.PI * 2.0 / segments;
            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;
            Vec3 p0 = center.add(right.scale(Math.cos(a0) * (radiusX - width)))
                    .add(up.scale(Math.sin(a0) * (radiusY - width)));
            Vec3 p1 = center.add(right.scale(Math.cos(a0) * (radiusX + width)))
                    .add(up.scale(Math.sin(a0) * (radiusY + width)));
            Vec3 p2 = center.add(right.scale(Math.cos(a1) * (radiusX + width)))
                    .add(up.scale(Math.sin(a1) * (radiusY + width)));
            Vec3 p3 = center.add(right.scale(Math.cos(a1) * (radiusX - width)))
                    .add(up.scale(Math.sin(a1) * (radiusY - width)));
            addQuadUV(draw, p0, p1, p2, p3, color, alpha, u0, u1);
        }
    }

    private static void addEllipticalRingGlow(
            DrawState draw,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            float radiusX,
            float radiusY,
            float width,
            int color,
            float alpha,
            int segments,
            double rotation) {
        for (int i = 0; i < segments; i++) {
            if ((i + (int) (rotation * 4.0)) % 7 == 0) continue;
            double a0 = rotation + i * Math.PI * 2.0 / segments;
            double a1 = rotation + (i + 1) * Math.PI * 2.0 / segments;
            Vec3 p0 = center.add(right.scale(Math.cos(a0) * (radiusX - width)))
                    .add(up.scale(Math.sin(a0) * (radiusY - width)));
            Vec3 p1 = center.add(right.scale(Math.cos(a0) * (radiusX + width)))
                    .add(up.scale(Math.sin(a0) * (radiusY + width)));
            Vec3 p2 = center.add(right.scale(Math.cos(a1) * (radiusX + width)))
                    .add(up.scale(Math.sin(a1) * (radiusY + width)));
            Vec3 p3 = center.add(right.scale(Math.cos(a1) * (radiusX - width)))
                    .add(up.scale(Math.sin(a1) * (radiusY - width)));
            addQuad(draw, p0, p1, p2, p3, color, alpha);
        }
    }

    /** Smooth lopsided elliptical band used for the black hole's photon and accretion rings. */
    private static void addAccretionRingGlow(
            DrawState draw,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            float radiusX,
            float radiusY,
            float width,
            int color,
            float alpha,
            int segments,
            double phase) {
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            double bright0 = 0.18 + 0.82 * Math.pow(0.5 + 0.5 * Math.cos(a0 - phase), 2.0);
            double bright1 = 0.18 + 0.82 * Math.pow(0.5 + 0.5 * Math.cos(a1 - phase), 2.0);
            double midBright = (bright0 + bright1) * 0.5;
            int segmentColor = mixColor(color, GauntletVfx.SIGMA_PALE, midBright * 0.34);
            Vec3 p0 = center.add(right.scale(Math.cos(a0) * (radiusX - width)))
                    .add(up.scale(Math.sin(a0) * (radiusY - width)));
            Vec3 p1 = center.add(right.scale(Math.cos(a0) * (radiusX + width)))
                    .add(up.scale(Math.sin(a0) * (radiusY + width)));
            Vec3 p2 = center.add(right.scale(Math.cos(a1) * (radiusX + width)))
                    .add(up.scale(Math.sin(a1) * (radiusY + width)));
            Vec3 p3 = center.add(right.scale(Math.cos(a1) * (radiusX - width)))
                    .add(up.scale(Math.sin(a1) * (radiusY - width)));
            addVertex(draw, p0, segmentColor, (float) (alpha * bright0));
            addVertex(draw, p1, segmentColor, (float) (alpha * bright0));
            addVertex(draw, p2, segmentColor, (float) (alpha * bright1));
            addVertex(draw, p3, segmentColor, (float) (alpha * bright1));
        }
    }

    /** Camera-facing ring band, like {@link #addHorizontalRing} but billboarded. */
    private static void addCameraRing(
            DrawState draw, Vec3 center, float radius, float width, int color, float alpha, int segments) {
        Vec3 toCamera = draw.camera.subtract(center).normalize();
        if (toCamera.lengthSqr() < 1.0E-6) toCamera = new Vec3(0, 0, 1);
        Vec3 right = UP.cross(toCamera).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = toCamera.cross(right).normalize();
        double inner = Math.max(0.02, radius - width * 0.5);
        double outer = radius + width * 0.5;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            Vec3 p0 = center.add(right.scale(Math.cos(a0) * inner)).add(up.scale(Math.sin(a0) * inner));
            Vec3 p1 = center.add(right.scale(Math.cos(a1) * inner)).add(up.scale(Math.sin(a1) * inner));
            Vec3 p2 = center.add(right.scale(Math.cos(a1) * outer)).add(up.scale(Math.sin(a1) * outer));
            Vec3 p3 = center.add(right.scale(Math.cos(a0) * outer)).add(up.scale(Math.sin(a0) * outer));
            addQuad(draw, p0, p1, p2, p3, color, alpha);
        }
    }

    /** Quad writer with a gradient: corners a,b get {@code alphaInner}, corners c,d get {@code alphaOuter}. */
    private static void addQuadVA(
            DrawState draw, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alphaInner, float alphaOuter) {
        addVertex(draw, a, color, alphaInner);
        addVertex(draw, b, color, alphaInner);
        addVertex(draw, c, color, alphaOuter);
        addVertex(draw, d, color, alphaOuter);
    }

    private static void renderAbsorbTrail(
            ClientLevel level, DrawState glow, DrawState gravity, AbsorbTrail trail) {
        Entity caster = level.getEntity(trail.casterId);
        if (caster == null) return;
        Vec3 end = graspAnchor(caster);
        if (!withinEffectRange(glow, trail.start) && !withinEffectRange(glow, end)) return;
        boolean detailed = withinDetailRange(glow, trail.start) || withinDetailRange(glow, end);
        Vec3 delta = end.subtract(trail.start);
        if (delta.lengthSqr() < 1.0E-5) return;
        float progress = Math.clamp((float) (clientTicks - trail.startTick) / trail.duration, 0f, 1f);
        float eased = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
        Vec3 side = delta.normalize().cross(UP).normalize();
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        Vec3 control = trail.start.add(delta.scale(0.52))
                .add(side.scale(Math.sin(trail.startTick * 1.7) * (trail.heavy ? 0.55 : 0.30)))
                .add(0, trail.heavy ? 0.30 : 0.16, 0);
        float tailT = Math.max(0f, eased - (trail.heavy ? 0.52f : 0.38f));
        Vec3 previous = bezier(trail.start, control, end, tailT);
        int steps = detailed ? 10 : 6;
        for (int i = 1; i <= steps; i++) {
            float t1 = tailT + (eased - tailT) * i / steps;
            float t0 = tailT + (eased - tailT) * (i - 1) / steps;
            Vec3 next = bezier(trail.start, control, end, t1);
            addBeamSegmentUV(
                    gravity, previous, next, trail.heavy ? 0.095f : 0.052f,
                    trail.heavy ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_VIOLET,
                    0.78f * (1.0f - progress * 0.35f), t0, t1);
            previous = next;
        }
        addBillboard(
                glow, previous, trail.heavy ? 0.13f : 0.075f,
                trail.heavy ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_VIOLET,
                0.72f * (1.0f - progress * 0.45f), true);
    }

    private static void renderFluxField(
            ClientLevel level, DrawState glow, DrawState gravity, FluxField field) {
        Vec3 center = field.pos.add(0, 0.12, 0);
        if (!withinEffectRange(glow, center)) return;
        boolean detailed = withinDetailRange(glow, center);
        if (field.phase == 1) {
            int ringCount = detailed ? 2 : 1;
            for (int ring = 0; ring < ringCount; ring++) {
                double radius = 0.72 + ring * 0.46 + 0.06 * Math.sin(clientTicks * 0.13 + ring);
                addBrokenHorizontalRingUV(
                        gravity, center.add(0, ring * 0.08, 0), radius,
                        0.045f, ring == 1 ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_VIOLET,
                        0.26f - ring * 0.06f, detailed ? 48 : 28,
                        clientTicks * (0.018 + ring * 0.008));
            }
        } else if (field.phase == 2) {
            double radius = Math.max(0.5, field.radius);
            addBrokenHorizontalRingUV(
                    gravity, center.add(0, 0.03, 0),
                    radius * 0.52, 0.040f,
                    GauntletVfx.SIGMA_DEEP, 0.18f, detailed ? 56 : 30,
                    -clientTicks * 0.012);
        }
    }

    private static void renderFluxTarget(
            ClientLevel level, DrawState glow, DrawState gravity, int targetId, float phaseValue) {
        Entity target = level.getEntity(targetId);
        if (target == null) return;
        if (!withinEffectRange(glow, target.position())) return;
        boolean detailed = withinDetailRange(glow, target.position());
        boolean falling = (int) phaseValue == 2;
        Vec3 base = target.position().add(0, 0.12, 0);
        double radius = Math.max(0.42, target.getBbWidth() * 0.72);
        int ringCount = detailed ? 2 : 1;
        for (int ring = 0; ring < ringCount; ring++) {
            double y = target.getBbHeight() * (0.25 + ring * 0.42);
            double spin = clientTicks * (falling ? -0.055 : 0.045) + ring * 1.4;
            addBrokenHorizontalRingUV(
                    gravity, base.add(0, y, 0), radius * (1.0 - ring * 0.10),
                    0.030f, ring == 1 ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_VIOLET,
                    falling ? 0.28f : 0.22f, detailed ? 36 : 22, spin);
        }
        addBillboard(
                glow, target.position().add(0, target.getBbHeight() * 0.52, 0),
                (float) radius * 0.65f, GauntletVfx.SIGMA_VIOLET,
                falling ? 0.055f : 0.040f, true);
    }

    private static void addBrokenHorizontalRingUV(
            DrawState draw,
            Vec3 center,
            double radius,
            float width,
            int color,
            float alpha,
            int segments,
            double rotation) {
        double inner = Math.max(0.03, radius - width);
        double outer = radius + width;
        for (int i = 0; i < segments; i++) {
            if (Math.floorMod(i * 17 + (int) Math.floor(clientTicks * 0.35), 11) < 2) continue;
            double a0 = rotation + i * Math.PI * 2.0 / segments;
            double a1 = rotation + (i + 1) * Math.PI * 2.0 / segments;
            Vec3 p0 = center.add(Math.cos(a0) * inner, 0, Math.sin(a0) * inner);
            Vec3 p1 = center.add(Math.cos(a0) * outer, 0, Math.sin(a0) * outer);
            Vec3 p2 = center.add(Math.cos(a1) * outer, 0, Math.sin(a1) * outer);
            Vec3 p3 = center.add(Math.cos(a1) * inner, 0, Math.sin(a1) * inner);
            addQuadUV(draw, p0, p1, p2, p3, color, alpha, (float) i / segments, (float) (i + 1) / segments);
        }
    }

    /**
     * Per-frame transient lenses for the gravity lensing pass: Kinetic Grasp black-hole anchors,
     * Gravitic Flux phase fields, and per-target lift bubbles. Rebuilt every frame.
     */
    public static List<LensView> transientLenses(ClientLevel level) {
        List<LensView> views = new ArrayList<>();
        for (int entityId : KINETIC_GRASP_FIELDS.keySet()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                views.add(new LensView(-1, graspAnchor(entity), 0.44f, 0.26f, 0.76f, 0.035f, 0.82f,
                        GauntletVfx.SIGMA_DEEP, 0.035f, 0.12f, 2.1f, 0.42f, 0.08f));
            }
        }
        for (FluxField field : FLUX_FIELDS.values()) {
            if (field.phase == 1) {
                views.add(new LensView(-1, field.pos, 0.95f, 0.25f, 0.76f, 0.025f, 0.84f,
                        GauntletVfx.SIGMA_DEEP, 0.03f, 0.18f, 2.4f, 0.22f, 0.12f));
            } else if (field.phase == 2) {
                views.add(new LensView(-1, field.pos, Math.max(1.3f, field.radius * 0.82f), 0.20f, 0.82f, 0.02f, 0.68f,
                        GauntletVfx.SIGMA_DEEP, 0.02f, 0.15f, 1.8f, 0.16f, 0.18f));
            }
        }
        for (Map.Entry<Integer, TimedFloat> entry : FLUX_TARGETS.entrySet()) {
            boolean falling = (int) entry.getValue().value == 2;
            views.add(new LensView(entry.getKey(), Vec3.ZERO, falling ? 0.78f : 0.88f, falling ? -0.23f : 0.28f,
                    0.78f, 0.025f, falling ? 1.18f : 0.88f,
                    GauntletVfx.SIGMA_DEEP, 0.03f, 0.18f, 2.1f, 0.22f, 0.13f));
        }
        return views;
    }

    /** One transient gravity-lens description; {@code entityId >= 0} binds it to the entity's interpolated center. */
    public record LensView(int entityId, Vec3 pos, float radius, float strength, float softness, float chroma,
            float aniso, int tintRgb, float tintAmount, float wobble, float wobbleFreq, float ring,
            float maxScreenRadius) {}

    private static void renderOneShot(DrawState draw, DrawState biotic, DrawState gravity, OneShot shot) {
        if (!withinEffectRange(draw, shot.pos)) return;
        float progress = Math.clamp((float) (clientTicks - shot.startTick) / shot.duration, 0f, 1f);
        switch (shot.type) {
            case ROCKET_PUNCH_IMPACT -> renderRocketPunchImpact(draw, shot, progress);
            case SEISMIC_SLAM_RING -> renderSeismicSlamRing(draw, shot, progress);
            case METEOR_IMPACT -> renderMeteorImpact(draw, shot, progress);
            case UPPERCUT_BURST -> renderUppercutBurst(draw, shot, progress);
            case NANO_SURGE_CAST -> renderNanoSurgeCast(draw, shot, progress);
            case FLUX_SLAM_RING -> renderFluxSlamRing(draw, gravity, shot, progress);
            case BIOTIC_ORB_BOUNCE -> renderBioticOrbBounce(draw, biotic, shot, progress);
            case BIOTIC_ORB_BURST -> renderBioticOrbBurst(draw, biotic, shot, progress);
            case FLUX_TARGET_IMPACT -> renderFluxTargetImpact(draw, gravity, shot, progress);
            default -> {}
        }
    }

    private static void renderBioticOrbBounce(
            DrawState glow, DrawState biotic, OneShot shot, float progress) {
        float fade = 1.0f - progress;
        addBillboardUV(
                biotic, shot.pos, 0.18f + progress * 0.42f,
                GauntletVfx.MOIRA_PURPLE, 0.58f * fade);
        for (int i = 0; i < 8; i++) {
            double angle = i * 2.399 + shot.startTick * 0.17;
            Vec3 end = shot.pos.add(
                    Math.cos(angle) * (0.16 + progress * 0.72),
                    Math.sin(angle * 1.7) * (0.08 + progress * 0.34),
                    Math.sin(angle) * (0.16 + progress * 0.72));
            addBeamSegment(
                    glow, shot.pos, end, 0.018f,
                    i % 3 == 0 ? GauntletVfx.MOIRA_PALE : GauntletVfx.MOIRA_PURPLE,
                    0.65f * fade);
        }
    }

    private static void renderBioticOrbBurst(
            DrawState glow, DrawState biotic, OneShot shot, float progress) {
        float fade = (1.0f - progress) * (1.0f - progress);
        addBillboardUV(
                biotic, shot.pos, 0.26f + progress * 1.05f,
                GauntletVfx.MOIRA_DEEP, 0.72f * fade);
        addBioticBurst(
                glow, shot.pos, 0.18f + progress * 1.10f,
                GauntletVfx.MOIRA_PURPLE, 0.80f * fade, shot.startTick);
        addCameraRing(
                glow, shot.pos, 0.20f + progress * 0.86f,
                0.055f * fade, GauntletVfx.MOIRA_PALE, 0.62f * fade, 42);
    }

    private static void renderFluxTargetImpact(
            DrawState glow, DrawState gravity, OneShot shot, float progress) {
        float collapse = Math.min(progress / 0.22f, 1.0f);
        float expand = Math.max(0f, (progress - 0.18f) / 0.82f);
        float fade = (1.0f - expand) * (1.0f - expand);
        if (progress < 0.22f) {
            addBrokenHorizontalRingUV(
                    gravity, shot.pos.add(0, 0.16, 0),
                    1.15 - collapse * 0.92, 0.08f,
                    GauntletVfx.SIGMA_PALE, 0.46f, 48, -collapse * 0.9);
        }
        double radius = (0.16 + expand * 1.35) * Math.max(0.65f, shot.param);
        addBrokenHorizontalRingUV(
                gravity, shot.pos.add(0, 0.10, 0), radius,
                0.085f, GauntletVfx.SIGMA_VIOLET, 0.42f * fade, 56, expand * 0.6);
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

    /** Sigma-violet recolor of the slam ring for Gravitic Flux (the gravity ultimate, not the fire skill). */
    private static void renderFluxSlamRing(
            DrawState draw, DrawState gravity, OneShot shot, float progress) {
        double radius = Math.max(0.5f, shot.param);
        float collapse = Math.min(progress / 0.18f, 1.0f);
        float expand = Math.max(0f, (progress - 0.14f) / 0.86f);
        float fade = (1.0f - expand) * (1.0f - expand);
        if (progress < 0.18f) {
            addBrokenHorizontalRingUV(
                    gravity, shot.pos.add(0, 0.17, 0), radius * (1.0 - collapse * 0.82),
                    0.11f, GauntletVfx.SIGMA_PALE, 0.44f, 72, -collapse);
        }
        addBrokenHorizontalRingUV(
                gravity, shot.pos.add(0, 0.10, 0), expand * radius,
                0.13f, GauntletVfx.SIGMA_VIOLET, 0.42f * fade, 72, expand * 0.7);
        addBrokenHorizontalRingUV(
                gravity, shot.pos.add(0, 0.16, 0), expand * radius * 1.13,
                0.07f, GauntletVfx.SIGMA_PALE, 0.24f * fade, 72, -expand * 0.5);
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
        if (!withinLocalDetailRange(entity.position())) return;
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
            if (!withinLocalDetailRange(player.position())) continue;
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

    private static HandPlacement bioticHandPlacement(
            Entity entity, Vec3 camera, int requestedSide, float scale) {
        Minecraft mc = Minecraft.getInstance();
        int side = requestedSide * mainHandSide(entity);
        Vec3 look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        Vec3 right = look.cross(UP).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(look).normalize();

        if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return new HandPlacement(
                    camera.add(look.scale(0.60)).add(right.scale(0.30 * side)).subtract(up.scale(0.28)),
                    scale);
        }
        return new HandPlacement(
                entity.position()
                        .add(0, entity.getBbHeight() * 0.60, 0)
                        .add(right.scale(0.34 * side))
                        .add(look.scale(0.30)),
                scale * 1.5f);
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

    /**
     * Purple recolor of the meteor target beacon: two pulsing rings plus a beacon pair. The
     * block-surface coating is not reused — it is typed to {@link MeteorTarget} and its
     * inner/outer-radius fade, and Gravitic Flux has a single radius.
     */
    private static void renderGraviticZone(
            ClientLevel level, DrawState draw, DrawState gravity, GraviticZone effect) {
        if (!withinEffectRange(draw, effect.position)) return;
        boolean detailed = withinDetailRange(draw, effect.position);
        double radius = Math.max(0.5, effect.radius);
        if (detailed && effect.shouldRebuildSurfaces(clientTicks)) rebuildGraviticZoneSurfaces(level, effect);
        double pulse = 0.72 + 0.28 * Math.sin(clientTicks * 0.25);
        Vec3 center = new Vec3(effect.position.x, effect.position.y + 0.12, effect.position.z);
        if (detailed) {
            for (MeteorSurfaceFace face : effect.surfaceFaces) {
                double rim = Math.clamp(1.0 - face.horizontalDist / radius, 0.0, 1.0);
                int hash = Math.floorMod(face.pos.getX() * 31 + face.pos.getY() * 17 + face.pos.getZ() * 13, 9);
                if (hash < 2 && rim < 0.72) continue;
                int color = hash % 3 == 0 ? GauntletVfx.SIGMA_PALE : GauntletVfx.SIGMA_DEEP;
                float alpha = (float) ((0.08 + rim * 0.15) * pulse * face.alphaMultiplier);
                addShapeBoxFace(draw, face.pos, face.box, face.direction, color, alpha);
            }
        }

        addBrokenHorizontalRingUV(
                gravity, center, radius, 0.10f,
                GauntletVfx.SIGMA_VIOLET, (float) (0.30 * pulse), detailed ? 80 : 40,
                clientTicks * 0.014);
        int arcCount = detailed ? 1 : 0;
        for (int arc = 0; arc < arcCount; arc++) {
            double inner = radius * (0.46 + 0.025 * Math.sin(clientTicks * 0.12));
            addBrokenHorizontalRingUV(
                    gravity, center.add(0, 0.035 + arc * 0.018, 0), inner,
                    0.040f, GauntletVfx.SIGMA_DEEP,
                    (float) (0.16 * pulse), 64,
                    -clientTicks * 0.014);
        }
    }

    private static void rebuildGraviticZoneSurfaces(ClientLevel level, GraviticZone effect) {
        effect.surfaceFaces.clear();
        effect.lastSurfaceBuildTick = clientTicks;
        double radius = Math.max(0.5, effect.radius);
        int minX = (int) Math.floor(effect.position.x - radius);
        int maxX = (int) Math.ceil(effect.position.x + radius);
        int minY = (int) Math.floor(effect.position.y - 3.0);
        int maxY = (int) Math.ceil(effect.position.y + 3.0);
        int minZ = (int) Math.floor(effect.position.z - radius);
        int maxZ = (int) Math.ceil(effect.position.z + radius);
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            VoxelShape shape = state.getShape(level, pos);
            if (shape.isEmpty()) continue;
            List<AABB> boxes = shape.toAabbs();
            for (AABB box : boxes) {
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN || isMeteorFaceOccluded(level, pos, box, boxes, direction)) continue;
                    Vec3 faceCenter = meteorFaceCenter(pos, box, direction);
                    double horizontalDist = new Vec3(
                            faceCenter.x - effect.position.x, 0, faceCenter.z - effect.position.z)
                                    .length();
                    if (horizontalDist > radius) continue;
                    float alphaMultiplier = direction == Direction.UP ? 1.0f : 0.42f;
                    effect.surfaceFaces.add(new MeteorSurfaceFace(
                            pos.immutable(), box, direction, horizontalDist, alphaMultiplier));
                    if (effect.surfaceFaces.size() >= MAX_TERRAIN_FACES) return;
                }
            }
        }
    }

    private static void collectMeteorBlockCoating(
            ClientLevel level,
            MeteorTarget effect,
            double outer,
            BlockPos pos,
            BlockState state) {
        if (effect.surfaceFaces.size() >= MAX_TERRAIN_FACES) return;
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
                if (effect.surfaceFaces.size() >= MAX_TERRAIN_FACES) return;
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

    /** UV-aware beam segment for the energy pipeline: u runs along the beam, v across (-1..1). */
    private static void addBeamSegmentUV(
            DrawState draw, Vec3 p0, Vec3 p1, float width, int color, float alpha, float u0, float u1) {
        Vec3 dir = p1.subtract(p0).normalize();
        if (dir.lengthSqr() < 1.0E-6) return;
        Vec3 mid = p0.add(p1).scale(0.5);
        Vec3 toCamera = draw.camera.subtract(mid).normalize();
        Vec3 side = dir.cross(toCamera).normalize();
        if (side.lengthSqr() < 1.0E-6) side = dir.cross(UP).normalize();
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        side = side.scale(width);
        addVertexUV(draw, p0.subtract(side), color, alpha, u0, -1f);
        addVertexUV(draw, p0.add(side), color, alpha, u0, 1f);
        addVertexUV(draw, p1.add(side), color, alpha, u1, 1f);
        addVertexUV(draw, p1.subtract(side), color, alpha, u1, -1f);
    }

    /**
     * Emits a camera-facing ribbon whose neighboring quads share the same edge vertices. Tangents
     * are evaluated across adjacent points and side-vector orientation is kept continuous, avoiding
     * the cracks and rectangular hinges produced by independently billboarded beam segments.
     */
    private static void addSmoothRibbonUV(
            DrawState draw, Vec3[] points, float[] widths, int color, float alpha) {
        if (points.length < 2 || points.length != widths.length) return;
        Vec3[] sides = new Vec3[points.length];
        Vec3 previousSide = null;
        for (int i = 0; i < points.length; i++) {
            Vec3 tangent;
            if (i == 0) {
                tangent = points[1].subtract(points[0]);
            } else if (i == points.length - 1) {
                tangent = points[i].subtract(points[i - 1]);
            } else {
                tangent = points[i + 1].subtract(points[i - 1]);
            }
            if (tangent.lengthSqr() < 1.0E-8) {
                tangent = i > 0 ? points[i].subtract(points[i - 1]) : new Vec3(0, 0, 1);
            }
            tangent = tangent.normalize();
            Vec3 toCamera = draw.camera.subtract(points[i]).normalize();
            Vec3 side = tangent.cross(toCamera).normalize();
            if (side.lengthSqr() < 1.0E-6) side = tangent.cross(UP).normalize();
            if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
            if (previousSide != null && side.dot(previousSide) < 0.0) side = side.scale(-1.0);
            previousSide = side;
            sides[i] = side.scale(widths[i]);
        }

        int last = points.length - 1;
        for (int i = 0; i < last; i++) {
            float u0 = (float) i / last;
            float u1 = (float) (i + 1) / last;
            addVertexUV(draw, points[i].subtract(sides[i]), color, alpha, u0, -1f);
            addVertexUV(draw, points[i].add(sides[i]), color, alpha, u0, 1f);
            addVertexUV(draw, points[i + 1].add(sides[i + 1]), color, alpha, u1, 1f);
            addVertexUV(draw, points[i + 1].subtract(sides[i + 1]), color, alpha, u1, -1f);
        }
    }

    /** UV-aware billboard for the energy pipeline: uv spans -1..1 on both axes (radial fade in the shader). */
    private static void addBillboardUV(DrawState draw, Vec3 center, float size, int color, float alpha) {
        Vec3 toCamera = draw.camera.subtract(center).normalize();
        if (toCamera.lengthSqr() < 1.0E-6) toCamera = new Vec3(0, 0, 1);
        Vec3 right = UP.cross(toCamera).normalize();
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        Vec3 up = toCamera.cross(right).normalize();
        Vec3 r = right.scale(size);
        Vec3 u = up.scale(size);
        addVertexUV(draw, center.subtract(r).subtract(u), color, alpha, -1f, -1f);
        addVertexUV(draw, center.add(r).subtract(u), color, alpha, 1f, -1f);
        addVertexUV(draw, center.add(r).add(u), color, alpha, 1f, 1f);
        addVertexUV(draw, center.subtract(r).add(u), color, alpha, -1f, 1f);
    }

    /** UV-aware band quad: corners a,b get u0, corners c,d get u1; v is -1 on a,d and +1 on b,c. */
    private static void addQuadUV(
            DrawState draw, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha, float u0, float u1) {
        addVertexUV(draw, a, color, alpha, u0, -1f);
        addVertexUV(draw, b, color, alpha, u0, 1f);
        addVertexUV(draw, c, color, alpha, u1, 1f);
        addVertexUV(draw, d, color, alpha, u1, -1f);
    }

    private static void addVertexUV(DrawState draw, Vec3 pos, int color, float alpha, float u, float v) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        draw.buffer.addVertex(draw.matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, Math.clamp(alpha, 0f, 1f))
                .setUv(u, v);
        draw.hasVertices = true;
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
        if (charge < 0.78f) return mixColor(0x42AFFF, 0xC8F7FF, charge / 0.78f);
        return mixColor(0xC8F7FF, 0xFF8A24, (charge - 0.78f) / 0.22f);
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

    private static boolean withinEffectRange(DrawState draw, Vec3 position) {
        return draw.camera.distanceToSqr(position) <= EFFECT_RENDER_DISTANCE_SQR;
    }

    private static boolean withinDetailRange(DrawState draw, Vec3 position) {
        return draw.camera.distanceToSqr(position) <= DETAIL_RENDER_DISTANCE_SQR;
    }

    private static boolean withinLocalDetailRange(Vec3 position) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.position().distanceToSqr(position) <= DETAIL_RENDER_DISTANCE_SQR;
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

    private static class TimedFloatPair extends TimedPair {
        final float value;

        TimedFloatPair(int ticksRemaining, int sourceId, int targetId, float value) {
            super(ticksRemaining, sourceId, targetId);
            this.value = value;
        }
    }

    private static class TimedOrbTether extends TimedPair {
        final Vec3 orbFallback;

        TimedOrbTether(int ticksRemaining, int sourceId, int targetId, Vec3 orbFallback) {
            super(ticksRemaining, sourceId, targetId);
            this.orbFallback = orbFallback;
        }
    }

    private static class TimedPos extends Timed {
        final Vec3 pos;

        TimedPos(int ticksRemaining, Vec3 pos) {
            super(ticksRemaining);
            this.pos = pos;
        }
    }

    private static class TimedTargetPos extends TimedPos {
        final int targetId;

        TimedTargetPos(int ticksRemaining, int targetId, Vec3 pos) {
            super(ticksRemaining, pos);
            this.targetId = targetId;
        }
    }

    private static class AbsorbTrail {
        final int casterId;
        final Vec3 start;
        final boolean heavy;
        final int startTick;
        final int duration;

        AbsorbTrail(int casterId, Vec3 start, boolean heavy, int startTick, int duration) {
            this.casterId = casterId;
            this.start = start;
            this.heavy = heavy;
            this.startTick = startTick;
            this.duration = duration;
        }
    }

    /** Gravitic Flux phase field: phase 1 = caster rise bubble, phase 2 = aiming ground shimmer. */
    private static class FluxField extends Timed {
        final int phase;
        final Vec3 pos;
        final float radius;

        FluxField(int ticksRemaining, int phase, Vec3 pos, float radius) {
            super(ticksRemaining);
            this.phase = phase;
            this.pos = pos;
            this.radius = radius;
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

    private static class GraviticZone extends Timed {
        Vec3 position;
        float radius;
        final List<MeteorSurfaceFace> surfaceFaces = new ArrayList<>();
        int lastSurfaceBuildTick = Integer.MIN_VALUE;
        Vec3 lastSurfaceCenter = Vec3.ZERO;

        GraviticZone(int ticksRemaining, Vec3 position, float radius) {
            super(ticksRemaining);
            this.position = position;
            this.radius = radius;
        }

        void refresh(int ticks, Vec3 newPosition, float newRadius) {
            ticksRemaining = ticks;
            if (position.distanceToSqr(newPosition) > 0.16 || Math.abs(radius - newRadius) > 0.05f) {
                lastSurfaceBuildTick = Integer.MIN_VALUE;
            }
            position = newPosition;
            radius = newRadius;
        }

        boolean shouldRebuildSurfaces(int tick) {
            if (lastSurfaceBuildTick == Integer.MIN_VALUE
                    || tick - lastSurfaceBuildTick >= 4
                    || lastSurfaceCenter.distanceToSqr(position) > 0.16) {
                lastSurfaceCenter = position;
                return true;
            }
            return false;
        }
    }

    private record MeteorSurfaceFace(BlockPos pos, AABB box, Direction direction, double horizontalDist,
            float alphaMultiplier) {}
}
