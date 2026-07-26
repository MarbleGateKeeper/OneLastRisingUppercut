package dev.marblegate.olru.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.common.entity.AxiomBarrierEntity;
import dev.marblegate.olru.config.OLRUConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * The Experimental Barrier as one seamless glass panel: a single width x height quad, emitted in
 * both windings so it reads from either side, textured with the vanilla glass block sprite from
 * the block atlas (full-sprite UVs). A white flash sweeps the panel for two ticks whenever the
 * synced durability drops.
 */
public class AxiomBarrierRenderer extends EntityRenderer<AxiomBarrierEntity, AxiomBarrierRenderState> {
    private static final Identifier GLASS_SPRITE = Identifier.withDefaultNamespace("block/glass");
    /** Offset of the damage-flash planes from the wall center, just outside the glass quad. */
    private static final float FLASH_Z = 0.01f;

    /** Per-entity client-side durability shadow driving the damage flash; client thread only. */
    private static final Map<Integer, FlashState> FLASH_STATES = new HashMap<>();
    private static int clientTick;

    public AxiomBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AxiomBarrierRenderState createRenderState() {
        return new AxiomBarrierRenderState();
    }

    @Override
    public void extractRenderState(AxiomBarrierEntity entity, AxiomBarrierRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.durabilityFraction = entity.getDurabilityFraction();
        state.entityId = entity.getId();
    }

    /** Client-tick hook: purges flash bookkeeping of barriers that stopped rendering. */
    public static void tick() {
        clientTick++;
        FLASH_STATES.values().removeIf(flash -> clientTick - flash.lastSeenTick > 100);
    }

    @Override
    public void submit(AxiomBarrierRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float flashAlpha = updateFlash(state);
        var cfg = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER;
        float halfWidth = (float) cfg.width.getAsDouble() * 0.5f;
        float halfHeight = (float) cfg.height.getAsDouble() * 0.5f;
        TextureAtlasSprite glass = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(GLASS_SPRITE);
        poseStack.pushPose();
        poseStack.translate(0.0, state.boundingBoxHeight * 0.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
                (pose, buffer) -> addGlassPanel(pose, buffer, glass, halfWidth, halfHeight, state.lightCoords));
        if (flashAlpha > 0f) {
            submitNodeCollector.submitCustomGeometry(poseStack, ClientGauntletEffects.GAUNTLET_GLOW,
                    (pose, buffer) -> addFlash(pose, buffer, halfWidth, halfHeight, flashAlpha));
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static float updateFlash(AxiomBarrierRenderState state) {
        FlashState flash = FLASH_STATES.computeIfAbsent(state.entityId, id -> new FlashState());
        if (flash.lastSeenTick == 0) {
            flash.lastFraction = state.durabilityFraction; // first sight: no flash
        }
        if (state.durabilityFraction < flash.lastFraction - 1.0E-4f) {
            flash.flashStartAge = state.ageInTicks;
        }
        flash.lastFraction = state.durabilityFraction;
        flash.lastSeenTick = clientTick;
        return Math.clamp(0.5f * (1f - (state.ageInTicks - flash.flashStartAge) / 2f), 0f, 0.5f);
    }

    /**
     * The glass panel as one quad covering the full sprite, emitted in both windings with the
     * normal pointing out of the rendered face (the pipeline is unculled, but per-face lighting
     * needs the outward normal on each side).
     */
    private static void addGlassPanel(
            PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite,
            float halfWidth, float halfHeight, int lightCoords) {
        addPanelQuad(pose, buffer, sprite, halfWidth, halfHeight, lightCoords, false);
        addPanelQuad(pose, buffer, sprite, halfWidth, halfHeight, lightCoords, true);
    }

    private static void addPanelQuad(
            PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite,
            float halfWidth, float halfHeight, int lightCoords, boolean flipped) {
        float normalZ = flipped ? -1f : 1f;
        // Corner order around the face; reversing it produces the opposite winding.
        float[] xs = flipped ? new float[] { halfWidth, -halfWidth, -halfWidth, halfWidth }
                : new float[] { -halfWidth, halfWidth, halfWidth, -halfWidth };
        float[] ys = { -halfHeight, -halfHeight, halfHeight, halfHeight };
        float[] us = flipped ? new float[] { sprite.getU1(), sprite.getU0(), sprite.getU0(), sprite.getU1() }
                : new float[] { sprite.getU0(), sprite.getU1(), sprite.getU1(), sprite.getU0() };
        float[] vs = { sprite.getV1(), sprite.getV1(), sprite.getV0(), sprite.getV0() };
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, xs[i], ys[i], 0f)
                    .setColor(1f, 1f, 1f, 1f)
                    .setUv(us[i], vs[i])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(lightCoords)
                    .setNormal(pose, 0f, 0f, normalZ);
        }
    }

    /** Damage flash overlay, decaying over two ticks, drawn just outside both faces of the panel. */
    private static void addFlash(PoseStack.Pose pose, VertexConsumer buffer, float halfWidth, float halfHeight, float flashAlpha) {
        addFlashAtZ(pose, buffer, halfWidth, halfHeight, flashAlpha, -FLASH_Z);
        addFlashAtZ(pose, buffer, halfWidth, halfHeight, flashAlpha, FLASH_Z);
    }

    private static void addFlashAtZ(
            PoseStack.Pose pose, VertexConsumer buffer, float halfWidth, float halfHeight, float flashAlpha, float z) {
        float a = Math.clamp(flashAlpha, 0f, 1f);
        buffer.addVertex(pose, -halfWidth, -halfHeight, z).setColor(1f, 1f, 1f, a);
        buffer.addVertex(pose, halfWidth, -halfHeight, z).setColor(1f, 1f, 1f, a);
        buffer.addVertex(pose, halfWidth, halfHeight, z).setColor(1f, 1f, 1f, a);
        buffer.addVertex(pose, -halfWidth, halfHeight, z).setColor(1f, 1f, 1f, a);
    }

    private static class FlashState {
        float lastFraction = 1.0f;
        float flashStartAge = -1000f;
        int lastSeenTick;
    }
}
