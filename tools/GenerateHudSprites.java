import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Deterministically regenerates OLRU's forged HUD chassis and screen-edge sprites. */
public final class GenerateHudSprites {
    private static final Path ROOT = Path.of(
            "src/main/resources/assets/olru/textures/gui/sprites/hud");

    private static final int PANEL_WIDTH = 182;
    private static final int PANEL_HEIGHT = 44;
    private static final int NORMAL_X = 10;
    private static final int NORMAL_Y = 4;
    private static final int NORMAL_WIDTH = 24;
    private static final int SKILL_Y = 4;
    private static final int[] SKILL_X = {38, 68, 98};
    private static final int SLOT_SIZE = 27;
    private static final int ULTIMATE_X = 137;
    private static final int ULTIMATE_Y = 2;
    private static final int ULTIMATE_SIZE = 34;
    private static final int RAIL_X = 10;
    private static final int RAIL_Y = 38;
    private static final int RAIL_WIDTH = 118;
    private static final int RAIL_HEIGHT = 4;
    private static final int SEGMENTS = 10;

    private static final String[] ICON_NAMES = {
        "skill_one", "skill_two", "skill_three", "ultimate"
    };

    private static final Theme[] THEMES = {
        new Theme(
                "legacy_prime",
                0xFF080A0D,
                0xFF15191E,
                0xFF272D34,
                0xFF454B52,
                0xFF737980,
                0xFFFF8B26,
                0xFFFFC75A,
                0xFFE46B1F,
                0xFFF1E8D7,
                0xFF8F8578),
        new Theme(
                "legacy_of_horus",
                0xFF091114,
                0xFF263438,
                0xFF465559,
                0xFF788589,
                0xFFB7C0BE,
                0xFF35DEEE,
                0xFFF2D263,
                0xFF64E1C5,
                0xFFE5FAF8,
                0xFF789A99),
        new Theme(
                "final_answer",
                0xFF08050C,
                0xFF171021,
                0xFF302043,
                0xFF5A3B72,
                0xFF8D6BA8,
                0xFFB84FE4,
                0xFFFFD15B,
                0xFFFFC843,
                0xFFE8DDF0,
                0xFF806D8D),
        new Theme(
                "the_axiom",
                0xFF07080D,
                0xFF141622,
                0xFF292D40,
                0xFF50566F,
                0xFF9299B0,
                0xFF9E70FF,
                0xFFE2DAFF,
                0xFFB187FF,
                0xFFF0F0F5,
                0xFF85889A)
    };

    private GenerateHudSprites() {}

    public static void main(String[] args) throws IOException {
        for (Theme theme : THEMES) {
            Path directory = ROOT.resolve("gauntlet").resolve(theme.key());
            write(directory.resolve("chassis.png"), chassis(theme));
        }

        Path effectDirectory = ROOT.resolve("effects");
        write(effectDirectory.resolve("fade_corner.png"),
                cornerTexture(0xFF5B1B83, 0xFF12051C, 144, 5));
        write(effectDirectory.resolve("sedation_corner.png"),
                cornerTexture(0xFF6085A4, 0xFF08111D, 126, 7));
        write(Path.of("build/olru-hud-chassis-preview.png"), preview());
    }

