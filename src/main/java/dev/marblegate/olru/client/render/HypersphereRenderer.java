package dev.marblegate.olru.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.marblegate.olru.common.entity.HypersphereEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the sphere's synced debris ItemStack as a small, slowly spinning thrown item. */
public class HypersphereRenderer extends EntityRenderer<HypersphereEntity, HypersphereRenderState> {
    private static final float SCALE = 0.7F;

    private final ItemModelResolver itemModelResolver;

    public HypersphereRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public HypersphereRenderState createRenderState() {
        return new HypersphereRenderState();
    }

    @Override
    public void extractRenderState(HypersphereEntity entity, HypersphereRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(HypersphereRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0, state.boundingBoxHeight * 0.5, 0.0);
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees((state.ageInTicks * 12.0F) % 360.0F));
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
