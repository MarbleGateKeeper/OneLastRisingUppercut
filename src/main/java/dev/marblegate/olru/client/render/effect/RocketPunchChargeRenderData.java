package dev.marblegate.olru.client.render.effect;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

public record RocketPunchChargeRenderData(int entityId, float charge) {
    public static final ContextKey<RocketPunchChargeRenderData> KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "rocket_punch_charge"));

    public float clampedCharge() {
        return Math.clamp(charge, 0.0F, 1.0F);
    }

    public float pulse(float ageInTicks) {
        return (float) (0.5F + 0.5F * Math.sin(ageInTicks * 0.32F + entityId * 0.29F + clampedCharge() * 2.4F));
    }
}