    private static BufferedImage chassis(Theme theme) {
        BufferedImage image = image(PANEL_WIDTH, PANEL_HEIGHT);

        // Drop shadow and side wings make one connected silhouette instead of five loose cards.
        fillChamfered(image, 3, 3, 176, 41, 4, 0x72000000);
        drawWing(image, 0, false, theme);
        drawWing(image, 168, true, theme);

        fillChamfered(image, 7, 1, 168, 42, 4, theme.base0());
        fillChamfered(image, 8, 2, 166, 40, 3, theme.edge());
        fillChamfered(image, 9, 3, 164, 38, 2, theme.base2());
        fill(image, 11, 4, 158, 32, theme.base1());
        fill(image, 11, 4, 158, 1, theme.highlight());
        fill(image, 10, 34, 160, 2, theme.base0());

        // Raised ultimate tower, matching the heavier right-hand block in the concept art.
        fillChamfered(image, 133, 1, 43, 39, 4, 0x8A000000);
        fillChamfered(image, 134, 0, 41, 39, 4, theme.base0());
        fillChamfered(image, 135, 1, 39, 37, 3, theme.edge());
        fillChamfered(image, 136, 2, 37, 35, 2, theme.base2());

        materialNoise(image, theme);
        drawRecess(image, NORMAL_X, NORMAL_Y, NORMAL_WIDTH, 27, false, theme);
        for (int x : SKILL_X) {
            drawRecess(image, x, SKILL_Y, SLOT_SIZE, SLOT_SIZE, false, theme);
        }
        drawRecess(image, ULTIMATE_X, ULTIMATE_Y,
                ULTIMATE_SIZE, ULTIMATE_SIZE, true, theme);

        drawBadgeMount(image, NORMAL_X, 28, NORMAL_WIDTH, false, theme);
        for (int x : SKILL_X) {
            drawBadgeMount(image, x, 28, SLOT_SIZE, false, theme);
        }
        drawBadgeMount(image, ULTIMATE_X, 33, ULTIMATE_SIZE, true, theme);

        drawRailGroove(image, theme);
        drawTopMeterGroove(image, theme);
        drawThemeMotif(image, theme);

        rivet(image, 8, 3, theme);
        rivet(image, 171, 3, theme);
        rivet(image, 8, 39, theme);
        rivet(image, 171, 37, theme);
        rivet(image, 134, 3, theme);
        rivet(image, 171, 32, theme);
        return image;
    }

    private static void drawWing(BufferedImage image, int x, boolean right, Theme theme) {
        fillChamfered(image, x, 8, 14, 28, 5, 0x70000000);
        fillChamfered(image, x, 7, 14, 28, 5, theme.base0());
        fillChamfered(image, x + 1, 8, 12, 26, 4, theme.edge());
        fillChamfered(image, x + 2, 9, 10, 24, 3, theme.base1());
        int seamX = right ? x + 2 : x + 11;
        fill(image, seamX, 12, 1, 16, theme.base0());
        pixel(image, seamX, 10, theme.highlight());
        pixel(image, seamX, 30, theme.accent());
    }

    private static void drawRecess(BufferedImage image, int x, int y,
            int width, int height, boolean ultimate, Theme theme) {
        int chamfer = ultimate ? 3 : 2;
        fillChamfered(image, x, y + 1, width, height, chamfer, 0x92000000);
        fillChamfered(image, x, y, width, height - 1, chamfer, theme.base0());
        fillChamfered(image, x + 1, y + 1, width - 2, height - 3,
                Math.max(1, chamfer - 1), theme.highlight());
        fillChamfered(image, x + 2, y + 2, width - 4, height - 5, 1, theme.edge());
        int innerHeight = height - (ultimate ? 8 : 7);
        fillChamfered(image, x + 3, y + 3, width - 6, innerHeight, 1, theme.base0());
        fill(image, x + 4, y + 4, width - 8, innerHeight - 3, 0xFF05070A);
        fill(image, x + 4, y + 4, width - 8, 1, theme.base2());
        fill(image, x + 4, y + innerHeight, width - 8, 1, 0xFF020305);

        if (ultimate) {
            pixel(image, x + 2, y + 5, theme.hot());
            pixel(image, x + width - 3, y + 5, theme.hot());
            pixel(image, x + 2, y + height - 6, theme.accent());
            pixel(image, x + width - 3, y + height - 6, theme.accent());
        } else {
            pixel(image, x + 2, y + 4, theme.accent());
            pixel(image, x + width - 3, y + height - 6, theme.accent());
        }
    }

    private static void drawBadgeMount(BufferedImage image, int x, int y,
            int width, boolean ultimate, Theme theme) {
        int mountWidth = ultimate ? 18 : 14;
        int mountX = x + (width - mountWidth) / 2;
        int mountHeight = ultimate ? 10 : 9;
        fillChamfered(image, mountX, y, mountWidth, mountHeight, 2, theme.base0());
        fillChamfered(image, mountX + 1, y + 1, mountWidth - 2,
                mountHeight - 2, 1, theme.edge());
        fill(image, mountX + 2, y + 2, mountWidth - 4,
                mountHeight - 4, 0xFF07080A);
        fill(image, mountX + 3, y + 2, mountWidth - 6, 1, theme.base2());
    }

