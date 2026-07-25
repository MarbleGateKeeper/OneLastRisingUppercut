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
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.common.item.FinalAnswerGauntletItem;
import dev.marblegate.olru.common.item.LegacyOfHorusGauntletItem;
import dev.marblegate.olru.common.item.LegacyPrimeGauntletItem;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import java.util.EnumMap;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.joml.Matrix3x2fStack;

/**
 * Bottom-center gauntlet skill bar: an LMB slot, three skill slots and a larger ultimate
 * slot, all fully procedural (no textures). Icons come from {@link GauntletHudIcons};
 * cooldowns are a radial spoke sweep, conditional/energy skills get a progress ring around
 * the slot, firing a skill triggers a short "release pop" scale animation, and a slot
 * becoming usable flashes its border instead of breathing all the time.
 */
public class GauntletHudRenderer implements GuiLayer {
    private static final int LMB_SLOT = 26;
    private static final int NORMAL_SLOT = 26;
    private static final int ULTIMATE_SLOT = 34;
    private static final int LMB_GAP = 8;
    private static final int SLOT_GAP = 5;
    private static final int PIP_SIZE = 3;
    private static final int PIP_GAP = 1;
    private static final int KEY_LABEL_GAP = 1;
    private static final int HUD_BOTTOM_OFFSET = 90;
    private static final int SWEEP_SPOKES = 48;
    private static final float POP_TICKS = 5f;
    private static final float READY_FLASH_TICKS = 16f;
    private static final float READY_FLASH_FULL_TICKS = 8f;
    private static final float DIM_FACTOR = 0.45f;
    private static final float ICON_DIM_FACTOR = 0.7f;

    private static final int TOTAL_WIDTH = LMB_SLOT + LMB_GAP + NORMAL_SLOT * 3 + ULTIMATE_SLOT + SLOT_GAP * 3;

    private static final int COLOR_SLOT_BG = 0xC00A0A14;
    private static final int COLOR_TOP_HIGHLIGHT = 0x22FFFFFF;
    private static final int COLOR_BOTTOM_SHADE = 0x33000000;
    private static final int COLOR_TEXT = 0xFFEFEFEF;
    private static final int COLOR_TEXT_SHADOW = 0x90000000;
    private static final int COLOR_KEY_LABEL = 0xFF6A6A6A;
    private static final int COLOR_COOLDOWN = 0x8C000000;
    private static final int COLOR_EMPTY_PIP = 0xFF333333;
    private static final int COLOR_ICON_SHADE = 0xFF6A6A6A;
    private static final int COLOR_ICON_OUTLINE = 0xF0101014;
    private static final int COLOR_BAR_BG = 0xDD050505;

    private static final Theme PRIME_THEME = theme(0xFF3A2412, 0xFFFFA028, 0xFFFFC94A, 0xFFFFD75A);
    private static final Theme HORUS_THEME = theme(0xFF122E36, 0xFF31E8FF, 0xFF9AEFFF, 0xFF7FE8C8);
    private static final Theme FINAL_ANSWER_THEME = theme(0xFF28132F, 0xFFB04AD8, 0xFFE0A0FF, 0xFFD8A0EC);
    private static final Theme NEUTRAL_THEME = theme(0xFF26262A, 0xFF9A9A9A, 0xFFCFCFCF, 0xFFB0B0B0);

