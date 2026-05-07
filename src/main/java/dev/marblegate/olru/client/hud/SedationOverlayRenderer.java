package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class SedationOverlayRenderer implements GuiLayer {
    private int ticks;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientGauntletEffects.isLocalPlayerSedated()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        ticks++;
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        guiGraphics.fill(0, 0, w, h, 0xAA000018);

        for (int i = 0; i < 6; i++) {
            int side = i % 2 == 0 ? 1 : -1;
            int x = side > 0 ? w - 36 - i * 3 : 24 + i * 3;
            int y = h - 42 - ((ticks * 2 + i * 17) % Math.max(1, h / 2));
            guiGraphics.text(mc.font, "Z", x, y, 0xFF9DDDFF, false);
        }
    }
}
