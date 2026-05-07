package dev.marblegate.olru.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class NanoSurgeRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final Identifier FORCEFIELD_TEXTURE = Identifier.withDefaultNamespace("textures/misc/forcefield.png");
    private static final Identifier FLOW_TEXTURE = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    public NanoSurgeRenderLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        NanoSurgeRenderData data = state.getRenderData(NanoSurgeRenderData.KEY);
        if (data == null || state.isInvisible) return;

        float pulse = data.pulse(state.ageInTicks);
        float flowU = (state.ageInTicks * 0.018F + data.entityId() * 0.071F) % 1.0F;
        float flowV = (-state.ageInTicks * 0.012F + data.entityId() * 0.037F) % 1.0F;
        M model = this.getParentModel();

        submitNodeCollector.order(1)
                .submitModel(
                        model,
                        state,
                        poseStack,
                        RenderTypes.entityTranslucentEmissive(FORCEFIELD_TEXTURE, false),
                        15728880,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                        ARGB.colorFromFloat(0.22F + pulse * 0.08F, 0.10F, 0.72F, 1.0F),
                        null,
                        state.outlineColor,
                        null);

        submitNodeCollector.order(2)
                .submitModel(
                        model,
                        state,
                        poseStack,
                        RenderTypes.energySwirl(FLOW_TEXTURE, flowU, flowV),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        ARGB.colorFromFloat(0.50F + pulse * 0.24F, 0.16F, 0.92F, 1.0F),
                        null,
                        state.outlineColor,
                        null);

        poseStack.pushPose();
        float outlineScale = 1.025F + pulse * 0.012F;
        poseStack.scale(outlineScale, outlineScale, outlineScale);
        submitNodeCollector.order(3)
                .submitModel(
                        model,
                        state,
                        poseStack,
                        RenderTypes.energySwirl(FLOW_TEXTURE, -flowU * 0.55F, flowV * 0.65F),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        ARGB.colorFromFloat(0.22F + pulse * 0.10F, 0.30F, 0.98F, 1.0F),
                        null,
                        state.outlineColor,
                        null);
        poseStack.popPose();
    }
}
