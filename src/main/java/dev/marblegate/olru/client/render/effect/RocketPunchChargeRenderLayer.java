package dev.marblegate.olru.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;

public class RocketPunchChargeRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    public RocketPunchChargeRenderLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        RocketPunchChargeRenderData data = state.getRenderData(RocketPunchChargeRenderData.KEY);
        if (data == null || state.isInvisible) return;
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) return;

        submitRightArmChargeCage(
                poseStack,
                submitNodeCollector,
                humanoidModel.rightArm,
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

        submitRightArmChargeCage(
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                rightArm,
                player.tickCount,
                data);
    }

    private static void submitRightArmChargeCage(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            ModelPart rightArm,
            float ageInTicks,
            RocketPunchChargeRenderData data) {
        float charge = data.clampedCharge();
        float pulse = data.pulse(ageInTicks);
        poseStack.pushPose();
        rightArm.translateAndRotate(poseStack);
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                ClientGauntletEffects.GAUNTLET_GLOW,
                (pose, buffer) -> addChargeCage(pose, buffer, ageInTicks, charge, pulse));
        poseStack.popPose();
    }

    private static void addChargeCage(
            PoseStack.Pose pose, VertexConsumer buffer, float age, float charge, float pulse) {
        int baseColor = chargeColor(charge);
        int highlight = mixColor(baseColor, 0xE8FBFF, 0.58F + pulse * 0.20F);
        float radiusX = 0.18F + charge * 0.025F;
        float radiusZ = 0.16F + charge * 0.020F;
        int rings = 4;
        for (int ring = 0; ring < rings; ring++) {
            float y = 0.19F + ring * 0.17F;
            float rotation = age * (0.10F + ring * 0.018F) + ring * 0.55F;
            addRing(
                    pose, buffer, y, radiusX * (1.0F - ring * 0.035F), radiusZ,
                    0.010F + charge * 0.006F,
                    ring == rings - 1 ? highlight : baseColor,
                    (0.42F + charge * 0.34F) * pulse, rotation);
        }

        int rails = 5 + (int) (charge * 3.0F);
        for (int rail = 0; rail < rails; rail++) {
            float angle = rail * ((float) Math.PI * 2.0F / rails) + age * 0.075F;
            float x = (float) Math.cos(angle) * radiusX;
            float z = (float) Math.sin(angle) * radiusZ;
            float sway = (float) Math.sin(age * 0.34F + rail * 1.7F) * (0.018F + charge * 0.015F);
            addRail(
                    pose, buffer,
                    x, 0.16F, z,
                    x + sway, 0.73F, z - sway,
                    0.008F + charge * 0.006F,
                    rail % 3 == 0 ? highlight : baseColor,
                    0.38F + charge * 0.42F);
        }

        addRing(
                pose, buffer, 0.75F, radiusX * 1.08F, radiusZ * 1.08F,
                0.018F + charge * 0.012F, highlight,
                0.62F + charge * 0.30F, -age * (0.16F + charge * 0.08F));
    }

    private static void addRing(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float y,
            float radiusX,
            float radiusZ,
            float width,
            int color,
            float alpha,
            float rotation) {
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            if ((i + (int) (rotation * 4.0F)) % 7 == 0) continue;
            float a0 = rotation + i * ((float) Math.PI * 2.0F / segments);
            float a1 = rotation + (i + 1) * ((float) Math.PI * 2.0F / segments);
            addVertex(pose, buffer, (float) Math.cos(a0) * (radiusX - width), y,
                    (float) Math.sin(a0) * (radiusZ - width), color, alpha);
            addVertex(pose, buffer, (float) Math.cos(a0) * (radiusX + width), y,
                    (float) Math.sin(a0) * (radiusZ + width), color, alpha);
            addVertex(pose, buffer, (float) Math.cos(a1) * (radiusX + width), y,
                    (float) Math.sin(a1) * (radiusZ + width), color, alpha);
            addVertex(pose, buffer, (float) Math.cos(a1) * (radiusX - width), y,
                    (float) Math.sin(a1) * (radiusZ - width), color, alpha);
        }
    }

    private static void addRail(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float width,
            int color,
            float alpha) {
        addVertex(pose, buffer, x0 - width, y0, z0, color, alpha);
        addVertex(pose, buffer, x0 + width, y0, z0, color, alpha);
        addVertex(pose, buffer, x1 + width, y1, z1, color, alpha);
        addVertex(pose, buffer, x1 - width, y1, z1, color, alpha);
    }

    private static void addVertex(
            PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        buffer.addVertex(pose, x, y, z).setColor(r, g, b, Math.clamp(alpha, 0.0F, 1.0F));
    }

    private static int chargeColor(float charge) {
        charge = Math.clamp(charge, 0.0F, 1.0F);
        if (charge < 0.78F) return mixColor(0x42AFFF, 0xC8F7FF, charge / 0.78F);
        return mixColor(0xC8F7FF, 0xFF8A24, (charge - 0.78F) / 0.22F);
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
