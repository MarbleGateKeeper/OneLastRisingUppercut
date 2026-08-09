package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.client.ClientInputHandler;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.movement.task.ClientEntityPushTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorFallTask;
import dev.marblegate.olru.client.movement.task.ClientMeteorHoverTask;
import dev.marblegate.olru.client.movement.task.ClientRocketPunchTask;
import dev.marblegate.olru.client.movement.task.ClientSeismicSlamTask;
import dev.marblegate.olru.common.attachment.GauntletSkillGroup;
import dev.marblegate.olru.common.attachment.skill.SkillDisplayData;
import dev.marblegate.olru.common.attachment.skill.SkillStateType;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.common.item.FinalAnswerGauntletItem;
import dev.marblegate.olru.common.item.LegacyOfHorusGauntletItem;
import dev.marblegate.olru.common.item.LegacyPrimeGauntletItem;
import dev.marblegate.olru.common.item.TheAxiomGauntletItem;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** A hotbar-width forged chassis with one weapon-status bay and four skill displays. */
public class GauntletHudRenderer implements GuiLayer {
    private static final int PANEL_WIDTH = 182;
    private static final int PANEL_HEIGHT = 44;
    private static final int HUD_BOTTOM_OFFSET = 96;

    private static final int NORMAL_X = 10;
    private static final int NORMAL_Y = 4;
    private static final int NORMAL_WIDTH = 24;
    private static final int NORMAL_HEIGHT = 27;
    private static final int SKILL_ONE_X = 38;
    private static final int SKILL_TWO_X = 68;
    private static final int SKILL_THREE_X = 98;
    private static final int ULTIMATE_X = 137;
    private static final int SKILL_Y = 4;
    private static final int ULTIMATE_Y = 2;
    private static final int SLOT_SIZE = 27;
    private static final int ULTIMATE_SIZE = 34;
    private static final int ICON_SIZE = 16;

    private static final int REGULAR_BADGE_Y = 29;
    private static final int ULTIMATE_BADGE_Y = 34;
    private static final int KEY_BADGE_HEIGHT = 8;
    private static final int ULTIMATE_RAIL_X = 10;
    private static final int ULTIMATE_RAIL_Y = 38;
    private static final int ULTIMATE_RAIL_WIDTH = 118;
    private static final int ULTIMATE_RAIL_HEIGHT = 4;
    private static final int SEGMENTS = 10;

    private static final int PRESS_TICKS = 4;
    private static final int READY_FLASH_TICKS = 12;
    private static final int RESOURCE_TEXT_TICKS = 20;

    private static final int COLOR_COOLDOWN = 0xC4000000;
    private static final int COLOR_GROOVE = 0xEE050609;
    private static final int COLOR_EMPTY = 0xFF202329;
    private static final int COLOR_TEXT = 0xFFF2F2F2;
    private static final int COLOR_TEXT_DIM = 0xFF747981;
    private static final int COLOR_TEXT_SHADOW = 0xC0000000;
    private static final int COLOR_ICON_DIM = 0xFF737373;

    private static final SkillType[] ALL_SKILLS = SkillType.values();
    private static final SkillType[] DISPLAY_SKILLS = {
            SkillType.SKILL_ONE, SkillType.SKILL_TWO, SkillType.SKILL_THREE, SkillType.ULTIMATE
    };

    private static final Theme PRIME_THEME = theme(
            "legacy_prime", 0xFF565C64, 0xFFFF8B26, 0xFFFFC75A, 0xFFE46B1F);
    private static final Theme HORUS_THEME = theme(
            "legacy_of_horus", 0xFF879397, 0xFF35DEEE, 0xFFF2D263, 0xFF64E1C5);
    private static final Theme FINAL_ANSWER_THEME = theme(
            "final_answer", 0xFF674B7A, 0xFFB84FE4, 0xFFFFD15B, 0xFFFFC843);
    private static final Theme AXIOM_THEME = theme(
            "the_axiom", 0xFF626880, 0xFF9E70FF, 0xFFE2DAFF, 0xFFB187FF);
    private static final Theme NEUTRAL_THEME = theme(
            "legacy_prime", 0xFF5B5F66, 0xFF9EA2A8, 0xFFD9D9D9, 0xFFB4B4B4);