    private static void drawRailGroove(BufferedImage image, Theme theme) {
        fillChamfered(image, 8, 36, 122, 8, 2, theme.base0());
        fillChamfered(image, 9, 37, 120, 6, 1, theme.edge());
        fill(image, RAIL_X, RAIL_Y, RAIL_WIDTH, RAIL_HEIGHT, 0xFF090A0D);
        fill(image, RAIL_X, RAIL_Y, RAIL_WIDTH, 1, theme.base2());
        for (int i = 1; i < SEGMENTS; i++) {
            int x = RAIL_X + i * RAIL_WIDTH / SEGMENTS - 1;
            fill(image, x, RAIL_Y, 1, RAIL_HEIGHT, theme.base0());
        }
        pixel(image, 8, 38, theme.accent());
        pixel(image, 128, 38, theme.hot());
    }

    private static void drawTopMeterGroove(BufferedImage image, Theme theme) {
        fillChamfered(image, 37, 0, 29, 4, 2, theme.base0());
        fill(image, 39, 1, 25, 2, 0xFF090A0D);
        for (int i = 1; i < SEGMENTS; i++) {
            int x = 39 + i * 25 / SEGMENTS - 1;
            pixel(image, x, 1, theme.base2());
            pixel(image, x, 2, theme.base0());
        }
    }

    private static void materialNoise(BufferedImage image, Theme theme) {
        int seed = switch (theme.key()) {
            case "legacy_prime" -> 3;
            case "legacy_of_horus" -> 7;
            case "final_answer" -> 11;
            case "the_axiom" -> 17;
            default -> 1;
        };
        for (int y = 5; y < 35; y++) {
            for (int x = 11; x < 169; x++) {
                int hash = x * 73 ^ y * 151 ^ seed * 991;
                hash ^= hash >>> 7;
                if ((hash & 63) == 0) pixel(image, x, y, theme.base2());
                if ((hash & 127) == 1) pixel(image, x, y, theme.edge());
            }
        }
    }

    private static void drawThemeMotif(BufferedImage image, Theme theme) {
        switch (theme.key()) {
            case "legacy_prime" -> drawPrimeMotif(image, theme);
            case "legacy_of_horus" -> drawHorusMotif(image, theme);
            case "final_answer" -> drawFinalAnswerMotif(image, theme);
            case "the_axiom" -> drawAxiomMotif(image, theme);
            default -> {}
        }
    }

    private static void drawPrimeMotif(BufferedImage image, Theme theme) {
        int[][] leftCrack = {{4, 13}, {5, 14}, {5, 16}, {4, 17}, {5, 19}, {5, 22},
                {4, 24}, {5, 26}, {4, 28}};
        for (int[] p : leftCrack) pixel(image, p[0], p[1], theme.accent());
        int[][] rightCrack = {{176, 12}, {175, 14}, {176, 16}, {176, 19}, {175, 21},
                {176, 23}, {175, 26}, {176, 29}};
        for (int[] p : rightCrack) pixel(image, p[0], p[1], theme.accent());
        fill(image, 83, 3, 5, 1, theme.base0());
        pixel(image, 82, 4, theme.hot());
        fill(image, 127, 20, 5, 2, theme.base0());
        pixel(image, 128, 19, theme.accent());
        pixel(image, 130, 22, theme.hot());
    }

    private static void drawHorusMotif(BufferedImage image, Theme theme) {
        engravedCross(image, 5, 20, theme.hot(), theme.base0());
        engravedCross(image, 176, 20, theme.hot(), theme.base0());
        fill(image, 79, 3, 10, 1, theme.accent());
        fill(image, 126, 12, 1, 14, theme.accent());
        pixel(image, 127, 11, theme.hot());
        pixel(image, 127, 27, theme.hot());
        pixel(image, 81, 2, theme.hot());
        pixel(image, 87, 2, theme.hot());
    }

    private static void drawFinalAnswerMotif(BufferedImage image, Theme theme) {
        int[] tears = {4, 7, 128, 176};
        for (int i = 0; i < tears.length; i++) {
            int x = tears[i];
            int y = 12 + i % 2;
            fill(image, x, y, 1, 7 + i % 3, theme.edge());
            pixel(image, x, y + 8 + i % 3, theme.accent());
            pixel(image, x + (i % 2 == 0 ? 1 : -1), y + 10 + i % 3, theme.accent());
        }
        fill(image, 79, 3, 6, 1, theme.hot());
        fill(image, 85, 3, 6, 1, theme.accent());
        pixel(image, 5, 30, theme.hot());
        pixel(image, 176, 30, theme.accent());
    }

