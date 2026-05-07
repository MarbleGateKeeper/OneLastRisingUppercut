package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.SkillDisplayData;
import dev.marblegate.olru.common.attachment.skill.SkillStateType;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class GauntletHudRenderer implements GuiLayer {
    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 4;
    private static final int SLOTS = 4;
    private static final int TOTAL_WIDTH = SLOTS * SLOT_SIZE + (SLOTS - 1) * SLOT_GAP;
    private static final int CHARGE_BAR_GAP = 4;
    private static final int CHARGE_BAR_H = 4;

    private static final int PIP_SIZE = 4;
    private static final int PIP_GAP = 2;
    private static final int CYCLE_BAR_H = 3;

    private static final SkillType[] SKILL_ORDER = {
            SkillType.NORMAL_ATTACK,
            SkillType.SKILL_ONE,
            SkillType.SKILL_TWO,
            SkillType.ULTIMATE
    };
    private static final String[] SKILL_LABELS = { "LMB", "RMB", "Shift", "X" };

    private static final int COLOR_COOLDOWN_BG = 0xAA000000;
    private static final int COLOR_READY_BG = 0x55000000;
    private static final int COLOR_BORDER = 0xFFAAAAAA;
    private static final int COLOR_BORDER_READY = 0xFFFFD700;
    private static final int COLOR_CD_TEXT = 0xFFFF4444;
    private static final int COLOR_BIND_TEXT = 0xFFCCCCCC;
    private static final int COLOR_PIP_FILLED = 0xFFFFD700;
    private static final int COLOR_PIP_EMPTY = 0xFF555555;
    private static final int COLOR_CYCLE_BAR = 0xFF88AAFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem gauntlet)) return;

        GauntletSkillGroup group = gauntlet.getSyncedSkillGroup(mc.player);
        if (group == null) return;

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();
        int startX = (screenW - TOTAL_WIDTH) / 2;
        int startY = screenH - 52 - SLOT_SIZE;

        for (int i = 0; i < SLOTS; i++) {
            SkillType type = SKILL_ORDER[i];
            int x = startX + i * (SLOT_SIZE + SLOT_GAP);
            renderSlot(guiGraphics, x, startY, type, SKILL_LABELS[i], group);
        }

        if (mc.player.isUsingItem()) {
            int ticksHeld = gauntlet.getUseDuration(mc.player.getMainHandItem(), mc.player)
                    - mc.player.getUseItemRemainingTicks();
            float charge = Math.min(1f, (float) ticksHeld / gauntlet.getMaxChargeTicks());
            renderChargeBar(guiGraphics, startX, startY + SLOT_SIZE + CHARGE_BAR_GAP, charge);
        }
    }

    private void renderSlot(GuiGraphicsExtractor guiGraphics, int x, int y,
            SkillType type, String bindLabel, GauntletSkillGroup group) {
        SkillDisplayData data = group.get(type).displayData();
        boolean isCooldownMode = data.mode() == SkillStateType.COOLDOWN;
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE,
                (data.usable() || isCooldownMode) ? COLOR_READY_BG : COLOR_COOLDOWN_BG);

        switch (data.mode()) {
            case COOLDOWN -> renderCooldownOverlay(guiGraphics, x, y, data);
            case INCREMENTAL_CHARGE, FULL_CHARGE, CONDITIONAL -> renderChargeProgress(guiGraphics, x, y, data);
        }

        guiGraphics.outline(x, y, SLOT_SIZE, SLOT_SIZE,
                data.usable() ? COLOR_BORDER_READY : COLOR_BORDER);

        guiGraphics.text(Minecraft.getInstance().font, bindLabel,
                x + 2, y + SLOT_SIZE - 9, COLOR_BIND_TEXT, false);
    }

    private void renderCooldownOverlay(GuiGraphicsExtractor guiGraphics, int x, int y, SkillDisplayData data) {
        if (data.usable()) return;
        int fillH = (int) (SLOT_SIZE * data.cdFraction());
        if (fillH > 0) guiGraphics.fill(x, y, x + SLOT_SIZE, y + fillH, COLOR_COOLDOWN_BG);
    }

    private void renderChargeProgress(GuiGraphicsExtractor guiGraphics, int x, int y, SkillDisplayData data) {
        if (data.cdFraction() > 0f) {
            int barColor = data.mode() == SkillStateType.CONDITIONAL ? COLOR_BORDER_READY : COLOR_CYCLE_BAR;
            int barY = y + SLOT_SIZE - CYCLE_BAR_H;
            int fillW = (int) (SLOT_SIZE * (1f - data.cdFraction())); // left-to-right fill
            guiGraphics.fill(x, barY, x + SLOT_SIZE, barY + CYCLE_BAR_H, COLOR_COOLDOWN_BG);
            if (fillW > 0) guiGraphics.fill(x, barY, x + fillW, barY + CYCLE_BAR_H, barColor);
        }

        int max = data.maxCharges();
        if (max <= 0) return;

        int totalPipW = max * PIP_SIZE + (max - 1) * PIP_GAP;
        int pipStartX = x + (SLOT_SIZE - totalPipW) / 2;
        int pipY = y + 4;
        for (int i = 0; i < max; i++) {
            int px = pipStartX + i * (PIP_SIZE + PIP_GAP);
            guiGraphics.fill(px, pipY, px + PIP_SIZE, pipY + PIP_SIZE,
                    i < data.currentCharges() ? COLOR_PIP_FILLED : COLOR_PIP_EMPTY);
        }
    }

    private void renderChargeBar(GuiGraphicsExtractor guiGraphics, int x, int y, float charge) {
        guiGraphics.fill(x, y, x + TOTAL_WIDTH, y + CHARGE_BAR_H, COLOR_COOLDOWN_BG);
        int fillW = (int) (TOTAL_WIDTH * charge);
        if (fillW > 0) guiGraphics.fill(x, y, x + fillW, y + CHARGE_BAR_H, COLOR_BORDER_READY);
        guiGraphics.outline(x, y, TOTAL_WIDTH, CHARGE_BAR_H, COLOR_BORDER);
    }
}
