package dev.marblegate.olru.client.hud;

import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload.SkillType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Procedural pixel-map icons for the gauntlet skill HUD: each icon is a 16x16 grid of
 * characters ('.' empty, '#' main, '+' accent, '-' shade) rendered as 1x1 rectangles.
 * No texture assets are involved.
 */
public final class GauntletHudIcons {
    public static final int ICON_SIZE = 16;

    private GauntletHudIcons() {}

    public enum GauntletSkillIcon {
        HAND_CANNON(
                "................",
                "................",
                "......#.........",
                ".....###........",
                "....#####.+.....",
                "...#######++....",
                "..++#######++...",
                "..++#######++...",
                "...#######++....",
                "....#####.+.....",
                ".....###........",
                "......#.........",
                "................",
                "................",
                "................",
                "................"),
        ROCKET_PUNCH(
                "................",
                "................",
                "...++...######..",
                "..++++.########.",
                "..+++.#########.",
                "..+++.#########.",
                "..++++.########.",
                "...++...######..",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"),
        RISING_UPPERCUT(
                "......+.........",
                ".....+++........",
                "....+++++.......",
                "...+++++++......",
                "......+++.......",
                "......+++.......",
                "......+++.......",
                "....#######.....",
                "....#######.....",
                "....#######.....",
                ".....#####......",
                "................",
                "................",
                "................",
                "................",
                "................"),
        SEISMIC_SLAM(
                "......+++.......",
                "......+++.......",
                "......+++.......",
                "....#######.....",
                "....#######.....",
                "....#######.....",
                ".....#####......",
                "................",
                ".##...##...##...",
                ".###.###.###.##.",
                "################",
                "################",
                "................",
                "................",
                "................",
                "................"),
        METEOR_STRIKE(
                "................",
                "..........++....",
                ".........+++....",
                "........++##....",
                ".......++###....",
                "......++#####...",
                ".....+++#####...",
                ".....+++#####...",
                "....++++.###....",
                "....++..........",
                "...++...........",
                "................",
                "................",
                "................",
                "................",
                "................"),
        BIOTIC_ROUND(
                "................",
                "................",
                "................",
                ".....####++.....",
                "....######++....",
                "...########.....",
                "...########.....",
                "....######......",
                ".....####.......",
                "......++........",
                ".....++++.......",
                "......++........",
                "................",
                "................",
                "................",
                "................"),
        FIELD_EXTRACTION(
                "................",
                "................",
                "................",
                "...+........+...",
                "....+......+....",
                ".....+....+.....",
                "......+..+......",
                ".......##.......",
                ".......##.......",
                "......+..+......",
                ".....+....+.....",
                "....+......+....",
                "...+........+...",
                "................",
                "................",
                "................"),
        SEDATIVE_DART(
                "................",
                "................",
                "..#.............",
                "...##...........",
                "....#####.......",
                ".....########...",
                "......########..",
                ".......######...",
                "........++##....",
                ".......++..#....",
                "......++........",
                ".....++.........",
                "................",
                "................",
                "................",
                "................"),
        BIOTIC_GRENADE(
                "................",
                "................",
                "................",
                ".......+........",
                ".....#####......",
                "....##########..",
                "...##########...",
                "...##########...",
                "...##########...",
                "...##########...",
                "....########....",
                ".....#####......",
                "................",
                "................",
                "................",
                "................"),
        NANO_SURGE(
                "......##........",
                ".....####.......",
                "......##........",
                "....######......",
                "...+######+.....",
                "....######......",
                ".....####.......",
                ".....#..#.......",
                "....#....#......",
                "...+..+...+.....",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"),
        BIOTIC_SPRAY(
                "................",
                "................",
                "................",
                "....#####.......",
                "...#######......",
                "...#######......",
                "....#####++.....",
                ".....##..++.....",
                "......+...+.....",
                ".....+....+.....",
                "......+...+.....",
                "....+......+....",
                "................",
                "................",
                "................",
                "................"),
        BIOTIC_GRASP(
                "................",
                "................",
                "................",
                "....#....#......",
                "....##..##......",
                "....##..##......",
                "....######......",
                "....######......",
                ".....####.......",
                ".....####.......",
                "......##........",
                "......##........",
                "................",
                "................",
                "................",
                "................"),
        FADE(
                "................",
                "................",
                "......###..+....",
                ".....#####.++...",
                ".....#####......",
                "....######......",
                "....######+.....",
                ".....####.......",
                ".....####+......",
                "......##..+.....",
                "......#......+..",
                "................",
                "................",
                "................",
                "................",
                "................"),
        BIOTIC_ORB(
                "................",
                "................",
                "................",
                ".....#####......",
                "....#######.....",
                "...#########....",
                "..+#++###++#+...",
                "..+#++###++#+...",
                "...#########....",
                "....#######.....",
                ".....#####......",
                "................",
                "................",
                "................",
                "................",
                "................"),
        COALESCENCE(
                "................",
                "................",
                "................",
                "................",
                "..############..",
                ".++##++++##+++..",
                ".++##++++##+++..",
                "..############..",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................");

        private final String[] rows;

        GauntletSkillIcon(String... rows) {
            if (rows.length != ICON_SIZE) {
                throw new IllegalArgumentException("Skill icon must have 16 rows, got " + rows.length);
            }
            for (String row : rows) {
                if (row.length() != ICON_SIZE) {
                    throw new IllegalArgumentException("Skill icon row must be 16 chars: \"" + row + "\"");
                }
            }
            this.rows = rows;
        }
    }

    /** Maps a gauntlet + skill slot to its icon; unknown gauntlets fall back to the Legacy Prime set. */
    public static GauntletSkillIcon forGauntlet(Identifier gauntletId, SkillType type) {
        return switch (gauntletId.getPath()) {
            case "legacy_of_horus" -> switch (type) {
                case NORMAL_ATTACK -> GauntletSkillIcon.BIOTIC_ROUND;
                case SKILL_ONE -> GauntletSkillIcon.FIELD_EXTRACTION;
                case SKILL_TWO -> GauntletSkillIcon.SEDATIVE_DART;
                case SKILL_THREE -> GauntletSkillIcon.BIOTIC_GRENADE;
                case ULTIMATE -> GauntletSkillIcon.NANO_SURGE;
            };
            case "final_answer" -> switch (type) {
                case NORMAL_ATTACK -> GauntletSkillIcon.BIOTIC_SPRAY;
                case SKILL_ONE -> GauntletSkillIcon.BIOTIC_GRASP;
                case SKILL_TWO -> GauntletSkillIcon.FADE;
                case SKILL_THREE -> GauntletSkillIcon.BIOTIC_ORB;
                case ULTIMATE -> GauntletSkillIcon.COALESCENCE;
            };
            default -> switch (type) {
                case NORMAL_ATTACK -> GauntletSkillIcon.HAND_CANNON;
                case SKILL_ONE -> GauntletSkillIcon.ROCKET_PUNCH;
                case SKILL_TWO -> GauntletSkillIcon.RISING_UPPERCUT;
                case SKILL_THREE -> GauntletSkillIcon.SEISMIC_SLAM;
                case ULTIMATE -> GauntletSkillIcon.METEOR_STRIKE;
            };
        };
    }

    /**
     * Draws the icon at 1 rect per pixel; '.' cells are skipped. The silhouette is first stamped
     * in the outline color at four 1px offsets, giving the icon a 1px dark outline that keeps it
     * readable on bright backgrounds.
     */
    public static void draw(GuiGraphicsExtractor guiGraphics, GauntletSkillIcon icon, int x, int y,
            int main, int accent, int shade, int outline) {
        stamp(guiGraphics, icon, x - 1, y, outline);
        stamp(guiGraphics, icon, x + 1, y, outline);
        stamp(guiGraphics, icon, x, y - 1, outline);
        stamp(guiGraphics, icon, x, y + 1, outline);
        for (int row = 0; row < ICON_SIZE; row++) {
            String line = icon.rows[row];
            for (int col = 0; col < ICON_SIZE; col++) {
                int color = switch (line.charAt(col)) {
                    case '#' -> main;
                    case '+' -> accent;
                    case '-' -> shade;
                    default -> 0;
                };
                if (color != 0) {
                    guiGraphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    /** Fills every non-empty cell of the map in a single color, offset from the icon origin. */
    private static void stamp(GuiGraphicsExtractor guiGraphics, GauntletSkillIcon icon, int x, int y, int color) {
        for (int row = 0; row < ICON_SIZE; row++) {
            String line = icon.rows[row];
            for (int col = 0; col < ICON_SIZE; col++) {
                if (line.charAt(col) != '.') {
                    guiGraphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }
}
