package dev.marblegate.olru.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.common.entity.BioticOrbEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class BioticOrbRenderer extends EntityRenderer<BioticOrbEntity, EntityRenderState> {
    private static final int CORE_COLOR = 0xB04AD8;
    private static final int SPARKLE_COLOR = 0xEAC4FF;
    private static final float CORE_HALF_SIZE = 0.32F;
    private static final float GLOW_HALF_SIZE = 0.85F;
    private static final double RING_RADIUS = 0.4;
    private static final int RING_POINTS = 12;
    private static final double INNER_RING_RADIUS = 0.25;
    private static final int INNER_RING_POINTS = 6;

    public BioticOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float age = state.ageInTicks;
        poseStack.pushPose();
        poseStack.translate(0.0, state.boundingBoxHeight * 0.5, 0.0);
        poseStack.mulPose(camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, ClientGauntletEffects.GAUNTLET_GLOW,
                (pose, buffer) -> addOrbGeometry(pose, buffer, age));
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void addOrbGeometry(PoseStack.Pose pose, VertexConsumer buffer, float age) {
        double pulse = 0.72 + 0.28 * Math.sin(age * 0.35);

        addQuad(pose, buffer, 0.0f, 0.0f, -0.03f, GLOW_HALF_SIZE, CORE_COLOR, (float) (0.22 * pulse));

        float spin = age * 0.12f;
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = spin + i * Math.PI * 2.0 / RING_POINTS;
            float alpha = (float) (0.30 + 0.20 * Math.sin(age * 0.3 + i * 0.9));
            addQuad(pose, buffer,
                    (float) (Math.cos(angle) * RING_RADIUS),
                    (float) (Math.sin(angle) * RING_RADIUS),
                    -0.015f, 0.06f, SPARKLE_COLOR, alpha);
        }

        for (int i = 0; i < INNER_RING_POINTS; i++) {
            double angle = -spin * 1.4 + i * Math.PI * 2.0 / INNER_RING_POINTS;
            float alpha = (float) (0.40 + 0.25 * Math.sin(age * 0.35 + i * 1.3));
            addQuad(pose, buffer,
                    (float) (Math.cos(angle) * INNER_RING_RADIUS),
                    (float) (Math.sin(angle) * INNER_RING_RADIUS),
                    -0.02f, 0.05f, SPARKLE_COLOR, alpha);
        }

        addQuad(pose, buffer, 0.0f, 0.0f, 0.0f, (float) (CORE_HALF_SIZE * (0.9 + 0.1 * pulse)),
                mixColor(CORE_COLOR, 0xFFFFFF, pulse * 0.35), (float) (0.95 * pulse));
    }

    private static void addQuad(
            PoseStack.Pose pose, VertexConsumer buffer, float cx, float cy, float z, float halfSize, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = Math.clamp(alpha, 0f, 1f);
        buffer.addVertex(pose, cx - halfSize, cy - halfSize, z).setColor(r, g, b, a);
        buffer.addVertex(pose, cx + halfSize, cy - halfSize, z).setColor(r, g, b, a);
        buffer.addVertex(pose, cx + halfSize, cy + halfSize, z).setColor(r, g, b, a);
        buffer.addVertex(pose, cx - halfSize, cy + halfSize, z).setColor(r, g, b, a);
    }

    private static int mixColor(int a, int b, double t) {
        t = Math.clamp((float) t, 0f, 1f);
        int rr = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int rg = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int rb = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (rr << 16) | (rg << 8) | rb;
    }
}
