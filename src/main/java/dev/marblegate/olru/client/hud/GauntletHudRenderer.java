package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.movement.task.ClientEntityPushTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorFallTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorHoverTask;
import dev.marblegate.olru.client.movement.task.ClientRocketPunchTask;
import dev.marblegate.olru.client.movement.task.ClientSeismicSlamTask;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.client.ClientInputHandler;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.SkillDisplayData;
import dev.marblegate.olru.common.attachment.skill.SkillStateType;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.common.item.LegacyOfHorusGauntletItem;
import dev.marblegate.olru.common.item.LegacyPrimeGauntletItem;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class GauntletHudRenderer implements GuiLayer {
    private static final int PRIMARY_RAIL_WIDTH = 52;
    private static final int PRIMARY_GAP = 10;
    private static final int NORMAL_SLOT = 26;
    private static final int ULTIMATE_SLOT = 30;
    private static final int SLOT_GAP = 5;
    private static final int ICON_SIZE = 14;
    private static final int PIP_SIZE = 3;
    private static final int PIP_GAP = 2;
    private static final int RESOURCE_BAR_H = 3;
    private static final int CHARGE_BAR_H = 4;
    private static final int KEY_BADGE_HEIGHT = 8;
    private static final int KEY_BADGE_GAP = 2;
    private static final int CHARGE_TAIL_GAP = 3;
    private static final int MAIN_WIDTH = NORMAL_SLOT * 3 + ULTIMATE_SLOT + SLOT_GAP * 3;
    private static final int HUD_BOTTOM_OFFSET = 90;

    private static final int COLOR_TEXT = 0xFFEFEFEF;
    private static final int COLOR_TEXT_DIM = 0xFFB6B6B6;
    private static final int COLOR_TEXT_SHADOW = 0xEE000000;
    private static final int COLOR_EMPTY_PIP = 0xFF4C4C4C;
    private static final int COLOR_COOLDOWN = 0xB9000000;
    private static final int COLOR_DISABLED = 0x77000000;

    private static final SkillType[] MAIN_SKILL_ORDER = {
            SkillType.SKILL_ONE,
            SkillType.SKILL_TWO,
            SkillType.SKILL_THREE,
            SkillType.ULTIMATE
    };

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem gauntlet)) return;

        Theme theme = themeFor(gauntlet);
        GauntletSkillGroup group = gauntlet.getSyncedSkillGroup(mc.player);

        int startX = (guiGraphics.guiWidth() - MAIN_WIDTH) / 2;
        int startY = guiGraphics.guiHeight() - HUD_BOTTOM_OFFSET;

        if (group == null) {
            renderSyncPending(guiGraphics, startX, startY, theme);
            return;
        }

        boolean charging = mc.player.isUsingItem()
                && mc.player.getUseItem().getItem() instanceof AbstractGauntletItem;
        float charge = 0f;
        if (charging) {
            int ticksHeld = gauntlet.getUseDuration(mc.player.getMainHandItem(), mc.player)
                    - mc.player.getUseItemRemainingTicks();
            charge = Math.min(1f, (float) ticksHeld / gauntlet.getMaxChargeTicks());
        }

        renderPrimaryAmmoRail(guiGraphics, mc.font, primaryX(startX), primaryY(startY),
                group.get(SkillType.NORMAL_ATTACK).displayData(), theme);

        int x = startX;
        for (SkillType type : MAIN_SKILL_ORDER) {
            int size = slotSize(type);
            boolean slotCharging = charging && type == SkillType.SKILL_ONE;
            renderSlot(guiGraphics, mc.font, x, startY + (ULTIMATE_SLOT - size), size,
                    skillLabel(type), group.get(type).displayData(), theme, iconFor(gauntlet, type),
                    slotCharging, charge, isSkillActive(gauntlet, type));
            x += size + SLOT_GAP;
        }

        if (charging) {
            int skillOneX = startX;
            int skillOneY = startY + (ULTIMATE_SLOT - NORMAL_SLOT);
            renderChargeTail(guiGraphics, skillOneX, skillOneY - CHARGE_BAR_H - CHARGE_TAIL_GAP, NORMAL_SLOT, charge, theme);
        }
    }

    private void renderSyncPending(GuiGraphicsExtractor guiGraphics, int startX, int startY, Theme theme) {
        renderPrimaryAmmoRailPlaceholder(guiGraphics, Minecraft.getInstance().font, primaryX(startX), primaryY(startY), theme);

        int x = startX;
        for (SkillType type : MAIN_SKILL_ORDER) {
            int size = slotSize(type);
            int y = startY + (ULTIMATE_SLOT - size);
            renderSlotFrame(guiGraphics, x, y, size, theme, false, false, false);
            drawPixelQuestion(guiGraphics, x + (size - ICON_SIZE) / 2, y + 5, theme.dimIcon());
            drawKeyBadgeBelow(guiGraphics, Minecraft.getInstance().font, x, y + size + KEY_BADGE_GAP, size,
                    skillLabel(type));
            x += size + SLOT_GAP;
        }
    }

    private void renderPrimaryAmmoRail(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, SkillDisplayData data, Theme theme) {
        guiGraphics.text(font, "LMB", x, y + 1, data.usable() ? 0xFF8F8F8F : 0xFF5F5F5F, false);

        int max = data.maxCharges();
        if (max > 0) {
            int pipSize = 3;
            int gap = 2;
            int pipW = max * pipSize + (max - 1) * gap;
            int px = x + PRIMARY_RAIL_WIDTH - pipW;
            int py = y + 3;
            for (int i = 0; i < max; i++) {
                int color = i < data.currentCharges() ? (theme.resource() & 0xCCFFFFFF) : 0xFF2F2F2F;
                guiGraphics.fill(px, py, px + pipSize, py + pipSize, color);
                px += pipSize + gap;
            }

            if (data.cdFraction() > 0f && data.currentCharges() < data.maxCharges()) {
                int barX = x + PRIMARY_RAIL_WIDTH - pipW;
                int fillW = (int) (pipW * (1f - data.cdFraction()));
                guiGraphics.fill(barX, y + 8, barX + pipW, y + 9, 0x88050505);
                if (fillW > 0) {
                    guiGraphics.fill(barX, y + 8, barX + fillW, y + 9, theme.resource() & 0x99FFFFFF);
                }
            }
        }
    }

    private void renderPrimaryAmmoRailPlaceholder(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, Theme theme) {
        guiGraphics.text(font, "LMB", x, y + 1, 0xFF5F5F5F, false);
        int px = x + PRIMARY_RAIL_WIDTH - 18;
        for (int i = 0; i < 4; i++) {
            guiGraphics.fill(px, y + 3, px + 3, y + 6, 0xFF2F2F2F);
            px += 5;
        }
        guiGraphics.fill(x + 19, y + 8, x + PRIMARY_RAIL_WIDTH, y + 9, theme.line() & 0x55000000);
    }

    private void renderSlot(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int size,
            String bindLabel, SkillDisplayData data, Theme theme, SkillIcon icon,
            boolean charging, float charge, boolean active) {
        boolean ready = data.usable();
        boolean unavailable = !ready && data.mode() != SkillStateType.COOLDOWN;
        renderSlotFrame(guiGraphics, x, y, size, theme, ready, active, charging);

        int iconColor = active || ready || charging ? theme.icon() : theme.dimIcon();
        int iconAccent = active || charging ? theme.hot() : theme.accent();
        drawSkillIcon(guiGraphics, icon, x + (size - ICON_SIZE) / 2, y + 5, iconColor, iconAccent);

        switch (data.mode()) {
            case COOLDOWN -> renderCooldown(guiGraphics, font, x, y, size, data, theme);
            case INCREMENTAL_CHARGE -> renderChargePips(guiGraphics, x, y, size, data, theme, true);
            case FULL_CHARGE -> renderChargePips(guiGraphics, x, y, size, data, theme, false);
            case CONDITIONAL -> renderConditionalBar(guiGraphics, x, y, size, data, theme);
        }

        if (charging) renderChargingOverlay(guiGraphics, x, y, size, charge, theme);
        if (active) renderActiveEdge(guiGraphics, x, y, size, theme);
        if (unavailable) guiGraphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, COLOR_DISABLED);
        drawKeyBadgeBelow(guiGraphics, font, x, y + size + KEY_BADGE_GAP, size, bindLabel);
    }

    private void renderSlotFrame(GuiGraphicsExtractor guiGraphics, int x, int y, int size,
            Theme theme, boolean ready, boolean active, boolean charging) {
        guiGraphics.fill(x + 2, y + 2, x + size + 2, y + size + 2, 0x99000000);
        guiGraphics.fill(x, y, x + size, y + size, 0xFF050505);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, theme.slotBg());
        guiGraphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, theme.slotInner());
        int line = active || charging ? theme.hot() : ready ? theme.accent() : theme.line();
        outline(guiGraphics, x, y, size, size, line);
        outline(guiGraphics, x + 1, y + 1, size - 2, size - 2, ready || active || charging ? theme.innerLine() : 0xFF111111);
    }

    private void renderCooldown(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int size,
            SkillDisplayData data, Theme theme) {
        if (data.usable()) return;
        int fillH = Math.min(size, Math.max(0, (int) Math.ceil(size * data.cdFraction())));
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + 1 + fillH, COLOR_COOLDOWN);
        int barY = y + size - RESOURCE_BAR_H - 2;
        int fillW = (int) ((size - 4) * (1f - data.cdFraction()));
        guiGraphics.fill(x + 2, barY, x + size - 2, barY + RESOURCE_BAR_H, 0xCC050505);
        if (fillW > 0) guiGraphics.fill(x + 2, barY, x + 2 + fillW, barY + RESOURCE_BAR_H, theme.cooldownReady());
        if (data.remainingTicks() > 20) {
            String text = cooldownText(data.remainingTicks());
            drawCenteredText(guiGraphics, font, text, x + size / 2, y + 9, COLOR_TEXT);
        }
    }

    private void renderChargePips(GuiGraphicsExtractor guiGraphics, int x, int y, int size,
            SkillDisplayData data, Theme theme, boolean incremental) {
        int max = data.maxCharges();
        if (max > 0) {
            int pipW = max * PIP_SIZE + (max - 1) * PIP_GAP;
            int px = x + (size - pipW) / 2;
            int py = y + size - 7;
            for (int i = 0; i < max; i++) {
                int color = i < data.currentCharges() ? theme.resource() : COLOR_EMPTY_PIP;
                guiGraphics.fill(px - 1, py - 1, px + PIP_SIZE + 1, py + PIP_SIZE + 1, 0xCC000000);
                guiGraphics.fill(px, py, px + PIP_SIZE, py + PIP_SIZE, color);
                px += PIP_SIZE + PIP_GAP;
            }
        }

        if (data.cdFraction() > 0f && data.currentCharges() < data.maxCharges()) {
            int barY = y + size - 3;
            int fillW = (int) ((size - 6) * (1f - data.cdFraction()));
            int color = incremental ? theme.resource() : theme.hot();
            guiGraphics.fill(x + 3, barY, x + size - 3, barY + 1, 0xDD050505);
            if (fillW > 0) guiGraphics.fill(x + 3, barY, x + 3 + fillW, barY + 1, color);
        }
    }

    private void renderConditionalBar(GuiGraphicsExtractor guiGraphics, int x, int y, int size,
            SkillDisplayData data, Theme theme) {
        int barW = size - 6;
        int barY = y + size - 6;
        int fillW = (int) (barW * (1f - data.cdFraction()));
        guiGraphics.fill(x + 3, barY, x + 3 + barW, barY + RESOURCE_BAR_H, 0xDD050505);
        if (fillW > 0) guiGraphics.fill(x + 3, barY, x + 3 + fillW, barY + RESOURCE_BAR_H, theme.resource());
        if (data.usable()) {
            guiGraphics.fill(x + 3, barY - 1, x + 3 + barW, barY, theme.hot());
        }
    }

    private void renderChargingOverlay(GuiGraphicsExtractor guiGraphics, int x, int y, int size, float charge, Theme theme) {
        int fill = Math.max(1, (int) ((size - 4) * charge));
        guiGraphics.fill(x + 2, y + 2, x + 2 + fill, y + 4, theme.hot());
        guiGraphics.fill(x + 2, y + size - 4, x + 2 + fill, y + size - 2, theme.accent());
        if (charge >= 1f) {
            outline(guiGraphics, x - 1, y - 1, size + 2, size + 2, 0xFFFFFFFF);
        }
    }

    private void renderActiveEdge(GuiGraphicsExtractor guiGraphics, int x, int y, int size, Theme theme) {
        int tick = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount : 0;
        int pulse = (tick / 4) % 4;
        guiGraphics.fill(x + 2 + pulse, y - 1, x + size - 2, y, theme.hot());
        guiGraphics.fill(x + 2, y + size, x + size - 2 - pulse, y + size + 1, theme.hot());
    }

    private void renderChargeTail(GuiGraphicsExtractor guiGraphics, int x, int y, int width, float charge, Theme theme) {
        guiGraphics.fill(x, y, x + width, y + CHARGE_BAR_H, 0xDD050505);
        int fillW = Math.max(1, (int) (width * charge));
        guiGraphics.fill(x, y, x + fillW, y + CHARGE_BAR_H, theme.hot());
        outline(guiGraphics, x - 1, y - 1, width + 2, CHARGE_BAR_H + 2, 0xCC000000);
    }

    private void drawKeyBadgeBelow(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int size, String label) {
        int w = Math.max(14, font.width(label) + 6);
        int bx = x + (size - w) / 2;
        guiGraphics.fill(bx + 1, y + 1, bx + w + 1, y + KEY_BADGE_HEIGHT + 1, 0x77000000);
        guiGraphics.fill(bx, y, bx + w, y + KEY_BADGE_HEIGHT, 0xDD050505);
        outline(guiGraphics, bx, y, w, KEY_BADGE_HEIGHT, 0xFF2F2F2F);
        guiGraphics.text(font, label, bx + 3, y, COLOR_TEXT_DIM, false);
    }

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, Font font, String text, int centerX, int y, int color) {
        int x = centerX - font.width(text) / 2;
        guiGraphics.text(font, text, x + 1, y + 1, COLOR_TEXT_SHADOW, false);
        guiGraphics.text(font, text, x, y, color, false);
    }

    private void drawSkillIcon(GuiGraphicsExtractor guiGraphics, SkillIcon icon, int x, int y, int color, int accent) {
        switch (icon) {
            case CANNON -> drawCannon(guiGraphics, x, y, color, accent);
            case ROCKET -> drawRocket(guiGraphics, x, y, color, accent);
            case UPPERCUT -> drawUppercut(guiGraphics, x, y, color, accent);
            case METEOR -> drawMeteor(guiGraphics, x, y, color, accent);
            case BIOTIC -> drawBiotic(guiGraphics, x, y, color, accent);
            case EXTRACTION -> drawExtraction(guiGraphics, x, y, color, accent);
            case SEDATIVE -> drawSedative(guiGraphics, x, y, color, accent);
            case SLAM -> drawSlam(guiGraphics, x, y, color, accent);
            case GRENADE -> drawGrenade(guiGraphics, x, y, color, accent);
            case NANO -> drawNano(guiGraphics, x, y, color, accent);
        }
    }

    private void drawCannon(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 2, y + 6, x + 10, y + 9, c);
        g.fill(x + 9, y + 5, x + 13, y + 10, c);
        g.fill(x + 1, y + 8, x + 5, y + 12, c);
        g.fill(x + 11, y + 6, x + 14, y + 9, a);
    }

    private void drawRocket(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 5, y + 2, x + 10, y + 10, c);
        g.fill(x + 4, y + 5, x + 11, y + 9, c);
        g.fill(x + 6, y, x + 9, y + 3, a);
        g.fill(x + 3, y + 10, x + 12, y + 12, a);
        g.fill(x + 5, y + 12, x + 10, y + 14, 0xFFFF5A24);
    }

    private void drawUppercut(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 6, y + 1, x + 9, y + 11, a);
        g.fill(x + 4, y + 3, x + 11, y + 6, a);
        g.fill(x + 3, y + 8, x + 11, y + 13, c);
        g.fill(x + 2, y + 10, x + 12, y + 13, c);
    }

    private void drawMeteor(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 6, y + 1, x + 11, y + 6, a);
        g.fill(x + 4, y + 3, x + 12, y + 9, c);
        g.fill(x + 2, y + 8, x + 5, y + 11, a);
        g.fill(x + 7, y + 10, x + 9, y + 14, a);
        g.fill(x + 4, y + 12, x + 12, y + 13, c);
    }

    private void drawBiotic(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 2, y + 6, x + 12, y + 9, c);
        g.fill(x + 9, y + 4, x + 13, y + 11, a);
        g.fill(x + 5, y + 3, x + 8, y + 12, 0xFF65E68D);
        g.fill(x + 2, y + 6, x + 11, y + 9, 0xFF65E68D);
    }

    private void drawExtraction(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 1, y + 7, x + 5, y + 10, a);
        g.fill(x + 9, y + 4, x + 13, y + 13, c);
        g.fill(x + 4, y + 8, x + 10, y + 9, a);
        g.fill(x + 6, y + 6, x + 8, y + 11, a);
    }

    private void drawSedative(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 2, y + 9, x + 11, y + 11, c);
        g.fill(x + 10, y + 8, x + 14, y + 12, a);
        g.fill(x + 4, y + 3, x + 9, y + 4, a);
        g.fill(x + 8, y + 3, x + 8, y + 6, a);
        g.fill(x + 5, y + 6, x + 10, y + 7, a);
    }

    private void drawNano(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 6, y + 1, x + 10, y + 5, a);
        g.fill(x + 5, y + 5, x + 11, y + 11, c);
        g.fill(x + 3, y + 7, x + 13, y + 9, a);
        g.fill(x + 4, y + 11, x + 7, y + 14, c);
        g.fill(x + 9, y + 11, x + 12, y + 14, c);
    }

    private void drawSlam(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 6, y + 1, x + 9, y + 8, c);
        g.fill(x + 3, y + 5, x + 12, y + 8, c);
        g.fill(x + 4, y + 9, x + 11, y + 12, a);
        g.fill(x + 2, y + 12, x + 5, y + 14, a);
        g.fill(x + 6, y + 12, x + 9, y + 14, a);
        g.fill(x + 10, y + 12, x + 13, y + 14, a);
    }

    private void drawGrenade(GuiGraphicsExtractor g, int x, int y, int c, int a) {
        g.fill(x + 5, y + 4, x + 11, y + 10, c);
        g.fill(x + 6, y + 3, x + 10, y + 11, c);
        g.fill(x + 7, y + 1, x + 10, y + 4, a);
        g.fill(x + 9, y + 2, x + 13, y + 3, a);
        g.fill(x + 3, y + 11, x + 13, y + 13, a);
        g.fill(x + 2, y + 6, x + 4, y + 8, 0xFF65E68D);
    }

    private void drawPixelQuestion(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x + 4, y + 2, x + 10, y + 4, color);
        g.fill(x + 9, y + 4, x + 11, y + 7, color);
        g.fill(x + 6, y + 7, x + 10, y + 9, color);
        g.fill(x + 6, y + 11, x + 9, y + 14, color);
    }

    private SkillIcon iconFor(AbstractGauntletItem gauntlet, SkillType type) {
        boolean horus = gauntlet instanceof LegacyOfHorusGauntletItem;
        return switch (type) {
            case NORMAL_ATTACK -> horus ? SkillIcon.BIOTIC : SkillIcon.CANNON;
            case SKILL_ONE -> horus ? SkillIcon.EXTRACTION : SkillIcon.ROCKET;
            case SKILL_TWO -> horus ? SkillIcon.SEDATIVE : SkillIcon.UPPERCUT;
            case SKILL_THREE -> horus ? SkillIcon.GRENADE : SkillIcon.SLAM;
            case ULTIMATE -> horus ? SkillIcon.NANO : SkillIcon.METEOR;
        };
    }

    private boolean isSkillActive(AbstractGauntletItem gauntlet, SkillType type) {
        boolean prime = gauntlet instanceof LegacyPrimeGauntletItem;
        boolean horus = gauntlet instanceof LegacyOfHorusGauntletItem;
        return switch (type) {
            case NORMAL_ATTACK -> false;
            case SKILL_ONE -> prime && ClientMovementManager.isActiveTask(ClientRocketPunchTask.class)
                    || horus && ClientMovementManager.isActiveTask(ClientEntityPushTask.class);
            case SKILL_TWO -> prime && ClientMovementManager.isActiveTask(ClientEntityPushTask.class);
            case SKILL_THREE -> prime && ClientMovementManager.isActiveTask(ClientSeismicSlamTask.class);
            case ULTIMATE -> prime && (ClientMovementManager.isActiveTask(ClientMeteorHoverTask.class)
                    || ClientMovementManager.isActiveTask(ClientMeteorFallTask.class))
                    || horus && mcPlayerHasNanoSurge();
        };
    }

    private boolean mcPlayerHasNanoSurge() {
        var player = Minecraft.getInstance().player;
        return player != null && ClientGauntletEffects.isNanoSurgeActive(player.getId());
    }

    private String skillLabel(SkillType type) {
        return switch (type) {
            case NORMAL_ATTACK -> "LMB";
            case SKILL_ONE -> "RMB";
            case SKILL_TWO -> compactKeyLabel(ClientInputHandler.SKILL_TWO_KEY, "Sft");
            case SKILL_THREE -> compactKeyLabel(ClientInputHandler.SKILL_THREE_KEY, "V");
            case ULTIMATE -> compactKeyLabel(ClientInputHandler.ULTIMATE_KEY, "X");
        };
    }

    private String compactKeyLabel(KeyMapping mapping, String fallback) {
        String raw = mapping.getTranslatedKeyMessage().getString();
        if (raw == null || raw.isBlank()) return fallback;
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("shift")) return "Sft";
        if (lower.contains("ctrl") || lower.contains("control")) return "Ctrl";
        if (lower.contains("alt")) return "Alt";
        if (lower.contains("space")) return "Spc";
        if (lower.contains("mouse")) {
            if (lower.contains("left")) return "LMB";
            if (lower.contains("right")) return "RMB";
            if (lower.contains("middle")) return "MMB";
        }
        if (raw.length() <= 4) return raw;
        return raw.substring(0, 4);
    }

    private int slotSize(SkillType type) {
        return type == SkillType.ULTIMATE ? ULTIMATE_SLOT : NORMAL_SLOT;
    }

    private int primaryX(int startX) {
        return Math.max(4, startX - PRIMARY_GAP - PRIMARY_RAIL_WIDTH);
    }

    private int primaryY(int startY) {
        return startY + ULTIMATE_SLOT - 11;
    }

    private Theme themeFor(AbstractGauntletItem gauntlet) {
        if (gauntlet instanceof LegacyPrimeGauntletItem) {
            return new Theme(
                    0xFF2A1A10, 0xFF3C2818, 0xFF735322,
                    0xFFFFD15A, 0xFFFF6B35, 0xFFFF4332, 0xFFFFA31A,
                    0xFFE9D7A0, 0xFF8F7242, 0xFFBFA76A);
        }
        return new Theme(
                0xFF10242A, 0xFF123842, 0xFF2A7A86,
                0xFF66ECFF, 0xFF72FFB7, 0xFF40A7FF, 0xFF6DFFB2,
                0xFFD2FAFF, 0xFF4A8490, 0xFF75DDE8);
    }

    private String cooldownText(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds >= 10.0) return Integer.toString((int) Math.ceil(seconds));
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private void outline(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.outline(x, y, width, height, color);
    }

    private enum SkillIcon {
        CANNON,
        ROCKET,
        UPPERCUT,
        SLAM,
        METEOR,
        BIOTIC,
        EXTRACTION,
        SEDATIVE,
        GRENADE,
        NANO
    }

    private record Theme(
            int slotBg,
            int slotInner,
            int line,
            int accent,
            int hot,
            int cooldownReady,
            int resource,
            int icon,
            int dimIcon,
            int innerLine) {}
}