    private final EnumMap<SkillType, SlotAnim> slotAnims = new EnumMap<>(SkillType.class);
    private Identifier lastGauntletId;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem gauntlet)) {
            clearAnimationState();
            return;
        }

        Identifier gauntletId = gauntlet.gauntletId();
        Theme theme = themeFor(gauntletId);
        float time = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true);
        int panelX = (guiGraphics.guiWidth() - PANEL_WIDTH) / 2;
        int panelY = guiGraphics.guiHeight() - HUD_BOTTOM_OFFSET;

        if (!gauntletId.equals(lastGauntletId)) {
            lastGauntletId = gauntletId;
            slotAnims.clear();
        }

        GauntletSkillGroup group = gauntlet.getSyncedSkillGroup(mc.player);
        renderSprite(guiGraphics, theme.chassis(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                group == null ? 0xFF686868 : 0xFFFFFFFF);
        if (group == null) {
            slotAnims.clear();
            renderSyncPending(guiGraphics, mc.font, panelX, panelY, theme);
            return;
        }

        EnumMap<SkillType, SkillDisplayData> displayData = new EnumMap<>(SkillType.class);
        for (SkillType type : ALL_SKILLS) {
            SkillDisplayData data = group.get(type).displayData();
            displayData.put(type, data);
            updateSlotAnim(type, data, time);
        }

        boolean charging = mc.player.isUsingItem()
                && mc.player.getUseItem().getItem() == gauntlet;
        float charge = 0f;
        if (charging) {
            int ticksHeld = gauntlet.getUseDuration(mc.player.getMainHandItem(), mc.player)
                    - mc.player.getUseItemRemainingTicks();
            charge = Math.min(1f, (float) ticksHeld / Math.max(1, gauntlet.getMaxChargeTicks()));
        }

        SkillDisplayData normal = displayData.get(SkillType.NORMAL_ATTACK);
        SkillDisplayData skillOne = displayData.get(SkillType.SKILL_ONE);
        SkillDisplayData ultimate = displayData.get(SkillType.ULTIMATE);
        boolean normalActive = isSkillActive(gauntlet, SkillType.NORMAL_ATTACK, normal);
        boolean skillOneActive = isSkillActive(gauntlet, SkillType.SKILL_ONE, skillOne);
        boolean ultimateActive = isSkillActive(gauntlet, SkillType.ULTIMATE, ultimate);

        renderUltimateRail(guiGraphics, panelX, panelY, ultimate, theme, ultimateActive, time);
        renderSkillOneMeter(guiGraphics, mc.font, panelX, panelY, gauntlet, skillOne,
                slotAnims.get(SkillType.SKILL_ONE), charging, charge, skillOneActive, theme, time);
        renderNormalStatus(guiGraphics, mc.font, panelX, panelY, normal,
                slotAnims.get(SkillType.NORMAL_ATTACK), theme, normalActive, time);

        for (SkillType type : DISPLAY_SKILLS) {
            SkillDisplayData data = displayData.get(type);
            renderSkillSlot(guiGraphics, mc.font, panelX, panelY, type, data,
                    slotAnims.get(type), theme,
                    charging && type == SkillType.SKILL_ONE,
                    type == SkillType.SKILL_ONE ? skillOneActive
                            : type == SkillType.ULTIMATE ? ultimateActive : isSkillActive(gauntlet, type, data),
                    time);
        }
    }

    private void renderNormalStatus(GuiGraphicsExtractor guiGraphics, Font font,
            int panelX, int panelY, SkillDisplayData data, SlotAnim anim,
            Theme theme, boolean active, float time) {
        int x = panelX + NORMAL_X;
        int y = panelY + NORMAL_Y;
        boolean ready = data.usable();
        int stateColor = stateColor(theme, SkillType.NORMAL_ATTACK);
        renderStateFrame(guiGraphics, x, y, NORMAL_WIDTH, NORMAL_HEIGHT,
                ready, active, false, false, stateColor, anim, time);

        int pressOffset = pressOffset(anim, time);
        switch (data.mode()) {
            case INCREMENTAL_CHARGE, FULL_CHARGE -> renderAmmoChamber(guiGraphics, font, panelX, panelY, data, theme, pressOffset);
            case CONDITIONAL -> renderEnergyChamber(
                    guiGraphics, font, panelX, panelY, data, anim, theme, active, time, pressOffset);
            case COOLDOWN -> renderCooldown(guiGraphics, font,
                    x + 2, y + 3, NORMAL_WIDTH - 4, 19, data, theme);
            case RESOURCE_COOLDOWN -> {}
        }

        renderKeyBadge(guiGraphics, font, x + NORMAL_WIDTH / 2,
                panelY + REGULAR_BADGE_Y + pressOffset, NORMAL_WIDTH - 2,
                skillLabel(SkillType.NORMAL_ATTACK), ready, active, false, stateColor);
    }

    private void renderAmmoChamber(GuiGraphicsExtractor guiGraphics, Font font,
            int panelX, int panelY, SkillDisplayData data, Theme theme, int pressOffset) {
        int max = data.maxCharges();
        if (max <= 0) return;
        if (max > 8) {
            String text = data.currentCharges() + "/" + max;
            int textWidth = font.width(text);
            int centerX = Math.clamp(panelX + NORMAL_X + NORMAL_WIDTH / 2,
                    panelX + textWidth / 2 + 3, panelX + PANEL_WIDTH - textWidth / 2 - 3);
            int textY = panelY - 10;
            guiGraphics.fill(centerX - textWidth / 2 - 2, textY - 1,
                    centerX + (textWidth + 1) / 2 + 2, textY + 9, COLOR_GROOVE);
            drawCenteredText(guiGraphics, font, text, centerX, textY,
                    data.currentCharges() > 0 ? theme.resource() : COLOR_TEXT_DIM);
            renderPendingDots(guiGraphics, panelX + NORMAL_X + NORMAL_WIDTH / 2,
                    panelY + NORMAL_Y + 12 + pressOffset, theme.resource());
            return;
        }

        int firstRow = Math.min(4, max);
        int secondRow = Math.max(0, max - firstRow);
        int firstY = panelY + NORMAL_Y + (secondRow > 0 ? 7 : 10) + pressOffset;
        renderAmmoRow(guiGraphics, panelX + NORMAL_X + NORMAL_WIDTH / 2,
                firstY, 0, firstRow, data.currentCharges(), theme);
        if (secondRow > 0) {
            renderAmmoRow(guiGraphics, panelX + NORMAL_X + NORMAL_WIDTH / 2,
                    firstY + 6, firstRow, secondRow, data.currentCharges(), theme);
        }

        if (data.currentCharges() < max && data.cdFraction() > 0f) {
            int barX = panelX + NORMAL_X + 2;
            int barY = panelY + NORMAL_Y + 21;
            int barWidth = NORMAL_WIDTH - 4;
            guiGraphics.fill(barX, barY, barX + barWidth, barY + 2, COLOR_GROOVE);
            int progress = Math.clamp(
                    (int) Math.floor(barWidth * (1f - data.cdFraction())), 0, barWidth);
            if (progress > 0) {
                guiGraphics.fill(barX, barY, barX + progress, barY + 2,
                        ARGB.multiplyAlpha(theme.resource(), 0.86f));
            }
        }
    }

    private void renderAmmoRow(GuiGraphicsExtractor guiGraphics, int centerX, int y,
            int startIndex, int count, int currentCharges, Theme theme) {
        int pipSize = 3;
        int gap = 2;
        int width = count * pipSize + Math.max(0, count - 1) * gap;
        int x = centerX - width / 2;
        for (int i = 0; i < count; i++) {
            int index = startIndex + i;
            guiGraphics.fill(x, y, x + pipSize, y + pipSize, COLOR_GROOVE);
            if (index < currentCharges) {
                guiGraphics.fill(x + 1, y, x + pipSize, y + pipSize - 1, theme.resource());
                guiGraphics.fill(x + 1, y, x + pipSize - 1, y + 1, theme.hot());
            } else {
                guiGraphics.fill(x + 1, y + 1, x + pipSize, y + pipSize, COLOR_EMPTY);
            }
            x += pipSize + gap;
        }
    }

    private void renderEnergyChamber(GuiGraphicsExtractor guiGraphics, Font font,
            int panelX, int panelY, SkillDisplayData data, SlotAnim anim,
            Theme theme, boolean active, float time, int pressOffset) {
        float progress = Math.clamp(1f - data.cdFraction(), 0f, 1f);
        int x = panelX + NORMAL_X + 2;
        int y = panelY + NORMAL_Y + 8 + pressOffset;
        renderNarrowSegments(guiGraphics, x, y, 7, progress, theme.resource());
        if (active || progress < 0.2f || time < anim.resourceTextUntil) {
            drawCenteredText(guiGraphics, font, Math.round(progress * 100f) + "%",
                    panelX + NORMAL_X + NORMAL_WIDTH / 2, panelY - 10,
                    progress > 0f ? theme.resource() : COLOR_TEXT_DIM);
        }
    }

    private void renderSkillOneMeter(GuiGraphicsExtractor guiGraphics, Font font,
            int panelX, int panelY, AbstractGauntletItem gauntlet, SkillDisplayData data,
            SlotAnim anim, boolean charging, float charge, boolean active, Theme theme, float time) {
        int x = panelX + SKILL_ONE_X + 1;
        int y = panelY + 1;
        if (charging && gauntlet.isChargeProgressMeaningful()) {
            renderCompactSegments(guiGraphics, x, y, 25, 2, charge, theme.hot());
            return;
        }

        boolean barrier = gauntlet instanceof TheAxiomGauntletItem;
        if (data.mode() == SkillStateType.CONDITIONAL) {
            float progress = Math.clamp(1f - data.cdFraction(), 0f, 1f);
            renderCompactSegments(guiGraphics, x, y, 25, 2, progress, theme.accent());
            if (active || progress < 0.2f || time < anim.resourceTextUntil) {
                drawCenteredText(guiGraphics, font, Math.round(progress * 100f) + "%",
                        panelX + SKILL_ONE_X + SLOT_SIZE / 2, panelY - 10,
                        progress > 0f ? theme.accent() : COLOR_TEXT_DIM);
            }
        } else if (barrier && data.mode() == SkillStateType.COOLDOWN) {
            renderCompactSegments(guiGraphics, x, y, 25, 2, 0f, theme.accent());
        }
    }

    private void renderSkillSlot(GuiGraphicsExtractor guiGraphics, Font font,
            int panelX, int panelY, SkillType type, SkillDisplayData data,
            SlotAnim anim, Theme theme, boolean charging, boolean active, float time) {
        int size = slotSize(type);
        int x = panelX + slotOffsetX(type);
        int y = panelY + slotOffsetY(type);
        boolean ready = data.usable();
        int stateColor = stateColor(theme, type);
        renderStateFrame(guiGraphics, x, y, size, size, ready, active, charging,
                type == SkillType.ULTIMATE, stateColor, anim, time);

        int pressOffset = pressOffset(anim, time);
        int iconX = x + (size - ICON_SIZE) / 2;
        int iconY = y + (type == SkillType.ULTIMATE ? 7 : 4) + pressOffset;
        int iconTint = ready || active || charging ? 0xFFFFFFFF : COLOR_ICON_DIM;
        renderSprite(guiGraphics, theme.icon(type), iconX, iconY, ICON_SIZE, ICON_SIZE, iconTint);

        int wellInset = type == SkillType.ULTIMATE ? 4 : 3;
        int wellHeight = type == SkillType.ULTIMATE ? 24 : 20;
        switch (data.mode()) {
            case COOLDOWN -> renderCooldown(guiGraphics, font,
                    x + wellInset, y + wellInset, size - wellInset * 2, wellHeight, data, theme);
            case INCREMENTAL_CHARGE, FULL_CHARGE -> renderInlineCharges(
                    guiGraphics, font, x, y, size, data, theme);
            case CONDITIONAL, RESOURCE_COOLDOWN -> {}
        }

        int badgeY = panelY + (type == SkillType.ULTIMATE ? ULTIMATE_BADGE_Y : REGULAR_BADGE_Y);
        renderKeyBadge(guiGraphics, font, x + size / 2, badgeY + pressOffset, size - 2,
                skillLabel(type), ready, active || charging, type == SkillType.ULTIMATE, stateColor);
    }

    private void renderStateFrame(GuiGraphicsExtractor guiGraphics, int x, int y,
            int width, int height, boolean ready, boolean active, boolean charging,
            boolean emphasizeReady, int stateColor, SlotAnim anim, float time) {
        if (active || charging) {
            float pulse = 0.55f + 0.12f * (float) Math.sin(time * 0.35f);
            outline(guiGraphics, x, y, width, height, ARGB.multiplyAlpha(stateColor, pulse));
        } else if (ready && emphasizeReady) {
            outline(guiGraphics, x, y, width, height, ARGB.multiplyAlpha(stateColor, 0.72f));
            drawCorners(guiGraphics, x - 1, y - 1, width + 2, height + 2,
                    ARGB.multiplyAlpha(stateColor, 0.62f), 4);
        }

        float elapsed = time - anim.readyFlashStart;
        if (elapsed >= 0f && elapsed < READY_FLASH_TICKS) {
            float alpha = 1f - elapsed / READY_FLASH_TICKS;
            int color = ARGB.multiplyAlpha(stateColor, alpha);
            drawCorners(guiGraphics, x - 1, y - 1, width + 2, height + 2, color, 5);
            int travel = Math.max(1, width - 5);
            int sweepX = x + 1 + Math.min(travel, (int) (travel * elapsed / READY_FLASH_TICKS));
            guiGraphics.fill(sweepX, y - 1, Math.min(x + width, sweepX + 3), y, color);
        }
    }

    private void renderCooldown(GuiGraphicsExtractor guiGraphics, Font font,
            int x, int y, int width, int height, SkillDisplayData data, Theme theme) {
        if (data.usable()) return;
        int veilHeight = Math.clamp((int) Math.ceil(height * data.cdFraction()), 0, height);
        if (veilHeight > 0) {
            guiGraphics.fill(x, y, x + width, y + veilHeight, COLOR_COOLDOWN);
        }

        int barY = y + height - 2;
        guiGraphics.fill(x, barY, x + width, barY + 2, COLOR_GROOVE);
        int fillWidth = Math.clamp((int) Math.floor(width * (1f - data.cdFraction())), 0, width);
        if (fillWidth > 0) {
            guiGraphics.fill(x, barY, x + fillWidth, barY + 2,
                    ARGB.multiplyAlpha(theme.accent(), 0.82f));
        }

        if (data.remainingTicks() > 0) {
            drawCenteredText(guiGraphics, font, cooldownText(data.remainingTicks()),
                    x + width / 2, y + Math.max(1, (height - 9) / 2), COLOR_TEXT);
        }
    }

    private void renderInlineCharges(GuiGraphicsExtractor guiGraphics, Font font,
            int x, int y, int size, SkillDisplayData data, Theme theme) {
        int max = data.maxCharges();
        if (max <= 0) return;
        if (max > 8) {
            drawCenteredText(guiGraphics, font, data.currentCharges() + "/" + max,
                    x + size / 2, y + size - 17,
                    data.currentCharges() > 0 ? theme.resource() : COLOR_TEXT_DIM);
            return;
        }
        int rowWidth = max * 2 + Math.max(0, max - 1);
        int pipX = x + (size - rowWidth) / 2;
        int pipY = y + size - 12;
        for (int i = 0; i < max; i++) {
            guiGraphics.fill(pipX, pipY, pipX + 2, pipY + 2,
                    i < data.currentCharges() ? theme.resource() : COLOR_EMPTY);
            pipX += 3;
        }
    }

    private void renderUltimateRail(GuiGraphicsExtractor guiGraphics,
            int panelX, int panelY, SkillDisplayData data, Theme theme, boolean active, float time) {
        float progress = data.mode() == SkillStateType.CONDITIONAL
                ? Math.clamp(1f - data.cdFraction(), 0f, 1f)
                : data.usable() ? 1f : 0f;
        int x = panelX + ULTIMATE_RAIL_X;
        int y = panelY + ULTIMATE_RAIL_Y;
        float pulse = active ? 0.82f + 0.12f * (float) Math.sin(time * 0.35f) : 1f;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + i * ULTIMATE_RAIL_WIDTH / SEGMENTS;
            int nextX = x + (i + 1) * ULTIMATE_RAIL_WIDTH / SEGMENTS;
            int segmentWidth = Math.max(1, nextX - segmentX - 1);
            guiGraphics.fill(segmentX, y, segmentX + segmentWidth,
                    y + ULTIMATE_RAIL_HEIGHT, COLOR_EMPTY);
            float local = Math.clamp(progress * SEGMENTS - i, 0f, 1f);
            int filled = Math.clamp((int) Math.ceil(segmentWidth * local), 0, segmentWidth);
            if (filled <= 0) continue;
            float alpha = pulse * (0.55f + 0.45f * local);
            if (theme == FINAL_ANSWER_THEME) {
                guiGraphics.fill(segmentX, y, segmentX + filled, y + 2,
                        ARGB.multiplyAlpha(theme.hot(), alpha));
                guiGraphics.fill(segmentX, y + 2, segmentX + filled, y + ULTIMATE_RAIL_HEIGHT,
                        ARGB.multiplyAlpha(theme.accent(), alpha));
            } else {
                int color = data.usable() ? theme.hot() : theme.accent();
                guiGraphics.fill(segmentX, y, segmentX + filled, y + ULTIMATE_RAIL_HEIGHT,
                        ARGB.multiplyAlpha(color, alpha));
                if (data.usable()) {
                    guiGraphics.fill(segmentX, y, segmentX + filled, y + 1,
                            ARGB.multiplyAlpha(theme.hot(), Math.min(1f, alpha + 0.18f)));
                }
            }
        }
    }

    private void renderNarrowSegments(GuiGraphicsExtractor guiGraphics,
            int x, int y, int height, float progress, int color) {
        progress = Math.clamp(progress, 0f, 1f);
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + i * 2;
            guiGraphics.fill(segmentX, y, segmentX + 1, y + height, COLOR_EMPTY);
            float local = Math.clamp(progress * SEGMENTS - i, 0f, 1f);
            int filledHeight = Math.clamp((int) Math.ceil(height * local), 0, height);
            if (filledHeight > 0) {
                guiGraphics.fill(segmentX, y + height - filledHeight,
                        segmentX + 1, y + height, ARGB.multiplyAlpha(color, 0.62f + 0.38f * local));
            }
        }
    }

    private void renderCompactSegments(GuiGraphicsExtractor guiGraphics,
            int x, int y, int width, int height, float progress, int color) {
        progress = Math.clamp(progress, 0f, 1f);
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + i * width / SEGMENTS;
            int nextX = x + (i + 1) * width / SEGMENTS;
            int segmentWidth = Math.max(1, nextX - segmentX - 1);
            guiGraphics.fill(segmentX, y, segmentX + segmentWidth, y + height, COLOR_EMPTY);
            float local = Math.clamp(progress * SEGMENTS - i, 0f, 1f);
            int filled = Math.clamp((int) Math.ceil(segmentWidth * local), 0, segmentWidth);
            if (filled > 0) {
                guiGraphics.fill(segmentX, y, segmentX + filled, y + height,
                        ARGB.multiplyAlpha(color, 0.58f + 0.42f * local));
            }
        }
    }

    private void renderKeyBadge(GuiGraphicsExtractor guiGraphics, Font font,
            int centerX, int y, int maxWidth, String label, boolean ready,
            boolean active, boolean emphasizeReady, int stateColor) {
        int textWidth = font.width(label);
        int width = Math.clamp(textWidth + 4, 10, maxWidth);
        int x = centerX - width / 2;
        guiGraphics.fill(x, y, x + width, y + KEY_BADGE_HEIGHT, 0xF0060709);
        int border = active ? stateColor
                : emphasizeReady && ready ? ARGB.multiplyAlpha(stateColor, 0.9f)
                        : ready ? ARGB.multiplyAlpha(stateColor, 0.38f) : 0xFF30343A;
        outline(guiGraphics, x, y, width, KEY_BADGE_HEIGHT, border);
        guiGraphics.text(font, label, centerX - textWidth / 2, y - 1,
                ready || active ? COLOR_TEXT : COLOR_TEXT_DIM, true);
    }

    private void renderSyncPending(GuiGraphicsExtractor guiGraphics, Font font,
            int panelX, int panelY, Theme theme) {
        renderPendingDots(guiGraphics, panelX + NORMAL_X + NORMAL_WIDTH / 2,
                panelY + NORMAL_Y + 12, COLOR_TEXT_DIM);
        renderKeyBadge(guiGraphics, font, panelX + NORMAL_X + NORMAL_WIDTH / 2,
                panelY + REGULAR_BADGE_Y, NORMAL_WIDTH - 2,
                skillLabel(SkillType.NORMAL_ATTACK), false, false, false, theme.frame());

        for (SkillType type : DISPLAY_SKILLS) {
            int size = slotSize(type);
            int x = panelX + slotOffsetX(type);
            int y = panelY + slotOffsetY(type);
            renderPendingDots(guiGraphics, x + size / 2, y + size / 2 - 2, COLOR_TEXT_DIM);
            renderKeyBadge(guiGraphics, font, x + size / 2,
                    panelY + (type == SkillType.ULTIMATE ? ULTIMATE_BADGE_Y : REGULAR_BADGE_Y),
                    size - 2, skillLabel(type), false, false, false, theme.frame());
        }

        renderUltimateRail(guiGraphics, panelX, panelY,
                new SkillDisplayData(SkillStateType.CONDITIONAL, 1f, 0, 0, false),
                theme, false, 0f);
    }

    private void renderPendingDots(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int color) {
        guiGraphics.fill(centerX - 4, centerY, centerX - 2, centerY + 2, color);
        guiGraphics.fill(centerX - 1, centerY, centerX + 1, centerY + 2, color);
        guiGraphics.fill(centerX + 2, centerY, centerX + 4, centerY + 2, color);
    }

    private SlotAnim updateSlotAnim(SkillType type, SkillDisplayData data, float time) {
        SlotAnim anim = slotAnims.computeIfAbsent(type, ignored -> new SlotAnim());
        float progress = data.mode() == SkillStateType.CONDITIONAL
                ? Math.clamp(1f - data.cdFraction(), 0f, 1f)
                : 0f;
        if (anim.initialized) {
            boolean released = switch (data.mode()) {
                case COOLDOWN -> anim.lastUsable && !data.usable();
                case INCREMENTAL_CHARGE, FULL_CHARGE -> data.currentCharges() < anim.lastCharges;
                case CONDITIONAL -> type == SkillType.ULTIMATE && anim.lastProgress - progress > 0.25f;
                case RESOURCE_COOLDOWN -> false;
            };
            if (released) anim.pressStart = time;
            if (!anim.lastUsable && data.usable()) anim.readyFlashStart = time;
            if (data.mode() == SkillStateType.CONDITIONAL
                    && Math.abs(anim.lastProgress - progress) > 0.001f) {
                anim.resourceTextUntil = time + RESOURCE_TEXT_TICKS;
            }
        }
        anim.initialized = true;
        anim.lastUsable = data.usable();
        anim.lastCharges = data.currentCharges();
        anim.lastProgress = progress;
        return anim;
    }

    private int pressOffset(SlotAnim anim, float time) {
        float elapsed = time - anim.pressStart;
        return elapsed >= 0f && elapsed < PRESS_TICKS / 2f ? 1 : 0;
    }

    private boolean isSkillActive(AbstractGauntletItem gauntlet, SkillType type, SkillDisplayData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        int playerId = mc.player.getId();
        boolean prime = gauntlet instanceof LegacyPrimeGauntletItem;
        boolean horus = gauntlet instanceof LegacyOfHorusGauntletItem;
        boolean finalAnswer = gauntlet instanceof FinalAnswerGauntletItem;
        boolean axiom = gauntlet instanceof TheAxiomGauntletItem;
        return switch (type) {
            case NORMAL_ATTACK -> finalAnswer && data.usable() && mc.options.keyAttack.isDown();
            case SKILL_ONE -> (prime && ClientMovementManager.isActiveTask(ClientRocketPunchTask.class))
                    || (horus && ClientMovementManager.isActiveTask(ClientEntityPushTask.class))
                    || ((finalAnswer || axiom) && mc.player.isUsingItem()
                            && mc.player.getUseItem().getItem() == gauntlet);
            case SKILL_TWO -> (prime && ClientMovementManager.isActiveTask(ClientEntityPushTask.class))
                    || (finalAnswer && ClientGauntletEffects.isFading(playerId))
                    || (axiom && ClientGauntletEffects.isKineticGraspActive(playerId));
            case SKILL_THREE -> prime && ClientMovementManager.isActiveTask(ClientSeismicSlamTask.class);
            case ULTIMATE -> (prime && (ClientMovementManager.isActiveTask(ClientMeteorHoverTask.class)
                    || ClientMovementManager.isActiveTask(ClientMeteorFallTask.class)))
                    || (horus && ClientGauntletEffects.isNanoSurgeActive(playerId))
                    || (finalAnswer && ClientGauntletEffects.isCoalescenceBeamActive(playerId))
                    || (axiom && ClientGauntletEffects.isGraviticFluxActive(playerId));
        };
    }

    private String skillLabel(SkillType type) {
        Minecraft mc = Minecraft.getInstance();
        return switch (type) {
            case NORMAL_ATTACK -> compactKeyLabel(mc.options.keyAttack, "LMB");
            case SKILL_ONE -> compactKeyLabel(mc.options.keyUse, "RMB");
            case SKILL_TWO -> compactKeyLabel(ClientInputHandler.SKILL_TWO_KEY, "Sft");
            case SKILL_THREE -> compactKeyLabel(ClientInputHandler.SKILL_THREE_KEY, "V");
            case ULTIMATE -> compactKeyLabel(ClientInputHandler.ULTIMATE_KEY, "X");
        };
    }

    private String compactKeyLabel(KeyMapping mapping, String fallback) {
        String raw = mapping.getTranslatedKeyMessage().getString();
        if (raw == null || raw.isBlank()) return fallback;
        String lower = raw.toLowerCase(Locale.ROOT);
        String saved = mapping.saveString().toLowerCase(Locale.ROOT);
        if (lower.contains("shift") || saved.contains("shift")) return "Sft";
        if (lower.contains("ctrl") || lower.contains("control") || saved.contains("control")) return "Ctrl";
        if (lower.contains("alt") || saved.contains("alt")) return "Alt";
        if (lower.contains("space") || saved.endsWith(".space")) return "Spc";
        if (lower.contains("mouse") || saved.startsWith("key.mouse.")) {
            if (lower.contains("left") || saved.endsWith(".left")) return "LMB";
            if (lower.contains("right") || saved.endsWith(".right")) return "RMB";
            if (lower.contains("middle") || saved.endsWith(".middle")) return "MMB";
            String button = saved.substring(saved.lastIndexOf('.') + 1);
            return button.chars().allMatch(Character::isDigit) ? "M" + button : "M?";
        }
        return raw.length() <= 4 ? raw : raw.substring(0, 4);
    }

    private String cooldownText(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds > 3.0) return Integer.toString((int) Math.ceil(seconds));
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private int slotOffsetX(SkillType type) {
        return switch (type) {
            case NORMAL_ATTACK -> NORMAL_X;
            case SKILL_ONE -> SKILL_ONE_X;
            case SKILL_TWO -> SKILL_TWO_X;
            case SKILL_THREE -> SKILL_THREE_X;
            case ULTIMATE -> ULTIMATE_X;
        };
    }

    private int slotOffsetY(SkillType type) {
        return type == SkillType.ULTIMATE ? ULTIMATE_Y : SKILL_Y;
    }

    private int slotSize(SkillType type) {
        return type == SkillType.ULTIMATE ? ULTIMATE_SIZE : SLOT_SIZE;
    }

    private int stateColor(Theme theme, SkillType type) {
        if (theme == FINAL_ANSWER_THEME && type == SkillType.NORMAL_ATTACK) return theme.resource();
        return type == SkillType.ULTIMATE ? theme.hot() : theme.accent();
    }

    private void renderSprite(GuiGraphicsExtractor guiGraphics, Identifier sprite,
            int x, int y, int width, int height, int color) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, color);
    }

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, Font font,
            String text, int centerX, int y, int color) {
        guiGraphics.centeredText(font, text, centerX + 1, y + 1, COLOR_TEXT_SHADOW);
        guiGraphics.centeredText(font, text, centerX, y, color);
    }

    private void drawCorners(GuiGraphicsExtractor guiGraphics, int x, int y,
            int width, int height, int color, int length) {
        guiGraphics.fill(x, y, x + length, y + 1, color);
        guiGraphics.fill(x, y, x + 1, y + length, color);
        guiGraphics.fill(x + width - length, y, x + width, y + 1, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + length, color);
        guiGraphics.fill(x, y + height - 1, x + length, y + height, color);
        guiGraphics.fill(x, y + height - length, x + 1, y + height, color);
        guiGraphics.fill(x + width - length, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x + width - 1, y + height - length, x + width, y + height, color);
    }

    private void outline(GuiGraphicsExtractor guiGraphics, int x, int y,
            int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void clearAnimationState() {
        lastGauntletId = null;
        slotAnims.clear();
    }

    private Theme themeFor(Identifier gauntletId) {
        return switch (gauntletId.getPath()) {
            case "legacy_prime" -> PRIME_THEME;
            case "legacy_of_horus" -> HORUS_THEME;
            case "final_answer" -> FINAL_ANSWER_THEME;
            case "the_axiom" -> AXIOM_THEME;
            default -> NEUTRAL_THEME;
        };
    }

    private static Theme theme(String key, int frame, int accent, int hot, int resource) {
        EnumMap<SkillType, Identifier> icons = new EnumMap<>(SkillType.class);
        icons.put(SkillType.SKILL_ONE, hudSprite("gauntlet/" + key + "/skill_one"));
        icons.put(SkillType.SKILL_TWO, hudSprite("gauntlet/" + key + "/skill_two"));
        icons.put(SkillType.SKILL_THREE, hudSprite("gauntlet/" + key + "/skill_three"));
        icons.put(SkillType.ULTIMATE, hudSprite("gauntlet/" + key + "/ultimate"));
        return new Theme(
                frame,
                accent,
                hot,
                resource,
                hudSprite("gauntlet/" + key + "/chassis"),
                Map.copyOf(icons));
    }

    private static Identifier hudSprite(String path) {
        return Identifier.fromNamespaceAndPath("olru", "hud/" + path);
    }

    private static final class SlotAnim {
        boolean initialized;
        boolean lastUsable = true;
        int lastCharges;
        float lastProgress;
        float pressStart = -1000f;
        float readyFlashStart = -1000f;
        float resourceTextUntil = -1000f;
    }

    private record Theme(
            int frame,
            int accent,
            int hot,
            int resource,
            Identifier chassis,
            Map<SkillType, Identifier> icons) {
        Identifier icon(SkillType type) {
            return icons.get(type);
        }
    }
}