    private static void drawAxiomMotif(BufferedImage image, Theme theme) {
        gravityDiamond(image, 5, 20, theme.accent(), theme.iconMain());
        gravityDiamond(image, 176, 20, theme.accent(), theme.iconMain());
        gravityDiamond(image, 130, 20, theme.accent(), theme.iconMain());
        fill(image, 80, 3, 10, 1, theme.iconMain());
        pixel(image, 78, 3, theme.accent());
        pixel(image, 92, 3, theme.accent());
        pixel(image, 130, 13, theme.iconMain());
        pixel(image, 130, 27, theme.iconMain());
    }

    private static void engravedCross(BufferedImage image, int x, int y, int color, int shadow) {
        fill(image, x - 1, y - 3, 3, 7, shadow);
        fill(image, x - 3, y - 1, 7, 3, shadow);
        fill(image, x, y - 2, 1, 5, color);
        fill(image, x - 2, y, 5, 1, color);
    }

    private static void gravityDiamond(BufferedImage image, int x, int y, int color, int core) {
        pixel(image, x, y - 3, core);
        pixel(image, x - 1, y - 2, color);
        pixel(image, x + 1, y - 2, color);
        pixel(image, x - 2, y - 1, color);
        pixel(image, x + 2, y - 1, color);
        pixel(image, x - 1, y, color);
        pixel(image, x + 1, y, color);
        pixel(image, x, y + 1, core);
    }

    private static BufferedImage preview() throws IOException {
        int rowHeight = 56;
        BufferedImage nativePreview = image(PANEL_WIDTH, rowHeight * THEMES.length);
        for (int themeIndex = 0; themeIndex < THEMES.length; themeIndex++) {
            Theme theme = THEMES[themeIndex];
            int rowY = themeIndex * rowHeight;
            drawPreviewBackground(nativePreview, rowY, rowHeight);
            int panelY = rowY + 6;
            blit(nativePreview, chassis(theme), 0, panelY);
            drawPreviewNormalStatus(nativePreview, panelY, theme);
            drawPreviewTopMeter(nativePreview, panelY, theme, 0.68f);
            drawPreviewUltimateRail(nativePreview, panelY, theme, 0.72f);

            Path directory = ROOT.resolve("gauntlet").resolve(theme.key());
            for (int i = 0; i < ICON_NAMES.length; i++) {
                BufferedImage icon = readRequired(directory.resolve(ICON_NAMES[i] + ".png"));
                boolean ultimate = i == ICON_NAMES.length - 1;
                int x = ultimate ? ULTIMATE_X : SKILL_X[i];
                int y = panelY + (ultimate ? ULTIMATE_Y + 7 : SKILL_Y + 4);
                int size = ultimate ? ULTIMATE_SIZE : SLOT_SIZE;
                blit(nativePreview, icon, x + (size - 16) / 2, y);
            }

            drawPreviewKey(nativePreview, NORMAL_X + NORMAL_WIDTH / 2, panelY + 29, 'L', theme);
            drawPreviewKey(nativePreview, SKILL_X[0] + SLOT_SIZE / 2, panelY + 29, 'R', theme);
            drawPreviewKey(nativePreview, SKILL_X[1] + SLOT_SIZE / 2, panelY + 29, 'S', theme);
            drawPreviewKey(nativePreview, SKILL_X[2] + SLOT_SIZE / 2, panelY + 29, 'V', theme);
            drawPreviewKey(nativePreview, ULTIMATE_X + ULTIMATE_SIZE / 2, panelY + 34, 'X', theme);
        }
        return scaleNearest(nativePreview, 4);
    }

    private static void drawPreviewBackground(BufferedImage image, int rowY, int rowHeight) {
        fill(image, 0, rowY, PANEL_WIDTH, rowHeight, 0xFF526068);
        for (int y = rowY; y < rowY + rowHeight; y += 4) {
            for (int x = (y / 4 & 1) * 4; x < PANEL_WIDTH; x += 8) {
                fill(image, x, y, 4, 4, 0xFF4B585F);
            }
        }
    }