    private final EnumMap<SkillType, SlotAnim> slotAnims = new EnumMap<>(SkillType.class);
    private Identifier lastGauntletId;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof AbstractGauntletItem gauntlet)) return;

        Identifier gauntletId = gauntlet.gauntletId();
        Theme theme = themeFor(gauntletId);
        float time = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true);

        int startX = (guiGraphics.guiWidth() - TOTAL_WIDTH) / 2;
        int startY = guiGraphics.guiHeight() - HUD_BOTTOM_OFFSET;

        GauntletSkillGroup group = gauntlet.getSyncedSkillGroup(mc.player);
        if (group == null) {
            slotAnims.clear();
            renderSyncPending(guiGraphics, mc.font, startX, startY, theme);
            return;
        }

        if (!gauntletId.equals(lastGauntletId)) {
            lastGauntletId = gauntletId;
            slotAnims.clear();
        }

        boolean charging = mc.player.isUsingItem()
                && mc.player.getUseItem().getItem() instanceof AbstractGauntletItem;
        float charge = 0f;
        if (charging) {
            int ticksHeld = gauntlet.getUseDuration(mc.player.getMainHandItem(), mc.player)
                    - mc.player.getUseItemRemainingTicks();
            charge = Math.min(1f, (float) ticksHeld / gauntlet.getMaxChargeTicks());
        }

        int skillOneX = 0;
        int x = startX;
        for (SkillType type : SkillType.values()) {
            int size = slotSize(type);
            int y = startY + (ULTIMATE_SLOT - size);
            if (type == SkillType.SKILL_ONE) skillOneX = x;
            SkillDisplayData data = group.get(type).displayData();
            renderSlot(guiGraphics, mc.font, x, y, size, type, data, theme, gauntletId,
                    charging && type == SkillType.SKILL_ONE, isSkillActive(gauntlet, type, data), time);
            x += size + gapAfter(type);
        }

        if (charging && gauntlet.isChargeProgressMeaningful()) {
            int skillOneY = startY + (ULTIMATE_SLOT - NORMAL_SLOT);
            renderProgressRing(guiGraphics, skillOneX + NORMAL_SLOT / 2f, skillOneY + NORMAL_SLOT / 2f,
                    ringRadius(NORMAL_SLOT), charge, theme.hot(), false);
        }
    }

    private void renderSlot(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int size,
            SkillType type, SkillDisplayData data, Theme theme, Identifier gauntletId,
            boolean charging, boolean active, float time) {
        boolean ready = data.usable();
        SlotAnim anim = updateSlotAnim(type, data, time);
        float popScale = popScale(anim, time);

        renderSlotFrame(guiGraphics, x, y, size, borderColor(theme, type, ready, active, charging, anim, time));

        boolean dim = !ready && !active && !charging;
        if (!dim) renderReadyUnderglow(guiGraphics, x, y, size, theme);

        int iconMain = dim ? ARGB.scaleRGB(theme.icon(), ICON_DIM_FACTOR) : theme.icon();
        int iconAccent = dim ? ARGB.scaleRGB(theme.accent(), ICON_DIM_FACTOR)
                : active || charging ? theme.hot() : theme.accent();
        int iconShade = dim ? ARGB.scaleRGB(COLOR_ICON_SHADE, ICON_DIM_FACTOR) : COLOR_ICON_SHADE;
        int iconOutline = dim ? ARGB.scaleRGB(COLOR_ICON_OUTLINE, ICON_DIM_FACTOR) : COLOR_ICON_OUTLINE;
        renderIcon(guiGraphics, gauntletId, type, x, y, size, iconMain, iconAccent, iconShade, iconOutline, popScale);

        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        switch (data.mode()) {
            case COOLDOWN -> renderCooldown(guiGraphics, font, x, y, size, data, centerX, centerY);
            case INCREMENTAL_CHARGE, FULL_CHARGE -> renderAmmoPips(guiGraphics, x, y, size, data, theme);
            case CONDITIONAL -> {
                // Gate on remaining progress, not usable(): resource bars (Final Answer energy)
                // are usable above zero but must still visualize their level while filling.
                if (data.cdFraction() > 0f) {
                    renderProgressRing(guiGraphics, centerX, centerY, ringRadius(size),
                            1f - data.cdFraction(), theme.accent(), true);
                }
            }
        }

        renderKeyLabel(guiGraphics, font, x, y, size, skillLabel(type));
    }

    private void renderSlotFrame(GuiGraphicsExtractor guiGraphics, int x, int y, int size, int borderColor) {
        guiGraphics.fill(x, y, x + size, y + size, borderColor);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, COLOR_SLOT_BG);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + 2, COLOR_TOP_HIGHLIGHT);
        guiGraphics.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, COLOR_BOTTOM_SHADE);
    }

    private void renderReadyUnderglow(GuiGraphicsExtractor guiGraphics, int x, int y, int size, Theme theme) {
        // fillGradient's first color is the top edge, second the bottom: the glow fades upward.
        guiGraphics.fillGradient(x + 1, y + size - 9, x + size - 1, y + size - 1,
                theme.accent() & 0x00FFFFFF, ARGB.multiplyAlpha(theme.accent(), 0.10f));
    }

    private int borderColor(Theme theme, SkillType type, boolean ready, boolean active, boolean charging,
            SlotAnim anim, float time) {
        if (active || charging) {
            return ARGB.multiplyAlpha(theme.accent(), 0.55f + 0.45f * (float) Math.sin(time * 0.35f));
        }
        if (ready) {
            float flashElapsed = time - anim.readyFlashStart;
            if (flashElapsed >= 0f && flashElapsed < READY_FLASH_TICKS) {
                int flashColor = type == SkillType.ULTIMATE ? theme.hot() : theme.accent();
                if (flashElapsed < READY_FLASH_FULL_TICKS) return flashColor;
                float t = (flashElapsed - READY_FLASH_FULL_TICKS) / READY_FLASH_FULL_TICKS;
                return ARGB.multiplyAlpha(flashColor, 0.9f - 0.15f * t);
            }
            return ARGB.multiplyAlpha(theme.accent(), 0.75f);
        }
        return theme.borderDim();
    }

    private void renderIcon(GuiGraphicsExtractor guiGraphics, Identifier gauntletId, SkillType type,
            int x, int y, int size, int main, int accent, int shade, int outline, float popScale) {
        GauntletHudIcons.GauntletSkillIcon icon = GauntletHudIcons.forGauntlet(gauntletId, type);
        int iconX = x + (size - GauntletHudIcons.ICON_SIZE) / 2;
        int iconY = y + (size - GauntletHudIcons.ICON_SIZE) / 2;
        if (popScale == 1f) {
            GauntletHudIcons.draw(guiGraphics, icon, iconX, iconY, main, accent, shade, outline);
            return;
        }
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(x + size / 2f, y + size / 2f);
        pose.scale(popScale, popScale);
        pose.translate(-(x + size / 2f), -(y + size / 2f));
        GauntletHudIcons.draw(guiGraphics, icon, iconX, iconY, main, accent, shade, outline);
        pose.popMatrix();
    }

    private void renderCooldown(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int size,
            SkillDisplayData data, float centerX, float centerY) {
        if (data.usable()) return;
        renderRadialSweep(guiGraphics, centerX, centerY, sweepRadius(size), data.cdFraction(), COLOR_COOLDOWN);
        if (data.remainingTicks() > 20) {
            String text = cooldownText(data.remainingTicks());
            guiGraphics.centeredText(font, text, x + size / 2 + 1, y + size / 2 - 4 + 1, COLOR_TEXT_SHADOW);
            guiGraphics.centeredText(font, text, x + size / 2, y + size / 2 - 4, COLOR_TEXT);
        }
    }

    private void renderAmmoPips(GuiGraphicsExtractor guiGraphics, int x, int y, int size,
            SkillDisplayData data, Theme theme) {
        int max = data.maxCharges();
        if (max <= 0) return;
        int rowWidth = max * PIP_SIZE + (max - 1) * PIP_GAP;
        int pipX = x + (size - rowWidth) / 2;
        int pipY = y + size - 6;
        for (int i = 0; i < max; i++) {
            int color = i < data.currentCharges() ? theme.resource() : COLOR_EMPTY_PIP;
            guiGraphics.fill(pipX, pipY, pipX + PIP_SIZE, pipY + PIP_SIZE, color);
            pipX += PIP_SIZE + PIP_GAP;
        }
        if (data.cdFraction() > 0f && data.currentCharges() < max) {
            int barX = x + (size - rowWidth) / 2;
            int barY = pipY + PIP_SIZE + 1;
            int fillW = (int) (rowWidth * (1f - data.cdFraction()));
            guiGraphics.fill(barX, barY, barX + rowWidth, barY + 1, COLOR_BAR_BG);
            if (fillW > 0) guiGraphics.fill(barX, barY, barX + fillW, barY + 1, theme.resource());
        }
    }

    private void renderSyncPending(GuiGraphicsExtractor guiGraphics, Font font, int startX, int startY, Theme theme) {
        int x = startX;
        for (SkillType type : SkillType.values()) {
            int size = slotSize(type);
            int y = startY + (ULTIMATE_SLOT - size);
            renderSlotFrame(guiGraphics, x, y, size, theme.border());
            guiGraphics.centeredText(font, "?", x + size / 2, y + size / 2 - 4, theme.iconDim());
            renderKeyLabel(guiGraphics, font, x, y, size, skillLabel(type));
            x += size + gapAfter(type);
        }
    }

    private void renderKeyLabel(GuiGraphicsExtractor guiGraphics, Font font, int x, int y, int size, String label) {
        guiGraphics.text(font, label, x + (size - font.width(label)) / 2, y + size + KEY_LABEL_GAP,
                COLOR_KEY_LABEL, false);
    }

    /**
     * Dark radial wipe used for cooldowns: {@code ceil(fraction * SWEEP_SPOKES)} 2px-wide spokes
     * fanning out from the slot center, starting at the top and going clockwise. Each spoke is a
     * rect drawn in rotated pose space, which the GUI renderer supports (vertices are transformed
     * by the pose matrix, so no per-spoke quad math is needed).
     */
    private void renderRadialSweep(GuiGraphicsExtractor guiGraphics, float centerX, float centerY,
            int radius, float fraction, int color) {
        int spokes = (int) Math.ceil(fraction * SWEEP_SPOKES);
        Matrix3x2fStack pose = guiGraphics.pose();
        for (int i = 0; i < spokes; i++) {
            pose.pushMatrix();
            pose.translate(centerX, centerY);
            pose.rotate((float) Math.toRadians(i * (360.0 / SWEEP_SPOKES)));
            guiGraphics.fill(-1, -radius, 1, 0, color);
            pose.popMatrix();
        }
    }

    /**
     * Progress ring hugging the outside of a slot frame, built from the same spoke technique.
     * Counterclockwise rings grow from the top towards the left (conditional charge-up);
     * clockwise rings grow towards the right (skill-one channel charge).
     */
    private void renderProgressRing(GuiGraphicsExtractor guiGraphics, float centerX, float centerY,
            int radius, float fraction, int color, boolean counterclockwise) {
        int spokes = (int) Math.ceil(Math.clamp(fraction, 0f, 1f) * SWEEP_SPOKES);
        Matrix3x2fStack pose = guiGraphics.pose();
        for (int i = 0; i < spokes; i++) {
            float angle = (float) Math.toRadians(i * (360.0 / SWEEP_SPOKES));
            pose.pushMatrix();
            pose.translate(centerX, centerY);
            pose.rotate(counterclockwise ? -angle : angle);
            guiGraphics.fill(-1, -radius, 1, -(radius - 2), color);
            pose.popMatrix();
        }
    }

    /**
     * Watches a slot's synced state for the two transition-driven animations: the release pop
     * (cooldown slot flips to unusable, or a charge pool loses a charge) and the ready flash
     * (slot becomes usable again). First observation records state only, so neither animation
     * fires on login, first sync or gauntlet switch (the anim map is cleared in those cases).
     */
    private SlotAnim updateSlotAnim(SkillType type, SkillDisplayData data, float time) {
        SlotAnim anim = slotAnims.computeIfAbsent(type, t -> new SlotAnim());
        if (anim.initialized) {
            boolean released = switch (data.mode()) {
                case COOLDOWN -> anim.lastUsable && !data.usable();
                case INCREMENTAL_CHARGE, FULL_CHARGE -> data.currentCharges() < anim.lastCharges;
                case CONDITIONAL -> false;
            };
            if (released) anim.popStart = time;
            if (!anim.lastUsable && data.usable()) anim.readyFlashStart = time;
        }
        anim.initialized = true;
        anim.lastUsable = data.usable();
        anim.lastCharges = data.currentCharges();
        return anim;
    }

    /** Ease-out-square pop scale: 1.18 back to 1.0 over {@link #POP_TICKS} ticks after firing. */
    private float popScale(SlotAnim anim, float time) {
        float elapsed = time - anim.popStart;
        if (elapsed < 0f || elapsed >= POP_TICKS) return 1f;
        float t = elapsed / POP_TICKS;
        return 1f + 0.18f * (1f - t) * (1f - t);
    }

    private boolean isSkillActive(AbstractGauntletItem gauntlet, SkillType type, SkillDisplayData data) {
        Minecraft mc = Minecraft.getInstance();
        boolean prime = gauntlet instanceof LegacyPrimeGauntletItem;
        boolean horus = gauntlet instanceof LegacyOfHorusGauntletItem;
        boolean finalAnswer = gauntlet instanceof FinalAnswerGauntletItem;
        return switch (type) {
            case NORMAL_ATTACK -> finalAnswer && data.usable() && mc.options.keyAttack.isDown();
            case SKILL_ONE -> prime && ClientMovementManager.isActiveTask(ClientRocketPunchTask.class)
                    || horus && ClientMovementManager.isActiveTask(ClientEntityPushTask.class)
                    || finalAnswer && mc.player.isUsingItem();
            case SKILL_TWO -> prime && ClientMovementManager.isActiveTask(ClientEntityPushTask.class)
                    || finalAnswer && ClientGauntletEffects.isFading(mc.player.getId());
            case SKILL_THREE -> prime && ClientMovementManager.isActiveTask(ClientSeismicSlamTask.class);
            case ULTIMATE -> prime && (ClientMovementManager.isActiveTask(ClientMeteorHoverTask.class)
                    || ClientMovementManager.isActiveTask(ClientMeteorFallTask.class))
                    || horus && mcPlayerHasNanoSurge()
                    || finalAnswer && ClientGauntletEffects.isCoalescenceBeamActive(mc.player.getId());
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

    private String cooldownText(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds >= 10.0) return Integer.toString((int) Math.ceil(seconds));
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private int slotSize(SkillType type) {
        return type == SkillType.ULTIMATE ? ULTIMATE_SLOT : NORMAL_SLOT;
    }

    private int gapAfter(SkillType type) {
        return switch (type) {
            case NORMAL_ATTACK -> LMB_GAP;
            case ULTIMATE -> 0;
            default -> SLOT_GAP;
        };
    }

    private int sweepRadius(int size) {
        return (size - 2) / 2 + 1;
    }

    private int ringRadius(int size) {
        return size / 2 + 3;
    }

    private Theme themeFor(Identifier gauntletId) {
        return switch (gauntletId.getPath()) {
            case "legacy_prime" -> PRIME_THEME;
            case "legacy_of_horus" -> HORUS_THEME;
            case "final_answer" -> FINAL_ANSWER_THEME;
            default -> NEUTRAL_THEME;
        };
    }

    private static Theme theme(int border, int accent, int hot, int resource) {
        return new Theme(border, 0xFF3A3A3A, 0xFFEFEFEF, ARGB.scaleRGB(0xFFEFEFEF, DIM_FACTOR),
                accent, hot, resource);
    }

    private static final class SlotAnim {
        boolean initialized;
        boolean lastUsable = true;
        int lastCharges;
        float popStart = -1000f;
        float readyFlashStart = -1000f;
    }

    private record Theme(int border, int borderDim, int icon, int iconDim, int accent, int hot, int resource) {}
}
