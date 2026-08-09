package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** Pixel-dithered purple-black screen edges used while Fade is active. */
public class FadeVignetteRenderer implements GuiLayer {
    private static final Identifier CORNER = Identifier.fromNamespaceAndPath(
            "olru", "hud/effects/fade_corner");
    private static final int EDGE_RGB = 0x42155F;

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
        float target = ClientGauntletEffects.isFading(mc.player.getId()) ? 1f : 0f;
        intensity = approach(intensity, target, delta * (target > intensity ? 0.34f : 0.22f));
        if (intensity <= 0.001f) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int corner = Math.clamp(Math.min(width, height) / 3, 48, 96);
        float pulse = 0.9f + 0.1f * (float) Math.sin(time * 0.42f);
        int tint = withAlpha(0xFFFFFF, Math.round(255f * intensity * pulse));

        renderCorner(guiGraphics, 0, 0, 1f, 1f, corner, tint);
        renderCorner(guiGraphics, width, 0, -1f, 1f, corner, tint);
        renderCorner(guiGraphics, 0, height, 1f, -1f, corner, tint);
        renderCorner(guiGraphics, width, height, -1f, -1f, corner, tint);
        renderEdgeShards(guiGraphics, width, height, time, intensity);
    }

    private void renderCorner(GuiGraphicsExtractor guiGraphics, int x, int y,
            float scaleX, float scaleY, int size, int tint) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scaleX, scaleY);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CORNER, 0, 0, size, size, tint);
        guiGraphics.pose().popMatrix();
    }

    private void renderEdgeShards(GuiGraphicsExtractor guiGraphics, int width, int height,
            float time, float alpha) {
        int phase = (int) Math.floor(time * 0.65f);
        int color = withAlpha(EDGE_RGB, Math.round(90f * alpha));
        for (int i = 0; i < 10; i++) {
            int length = 1 + i % 3;
            if ((i & 1) == 0) {
                int x = Math.floorMod(i * 47 + phase * (1 + i % 2), Math.max(1, width - length));
                int y = 1 + Math.floorMod(i * 5 + phase / 3, 6);
                guiGraphics.fill(x, y, x + length, y + 1, color);
                guiGraphics.fill(width - x - length, height - y - 1,
                        width - x, height - y, color);
            } else {
                int y = Math.floorMod(i * 31 + phase, Math.max(1, height - length));
                int x = 1 + Math.floorMod(i * 7 + phase / 4, 5);
                guiGraphics.fill(x, y, x + 1, y + length, color);
                guiGraphics.fill(width - x - 1, height - y - length,
                        width - x, height - y, color);
            }
        }
    }

    private static float approach(float value, float target, float amount) {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | rgb;
    }
}
