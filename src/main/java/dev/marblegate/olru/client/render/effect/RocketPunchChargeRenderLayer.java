package dev.marblegate.olru.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;

public class RocketPunchChargeRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final Identifier MEMBRANE_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");

    public RocketPunchChargeRenderLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        RocketPunchChargeRenderData data = state.getRenderData(RocketPunchChargeRenderData.KEY);
        if (data == null || state.isInvisible) return;
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) return;

        submitRightArmMembrane(
                poseStack,
                submitNodeCollector,
                humanoidModel.rightArm,
                15728880,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                state.ageInTicks,
                data);
    }

    public static void submitFirstPersonRightArm(RenderArmEvent event, RocketPunchChargeRenderData data) {
        if (event.getArm() != HumanoidArm.RIGHT) return;

        AbstractClientPlayer player = event.getPlayer();
        AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance()
                .getEntityRenderDispatcher()
                .getPlayerRenderer(player);
        PlayerModel model = renderer.getModel();
        ModelPart rightArm = model.rightArm;

        rightArm.resetPose();
        rightArm.visible = true;
        model.rightSleeve.visible = player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        model.rightArm.zRot = 0.1F;

        submitRightArmMembrane(
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                rightArm,
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                player.tickCount,
                data);
    }

    private static void submitRightArmMembrane(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            ModelPart rightArm,
            int lightCoords,
            int overlayCoords,
            float ageInTicks,
            RocketPunchChargeRenderData data) {
        float charge = data.clampedCharge();
        float pulse = data.pulse(ageInTicks);
        int baseColor = chargeColor(charge);
        int membraneColor = mixColor(baseColor, 0xFFFFFF, 0.10F + pulse * 0.10F);
        int highlightColor = mixColor(baseColor, charge < 0.45F ? 0xFFF6C8 : 0xFFE0E0, 0.28F + pulse * 0.12F);

        submitNodeCollector.order(2)
                .submitModelPart(
                        rightArm,
                        poseStack,
                        RenderTypes.entityTranslucentEmissive(MEMBRANE_TEXTURE, false),
                        15728880,
                        overlayCoords,
                        null,
                        ARGB.color(0.16F + charge * 0.13F + pulse * 0.04F, membraneColor),
                        null);

        submitNodeCollector.order(3)
                .submitModelPart(
                        rightArm,
                        poseStack,
                        RenderTypes.entityTranslucentEmissive(MEMBRANE_TEXTURE, false),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        null,
                        ARGB.color(0.07F + charge * 0.11F + pulse * 0.035F, highlightColor),
                        null);
    }

    private static int chargeColor(float charge) {
        charge = Math.clamp(charge, 0.0F, 1.0F);
        if (charge < 0.45F) return mixColor(0xFFF2A0, 0xFFD200, charge / 0.45F);
        return mixColor(0xFFD200, 0xFF2020, (charge - 0.45F) / 0.55F);
    }

    private static int mixColor(int a, int b, float t) {
        t = Math.clamp(t, 0.0F, 1.0F);
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
}
