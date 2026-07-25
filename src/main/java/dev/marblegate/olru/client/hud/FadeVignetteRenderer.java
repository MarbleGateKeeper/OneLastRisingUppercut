package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** Dark-purple pulsing vignette drawn while the local player's Fade is active. */
public class FadeVignetteRenderer implements GuiLayer {
    private static final int DIM_COLOR = 0x0A0014;
    private static final int EDGE_COLOR = 0x2A0A44; // 0x8A2BE2 tinted dark
    private static final int EDGE_STRIPS = 4;

    private int ticks;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!ClientGauntletEffects.isFading(mc.player.getId())) return;

        ticks++;
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        double pulse = 0.85 + 0.15 * Math.sin(ticks * 0.25);

        guiGraphics.fill(0, 0, w, h, withAlpha(DIM_COLOR, (int) (0x33 * pulse)));

        // Edge gradient: alpha grows toward the screen borders.
        int strip = Math.min(w, h) / 24;
        for (int i = 0; i < EDGE_STRIPS; i++) {
            int inset = i * strip;
            int alpha = (int) ((0x28 + (EDGE_STRIPS - 1 - i) * 0x1E) * pulse);
            int color = withAlpha(EDGE_COLOR, alpha);
            guiGraphics.fill(inset, inset, w - inset, inset + strip, color);
            guiGraphics.fill(inset, h - inset - strip, w - inset, h - inset, color);
            guiGraphics.fill(inset, inset + strip, inset + strip, h - inset - strip, color);
            guiGraphics.fill(w - inset - strip, inset + strip, w - inset, h - inset - strip, color);
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | rgb;
    }
}
