package dev.marblegate.olru.mixin;

import dev.marblegate.olru.client.animation.GauntletPoseRenderData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void olru$applyGauntletPose(HumanoidRenderState state, CallbackInfo ci) {
        GauntletPoseRenderData data = state.getRenderData(GauntletPoseRenderData.KEY);
        if (data == null) return;
        float weight = Math.clamp(data.weight(), 0f, 1f);
        if (weight <= 0f) return;

        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        // hat is a child of head, so it inherits head transforms automatically
        data.parts().forEach((part, pose) -> {
            ModelPart modelPart = switch (part) {
                case HEAD -> model.head;
                case BODY -> model.body;
                case RIGHT_ARM -> model.rightArm;
                case LEFT_ARM -> model.leftArm;
                case RIGHT_LEG -> model.rightLeg;
                case LEFT_LEG -> model.leftLeg;
            };
            modelPart.xRot = Mth.lerp(weight, modelPart.xRot, pose.xRot());
            modelPart.yRot = Mth.lerp(weight, modelPart.yRot, pose.yRot());
            modelPart.zRot = Mth.lerp(weight, modelPart.zRot, pose.zRot());
            modelPart.x += pose.x() * weight;
            modelPart.y += pose.y() * weight;
            modelPart.z += pose.z() * weight;
        });
    }
}
