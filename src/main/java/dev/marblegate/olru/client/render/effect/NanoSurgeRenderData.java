package dev.marblegate.olru.client.render.effect;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

public record NanoSurgeRenderData(int entityId, int ticksRemaining) {
    public static final ContextKey<NanoSurgeRenderData> KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "nano_surge"));

    public float pulse(float ageInTicks) {
        return (float) (0.5F + 0.5F * Math.sin(ageInTicks * 0.24F + entityId * 0.37F));
    }
}
