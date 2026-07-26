package dev.marblegate.olru.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.marblegate.olru.common.entity.AccretionBoulderEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the Accretion boulder as a tumbling stone block model. */
public class AccretionBoulderRenderer extends EntityRenderer<AccretionBoulderEntity, AccretionBoulderRenderState> {
    private static final BlockState BOULDER_STATE = Blocks.STONE.defaultBlockState();
    private static final float SCALE = 0.9F;
    private static final float TUMBLE_DEGREES_PER_TICK = 22.0F;

    private final BlockModelResolver blockModelResolver;

    public AccretionBoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public AccretionBoulderRenderState createRenderState() {
        return new AccretionBoulderRenderState();
    }

    @Override
    public void extractRenderState(AccretionBoulderEntity entity, AccretionBoulderRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        this.blockModelResolver.update(state.blockModel, BOULDER_STATE, BlockDisplayContext.create());
    }

    @Override
    public void submit(AccretionBoulderRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        // The block model bakes inside the 0..1 cube: center it on the entity, then tumble.
        poseStack.translate(0.0, state.boundingBoxHeight * 0.5, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees((state.ageInTicks * TUMBLE_DEGREES_PER_TICK) % 360.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-0.5, -0.5, -0.5);
        state.blockModel.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
