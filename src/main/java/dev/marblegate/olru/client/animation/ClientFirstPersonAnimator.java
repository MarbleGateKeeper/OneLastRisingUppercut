package dev.marblegate.olru.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.marblegate.olru.client.animation.ClientGauntletAnimations.WeightedFp;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Applies the active gauntlet pose's first-person transform to the main-hand arm (and held gauntlet)
 * by mutating the PoseStack before the hand is rendered.
 */
public final class ClientFirstPersonAnimator {
    private ClientFirstPersonAnimator() {}

    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!(event.getItemStack().getItem() instanceof AbstractGauntletItem)) return;
        WeightedFp fp = ClientGauntletAnimations.evaluateFp(player.getId());
        if (fp == null) return;
        float weight = fp.weight();
        GauntletPose.FpTransform transform = fp.transform();
        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(transform.x() * weight, transform.y() * weight, transform.z() * weight);
        poseStack.mulPose(Axis.ZP.rotationDegrees(transform.zRot() * weight));
        poseStack.mulPose(Axis.YP.rotationDegrees(transform.yRot() * weight));
        poseStack.mulPose(Axis.XP.rotationDegrees(transform.xRot() * weight));
    }
}