    private static void drawPreviewNormalStatus(BufferedImage image, int panelY, Theme theme) {
        if (theme.key().equals("final_answer")) {
            for (int i = 0; i < SEGMENTS; i++) {
                int color = i < 7 ? theme.resource() : 0xFF202329;
                fill(image, NORMAL_X + 2 + i * 2, panelY + NORMAL_Y + 8, 1, 7, color);
            }
            return;
        }
        int centerX = NORMAL_X + NORMAL_WIDTH / 2;
        for (int i = 0; i < 6; i++) {
            int row = i / 4;
            int rowCount = row == 0 ? 4 : 2;
            int column = i % 4;
            int rowWidth = rowCount * 3 + (rowCount - 1) * 2;
            int x = centerX - rowWidth / 2 + column * 5;
            int y = panelY + NORMAL_Y + 7 + row * 6;
            fill(image, x, y, 3, 3, i < 5 ? theme.resource() : 0xFF202329);
            if (i < 5) fill(image, x + 1, y, 2, 1, theme.hot());
        }
    }

    private static void drawPreviewTopMeter(BufferedImage image, int panelY, Theme theme, float progress) {
        int x = SKILL_X[0] + 1;
        int y = panelY + 1;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + i * 25 / SEGMENTS;
            int nextX = x + (i + 1) * 25 / SEGMENTS;
            int width = Math.max(1, nextX - segmentX - 1);
            fill(image, segmentX, y, width, 2,
                    i < progress * SEGMENTS ? theme.accent() : 0xFF202329);
        }
    }

    private static void drawPreviewUltimateRail(BufferedImage image, int panelY, Theme theme, float progress) {
        int y = panelY + RAIL_Y;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = RAIL_X + i * RAIL_WIDTH / SEGMENTS;
            int nextX = RAIL_X + (i + 1) * RAIL_WIDTH / SEGMENTS;
            int width = Math.max(1, nextX - segmentX - 1);
            if (i >= progress * SEGMENTS) {
                fill(image, segmentX, y, width, RAIL_HEIGHT, 0xFF202329);
            } else if (theme.key().equals("final_answer")) {
                fill(image, segmentX, y, width, 2, theme.hot());
                fill(image, segmentX, y + 2, width, 2, theme.accent());
            } else {
                fill(image, segmentX, y, width, RAIL_HEIGHT, theme.accent());
                fill(image, segmentX, y, width, 1, theme.hot());
            }
        }
    }

    private static void drawPreviewKey(BufferedImage image, int centerX, int y,
            char key, Theme theme) {
        String[] glyph = switch (key) {
            case 'L' -> new String[] {"100", "100", "100", "100", "111"};
            case 'R' -> new String[] {"110", "101", "110", "101", "101"};
            case 'S' -> new String[] {"111", "100", "111", "001", "111"};
            case 'V' -> new String[] {"101", "101", "101", "101", "010"};
            case 'X' -> new String[] {"101", "101", "010", "101", "101"};
            default -> throw new IllegalArgumentException("Unsupported preview key " + key);
        };
        fill(image, centerX - 4, y, 8, 8, 0xFF060709);
        outline(image, centerX - 4, y, 8, 8, theme.edge());
        for (int gy = 0; gy < glyph.length; gy++) {
            for (int gx = 0; gx < glyph[gy].length(); gx++) {
                if (glyph[gy].charAt(gx) == '1') {
                    pixel(image, centerX - 1 + gx, y + 1 + gy, theme.iconMain());
                }
            }
        }
    }

    private static BufferedImage readRequired(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Missing placeholder icon " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Unreadable PNG " + path);
        if (image.getWidth() != 16 || image.getHeight() != 16) {
            throw new IOException("Icon must be 16x16: " + path);
        }
        return image;
    }

    private static BufferedImage cornerTexture(int rgb, int shadowRgb, int maxAlpha, int seed) {
        BufferedImage image = image(64, 64);
        for (int blockY = 0; blockY < 32; blockY++) {
            for (int blockX = 0; blockX < 32; blockX++) {
                int x = blockX * 2;
                int y = blockY * 2;
                double radial = Math.sqrt(x * x + y * y) / 76.0;
                double edge = Math.max(1.0 - x / 31.0, 1.0 - y / 31.0);
                double strength = Math.max(0.0, Math.max(1.0 - radial, edge * 0.72));
                int hash = blockX * 0x45D9F3B ^ blockY * 0x119DE1F3 ^ seed * 0x3449D;
                hash ^= hash >>> 16;
                hash *= 0x45D9F3B;
                hash ^= hash >>> 16;
                int sample = hash & 0xFF;
                if (sample > (int) (strength * 232.0)) continue;
                int alphaStep = 2 + ((hash >>> 9) & 3);
                int alpha = Math.clamp((int) (maxAlpha * strength * alphaStep / 5.0), 0, maxAlpha);
                double colorStrength = ((hash >>> 13) & 3) == 0 ? strength * 0.45 : strength * 1.2;
                int color = blendRgb(shadowRgb, rgb, Math.min(1.0, colorStrength));
                fill(image, x, y, 2, 2, (alpha << 24) | (color & 0x00FFFFFF));
            }
        }
        return image;
    }

    private static BufferedImage scaleNearest(BufferedImage source, int scale) {
        BufferedImage result = image(source.getWidth() * scale, source.getHeight() * scale);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                fill(result, x * scale, y * scale, scale, scale, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static void blit(BufferedImage target, BufferedImage source, int targetX, int targetY) {
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int sourceColor = source.getRGB(x, y);
                int sourceAlpha = sourceColor >>> 24;
                if (sourceAlpha == 0) continue;
                if (sourceAlpha == 255) {
                    pixel(target, targetX + x, targetY + y, sourceColor);
                    continue;
                }
                int targetColor = target.getRGB(targetX + x, targetY + y);
                int inverseAlpha = 255 - sourceAlpha;
                int red = (((sourceColor >> 16) & 0xFF) * sourceAlpha
                        + ((targetColor >> 16) & 0xFF) * inverseAlpha) / 255;
                int green = (((sourceColor >> 8) & 0xFF) * sourceAlpha
                        + ((targetColor >> 8) & 0xFF) * inverseAlpha) / 255;
                int blue = ((sourceColor & 0xFF) * sourceAlpha
                        + (targetColor & 0xFF) * inverseAlpha) / 255;
                pixel(target, targetX + x, targetY + y,
                        0xFF000000 | red << 16 | green << 8 | blue);
            }
        }
    }

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private static void write(Path path, BufferedImage image) throws IOException {
        Files.createDirectories(path.getParent());
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("No PNG writer for " + path);
        }
    }

    private static void rivet(BufferedImage image, int x, int y, Theme theme) {
        pixel(image, x, y, theme.base0());
        pixel(image, x + 1, y, theme.highlight());
        pixel(image, x, y + 1, theme.edge());
        pixel(image, x + 1, y + 1, theme.accent());
    }

    private static void pixel(BufferedImage image, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
        image.setRGB(x, y, color);
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int py = Math.max(0, y); py < Math.min(image.getHeight(), y + height); py++) {
            for (int px = Math.max(0, x); px < Math.min(image.getWidth(), x + width); px++) {
                image.setRGB(px, py, color);
            }
        }
    }

    private static void outline(BufferedImage image, int x, int y, int width, int height, int color) {
        fill(image, x, y, width, 1, color);
        fill(image, x, y + height - 1, width, 1, color);
        fill(image, x, y, 1, height, color);
        fill(image, x + width - 1, y, 1, height, color);
    }

    private static void fillChamfered(
            BufferedImage image, int x, int y, int width, int height, int chamfer, int color) {
        for (int row = 0; row < height; row++) {
            int edgeDistance = Math.min(row, height - 1 - row);
            int inset = Math.max(0, chamfer - edgeDistance);
            fill(image, x + inset, y + row, width - inset * 2, 1, color);
        }
    }

    private static int blendRgb(int from, int to, double amount) {
        amount = Math.clamp(amount, 0.0, 1.0);
        int red = (int) (((from >> 16) & 0xFF) * (1.0 - amount)
                + ((to >> 16) & 0xFF) * amount);
        int green = (int) (((from >> 8) & 0xFF) * (1.0 - amount)
                + ((to >> 8) & 0xFF) * amount);
        int blue = (int) ((from & 0xFF) * (1.0 - amount) + (to & 0xFF) * amount);
        return (red << 16) | (green << 8) | blue;
    }

    private record Theme(
            String key,
            int base0,
            int base1,
            int base2,
            int edge,
            int highlight,
            int accent,
            int hot,
            int resource,
            int iconMain,
            int iconShade) {}
}
