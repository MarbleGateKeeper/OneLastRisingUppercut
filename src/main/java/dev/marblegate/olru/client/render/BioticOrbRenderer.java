package dev.marblegate.olru.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.client.effect.GauntletVfx;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.entity.BioticOrbEntity;
import java.util.Arrays;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders the damage orb as an asymmetric biotic nucleus: a faceted core, torn rotating shell
 * fragments, curled siphon tendrils, irregular voxel motes, and a smoothed fading trail.
 */
public class BioticOrbRenderer extends EntityRenderer<BioticOrbEntity, EntityRenderState> {
    /** EntityRenderState exposes no entity id, so the id is stashed under this key for the trail bookkeeping. */
    private static final ContextKey<Integer> ENTITY_ID_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "biotic_orb_entity_id"));

    private static final float CORE_RADIUS = 0.14F;
    private static final int CORE_SEGMENTS = 12;
    private static final int CORE_RINGS = 7;
    private static final float MID_SHELL_RADIUS = 0.27F;
    private static final int MID_SHELL_SEGMENTS = 14;
    private static final int MID_SHELL_RINGS = 8;
    private static final float MID_SHELL_ALPHA = 0.34F;
    private static final float OUTER_SHELL_RADIUS = 0.39F;
    private static final int OUTER_SHELL_SEGMENTS = 12;
    private static final int OUTER_SHELL_RINGS = 7;
    private static final float OUTER_SHELL_ALPHA = 0.18F;
    private static final double DETAIL_DISTANCE_SQR = 36.0 * 36.0;

    private static final float HALO_HALF_SIZE = 0.68F;
    private static final double SPARKLE_OUTER_RADIUS = 0.40;
    private static final int SPARKLE_OUTER_POINTS = 11;
    private static final double SPARKLE_INNER_RADIUS = 0.22;
    private static final int SPARKLE_INNER_POINTS = 7;

    private static final float TRAIL_HALF_WIDTH = 0.12F;
    private static final float TRAIL_MAX_ALPHA = 0.58F;

    public BioticOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(BioticOrbEntity entity, EntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.setRenderData(ENTITY_ID_KEY, entity.getId());
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float age = state.ageInTicks;
        // state.x/y/z are the interpolated world position; the pose stack is already centered there.
        double centerX = state.x;
        double centerY = state.y + state.boundingBoxHeight * 0.5;
        double centerZ = state.z;
        boolean detailed = camera.pos.distanceToSqr(new Vec3(centerX, centerY, centerZ)) <= DETAIL_DISTANCE_SQR;

        double[] trail = null;
        Integer entityId = state.getRenderData(ENTITY_ID_KEY);
        if (entityId != null) {
            BioticOrbTrail.update(entityId, age, centerX, centerY, centerZ);
            BioticOrbTrail.prune();
            double[] recorded = detailed
                    ? BioticOrbTrail.snapshotSmoothed(entityId)
                    : BioticOrbTrail.snapshot(entityId);
            if (recorded != null) {
                // Append the interpolated head so the ribbon connects to the orb without a one-tick gap.
                trail = Arrays.copyOf(recorded, recorded.length + 3);
                trail[trail.length - 3] = centerX;
                trail[trail.length - 2] = centerY;
                trail[trail.length - 1] = centerZ;
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.0, state.boundingBoxHeight * 0.5, 0.0);

        // Camera-facing halo and sparkles — on the energy pipeline so the shader feathers the edges.
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, GauntletVfx.GAUNTLET_BIOTIC,
                (pose, buffer) -> addBillboardGeometry(pose, buffer, age, detailed));
        poseStack.popPose();

        // World-oriented torn shell fragments and tendrils; there are deliberately no clean orbit rings.
        submitNodeCollector.submitCustomGeometry(poseStack, GauntletVfx.GAUNTLET_BIOTIC,
                (pose, buffer) -> addEnergyGeometry(pose, buffer, age, detailed));
        submitNodeCollector.submitCustomGeometry(poseStack, ClientGauntletEffects.GAUNTLET_GLOW,
                (pose, buffer) -> addCoreGeometry(pose, buffer, age));

        // Fading world-space ribbon along the recorded flight path.
        if (trail != null) {
            double[] trailPoints = trail;
            Vec3 cameraLocal = camera.pos.subtract(centerX, centerY, centerZ);
            submitNodeCollector.submitCustomGeometry(poseStack, ClientGauntletEffects.GAUNTLET_GLOW,
                    (pose, buffer) -> addTrailGeometry(pose, buffer, trailPoints, centerX, centerY, centerZ, cameraLocal));
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /** Soft pulsing halo behind everything plus an irregular cloud of square biotic motes. */
    private static void addBillboardGeometry(PoseStack.Pose pose, VertexConsumer buffer, float age, boolean detailed) {
        double pulse = 0.72 + 0.28 * Math.sin(age * 0.35);
        addQuad(pose, buffer, 0.0f, 0.0f, -0.03f, (float) (HALO_HALF_SIZE * (0.95 + 0.10 * pulse)),
                GauntletVfx.MOIRA_PURPLE, (float) (0.18 * pulse));

        float spin = age * 0.12f;
        int outerPoints = detailed ? SPARKLE_OUTER_POINTS : 5;
        int innerPoints = detailed ? SPARKLE_INNER_POINTS : 3;
        for (int i = 0; i < outerPoints; i++) {
            double angle = spin + i * 2.399;
            double radius = SPARKLE_OUTER_RADIUS * (0.68 + 0.34 * Math.sin(i * 1.73 + age * 0.09));
            float alpha = (float) (0.30 + 0.20 * Math.sin(age * 0.3 + i * 0.9));
            addQuad(pose, buffer,
                    (float) (Math.cos(angle) * radius),
                    (float) (Math.sin(angle) * radius),
                    -0.015f, 0.035f + (i % 3) * 0.012f,
                    i % 4 == 0 ? GauntletVfx.MOIRA_PALE : GauntletVfx.MOIRA_PURPLE, alpha);
        }
        for (int i = 0; i < innerPoints; i++) {
            double angle = -spin * 1.4 + i * 2.399;
            double radius = SPARKLE_INNER_RADIUS * (0.72 + 0.30 * Math.cos(i * 1.31 + age * 0.11));
            float alpha = (float) (0.40 + 0.25 * Math.sin(age * 0.35 + i * 1.3));
            addQuad(pose, buffer,
                    (float) (Math.cos(angle) * radius),
                    (float) (Math.sin(angle) * radius),
                    -0.02f, 0.045f, GauntletVfx.MOIRA_PALE, alpha);
        }
    }

    /** Bright near-white violet core sphere; gently breathing with the halo pulse. */
    private static void addCoreGeometry(PoseStack.Pose pose, VertexConsumer buffer, float age) {
        double pulse = 0.72 + 0.28 * Math.sin(age * 0.35);
        int color = GauntletVfx.mix(GauntletVfx.MOIRA_PALE, 0xFFFFFF, (float) (0.40 + 0.25 * pulse));
        addAsymmetricCore(
                pose, buffer,
                (float) (CORE_RADIUS * (0.95 + 0.08 * pulse)),
                CORE_SEGMENTS, CORE_RINGS,
                color, (float) (0.80 + 0.18 * pulse), age);
    }

    /** Broken, offset shell fragments and curled tendrils replace the old planet-like sphere and rings. */
    private static void addEnergyGeometry(PoseStack.Pose pose, VertexConsumer buffer, float age, boolean detailed) {
        double pulse = 0.72 + 0.28 * Math.sin(age * 0.35);
        addTornShell(
                pose, buffer,
                (float) (MID_SHELL_RADIUS * (0.97 + 0.05 * pulse)),
                MID_SHELL_SEGMENTS, MID_SHELL_RINGS,
                GauntletVfx.MOIRA_PURPLE, MID_SHELL_ALPHA, age, 0);
        if (detailed) {
            addTornShell(
                    pose, buffer,
                    (float) (OUTER_SHELL_RADIUS * (0.94 + 0.09 * pulse)),
                    OUTER_SHELL_SEGMENTS, OUTER_SHELL_RINGS,
                    GauntletVfx.MOIRA_DEEP, OUTER_SHELL_ALPHA, -age * 0.73f, 1);
        }
        addCurlTendrils(pose, buffer, age, detailed ? 8 : 4);
    }

    /**
     * Camera-facing ribbon through the recorded positions (oldest to newest), fading and narrowing
     * to nothing at the tail. Positions are world space; the pose stack origin is the orb center.
     */
    private static void addTrailGeometry(PoseStack.Pose pose, VertexConsumer buffer, double[] points,
            double centerX, double centerY, double centerZ, Vec3 cameraLocal) {
        int count = points.length / 3;
        if (count < 2) return;
        float r = GauntletVfx.red(GauntletVfx.MOIRA_PURPLE);
        float g = GauntletVfx.green(GauntletVfx.MOIRA_PURPLE);
        float b = GauntletVfx.blue(GauntletVfx.MOIRA_PURPLE);
        Vector3f segment = new Vector3f();
        Vector3f view = new Vector3f();
        Vector3f side = new Vector3f();
        for (int i = 0; i < count - 1; i++) {
            float x0 = (float) (points[i * 3] - centerX);
            float y0 = (float) (points[i * 3 + 1] - centerY);
            float z0 = (float) (points[i * 3 + 2] - centerZ);
            float x1 = (float) (points[(i + 1) * 3] - centerX);
            float y1 = (float) (points[(i + 1) * 3 + 1] - centerY);
            float z1 = (float) (points[(i + 1) * 3 + 2] - centerZ);
            segment.set(x1 - x0, y1 - y0, z1 - z0);
            if (segment.lengthSquared() < 1.0E-8f) continue;
            // Side vector perpendicular to both the segment and the view direction, so the quad faces the camera.
            view.set((float) cameraLocal.x - (x0 + x1) * 0.5f,
                    (float) cameraLocal.y - (y0 + y1) * 0.5f,
                    (float) cameraLocal.z - (z0 + z1) * 0.5f);
            segment.cross(view, side);
            if (side.lengthSquared() < 1.0E-10f) continue;
            side.normalize();
            float t0 = (float) i / (count - 1);
            float t1 = (float) (i + 1) / (count - 1);
            float w0 = TRAIL_HALF_WIDTH * t0;
            float w1 = TRAIL_HALF_WIDTH * t1;
            float a0 = TRAIL_MAX_ALPHA * t0;
            float a1 = TRAIL_MAX_ALPHA * t1;
            buffer.addVertex(pose, x0 - side.x * w0, y0 - side.y * w0, z0 - side.z * w0).setColor(r, g, b, a0);
            buffer.addVertex(pose, x0 + side.x * w0, y0 + side.y * w0, z0 + side.z * w0).setColor(r, g, b, a0);
            buffer.addVertex(pose, x1 + side.x * w1, y1 + side.y * w1, z1 + side.z * w1).setColor(r, g, b, a1);
            buffer.addVertex(pose, x1 - side.x * w1, y1 - side.y * w1, z1 - side.z * w1).setColor(r, g, b, a1);
        }
    }

    private static void addAsymmetricCore(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float radius,
            int segments,
            int rings,
            int color,
            float alpha,
            float age) {
        float r = GauntletVfx.red(color);
        float g = GauntletVfx.green(color);
        float b = GauntletVfx.blue(color);
        float a = Math.clamp(alpha, 0f, 1f);
        for (int ring = 0; ring < rings; ring++) {
            double theta0 = Math.PI * ring / rings;
            double theta1 = Math.PI * (ring + 1) / rings;
            for (int seg = 0; seg < segments; seg++) {
                double psi0 = Math.PI * 2.0 * seg / segments;
                double psi1 = Math.PI * 2.0 * (seg + 1) / segments;
                addCoreVertex(pose, buffer, radius, theta0, psi0, r, g, b, a, age);
                addCoreVertex(pose, buffer, radius, theta0, psi1, r, g, b, a, age);
                addCoreVertex(pose, buffer, radius, theta1, psi1, r, g, b, a, age);
                addCoreVertex(pose, buffer, radius, theta1, psi0, r, g, b, a, age);
            }
        }
    }

    private static void addCoreVertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float radius,
            double theta,
            double psi,
            float r,
            float g,
            float b,
            float a,
            float age) {
        float sinTheta = (float) Math.sin(theta);
        float distortion = 1.0f
                + 0.13f * (float) Math.sin(psi * 3.0 + age * 0.12f) * sinTheta
                + 0.07f * (float) Math.cos(theta * 5.0 - age * 0.17f);
        buffer.addVertex(pose,
                0.018f + radius * distortion * sinTheta * (float) Math.cos(psi),
                -0.012f + radius * distortion * (float) Math.cos(theta) * 0.92f,
                radius * distortion * sinTheta * (float) Math.sin(psi) * 1.08f)
                .setColor(r, g, b, a);
    }

    private static void addTornShell(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float radius,
            int segments,
            int rings,
            int color,
            float alpha,
            float age,
            int layer) {
        float r = GauntletVfx.red(color);
        float g = GauntletVfx.green(color);
        float b = GauntletVfx.blue(color);
        double rotation = age * (layer == 0 ? 0.035 : -0.026);
        for (int ring = 0; ring < rings; ring++) {
            double theta0 = Math.PI * ring / rings;
            double theta1 = Math.PI * (ring + 1) / rings;
            for (int seg = 0; seg < segments; seg++) {
                int fragmentHash = Math.floorMod(seg * 17 + ring * 29 + layer * 11, 19);
                if (fragmentHash < (layer == 0 ? 7 : 11)) continue;
                double psi0 = rotation + Math.PI * 2.0 * seg / segments;
                double psi1 = rotation + Math.PI * 2.0 * (seg + 1) / segments;
                float fragmentAlpha = alpha * (0.66f + fragmentHash / 50.0f);
                addShellVertex(pose, buffer, radius, theta0, psi0, r, g, b, fragmentAlpha, age, layer);
                addShellVertex(pose, buffer, radius, theta0, psi1, r, g, b, fragmentAlpha, age, layer);
                addShellVertex(pose, buffer, radius, theta1, psi1, r, g, b, fragmentAlpha, age, layer);
                addShellVertex(pose, buffer, radius, theta1, psi0, r, g, b, fragmentAlpha, age, layer);
            }
        }
    }

    private static void addShellVertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float radius,
            double theta,
            double psi,
            float r,
            float g,
            float b,
            float alpha,
            float age,
            int layer) {
        float sinTheta = (float) Math.sin(theta);
        float tornRadius = radius
                * (1.0f
                        + 0.12f * (float) Math.sin(psi * (3.0 + layer) + age * 0.09f)
                        + 0.08f * (float) Math.cos(theta * 5.0 - age * 0.06f));
        float x = (layer == 0 ? 0.025f : -0.035f)
                + tornRadius * sinTheta * (float) Math.cos(psi) * (layer == 0 ? 1.08f : 0.92f);
        float y = tornRadius * (float) Math.cos(theta) * (layer == 0 ? 0.88f : 1.14f);
        float z = tornRadius * sinTheta * (float) Math.sin(psi);
        buffer.addVertex(pose, x, y, z)
                .setColor(r, g, b, Math.clamp(alpha, 0f, 1f))
                .setUv((float) (psi / (Math.PI * 2.0)), (float) (theta / Math.PI * 2.0 - 1.0));
    }

    /** Short curling streamers break the silhouette so the orb reads as biotic energy, not a planet. */
    private static void addCurlTendrils(PoseStack.Pose pose, VertexConsumer buffer, float age, int tendrilCount) {
        for (int tendril = 0; tendril < tendrilCount; tendril++) {
            Vector3f previous = null;
            for (int i = 0; i <= 7; i++) {
                float t = i / 7.0f;
                double angle = tendril * 2.399 + age * (0.07 + tendril * 0.002) + t * Math.PI * 1.65;
                float radius = 0.20f + t * (0.30f + (tendril % 3) * 0.025f);
                Vector3f point = new Vector3f(
                        (float) Math.cos(angle) * radius,
                        (float) Math.sin(angle * 1.37 + tendril) * radius * 0.56f,
                        (float) Math.sin(angle) * radius);
                if (previous != null) {
                    Vector3f tangent = new Vector3f(point).sub(previous);
                    Vector3f side = tangent.cross(new Vector3f(0, 1, 0), new Vector3f());
                    if (side.lengthSquared() < 1.0E-8f) side.set(1, 0, 0);
                    side.normalize(0.018f + 0.012f * (1.0f - t));
                    int color = tendril % 3 == 0 ? GauntletVfx.MOIRA_PALE : GauntletVfx.MOIRA_PURPLE;
                    float r = GauntletVfx.red(color);
                    float g = GauntletVfx.green(color);
                    float b = GauntletVfx.blue(color);
                    float alpha = 0.58f * (1.0f - t * 0.55f);
                    float u0 = (i - 1) / 7.0f;
                    float u1 = i / 7.0f;
                    buffer.addVertex(pose, previous.x - side.x, previous.y - side.y, previous.z - side.z)
                            .setColor(r, g, b, alpha)
                            .setUv(u0, -1f);
                    buffer.addVertex(pose, previous.x + side.x, previous.y + side.y, previous.z + side.z)
                            .setColor(r, g, b, alpha)
                            .setUv(u0, 1f);
                    buffer.addVertex(pose, point.x + side.x, point.y + side.y, point.z + side.z)
                            .setColor(r, g, b, alpha)
                            .setUv(u1, 1f);
                    buffer.addVertex(pose, point.x - side.x, point.y - side.y, point.z - side.z)
                            .setColor(r, g, b, alpha)
                            .setUv(u1, -1f);
                }
                previous = point;
            }
        }
    }

    private static void addQuad(
            PoseStack.Pose pose, VertexConsumer buffer, float cx, float cy, float z, float halfSize, int color, float alpha) {
        float r = GauntletVfx.red(color);
        float g = GauntletVfx.green(color);
        float b = GauntletVfx.blue(color);
        float a = Math.clamp(alpha, 0f, 1f);
        buffer.addVertex(pose, cx - halfSize, cy - halfSize, z).setColor(r, g, b, a).setUv(-1f, -1f);
        buffer.addVertex(pose, cx + halfSize, cy - halfSize, z).setColor(r, g, b, a).setUv(1f, -1f);
        buffer.addVertex(pose, cx + halfSize, cy + halfSize, z).setColor(r, g, b, a).setUv(1f, 1f);
        buffer.addVertex(pose, cx - halfSize, cy + halfSize, z).setColor(r, g, b, a).setUv(-1f, 1f);
    }
}
