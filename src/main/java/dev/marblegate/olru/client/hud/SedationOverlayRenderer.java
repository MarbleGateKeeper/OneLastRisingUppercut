package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** Cool blue-grey stepped vignette with restrained eyelid shading for Sedative Dart. */
public class SedationOverlayRenderer implements GuiLayer {
    private static final Identifier CORNER = Identifier.fromNamespaceAndPath(
            "olru", "hud/effects/sedation_corner");
    private static final int LID_RGB = 0x111927;
    private static final int DRIFT_RGB = 0x7995AC;

    private float intensity;
    private float lastTime = Float.NaN;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            intensity = 0f;
            lastTime = Float.NaN;
            return;
        }
        if (mc.screen != null || mc.options.hideGui) {
            lastTime = Float.NaN;
            return;
        }

        float time = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true);
        float delta = Float.isNaN(lastTime) ? 1f : Math.clamp(time - lastTime, 0f, 2f);
        lastTime = time;
        float target = ClientGauntletEffects.isLocalPlayerSedated() ? 1f : 0f;
        intensity = approach(intensity, target, delta * (target > intensity ? 0.18f : 0.1f));
        if (intensity <= 0.001f) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int corner = Math.clamp(Math.min(width, height) / 3, 48, 96);
        int tint = withAlpha(0xFFFFFF, Math.round(220f * intensity));
        renderCorner(guiGraphics, 0, 0, 1f, 1f, corner, tint);
        renderCorner(guiGraphics, width, 0, -1f, 1f, corner, tint);
        renderCorner(guiGraphics, 0, height, 1f, -1f, corner, tint);
        renderCorner(guiGraphics, width, height, -1f, -1f, corner, tint);

        int lidHeight = Math.clamp(height / 18, 7, 15);
        int lidAlpha = Math.round(86f * intensity);
        guiGraphics.fillGradient(0, 0, width, lidHeight,
                withAlpha(LID_RGB, lidAlpha), withAlpha(LID_RGB, 0));
        guiGraphics.fillGradient(0, height - lidHeight, width, height,
                withAlpha(LID_RGB, 0), withAlpha(LID_RGB, lidAlpha));
        renderEdgeDrift(guiGraphics, width, height, time, intensity);
    }

    private void renderCorner(GuiGraphicsExtractor guiGraphics, int x, int y,
            float scaleX, float scaleY, int size, int tint) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scaleX, scaleY);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CORNER, 0, 0, size, size, tint);
        guiGraphics.pose().popMatrix();
    }

    private void renderEdgeDrift(GuiGraphicsExtractor guiGraphics, int width, int height,
            float time, float alpha) {
        int phase = (int) Math.floor(time * 0.3f);
        int color = withAlpha(DRIFT_RGB, Math.round(54f * alpha));
        for (int i = 0; i < 8; i++) {
            int y = Math.floorMod(i * 41 + phase * (1 + i % 3), Math.max(1, height - 2));
            int offset = 2 + Math.floorMod(i * 5 + phase / 2, 7);
            int length = 1 + i % 2;
            guiGraphics.fill(offset, y, offset + length, y + 1, color);
            guiGraphics.fill(width - offset - length, height - y - 1,
                    width - offset, height - y, color);
        }
    }

    private static float approach(float value, float target, float amount) {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | rgb;
    }
}
