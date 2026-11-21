package dev.aurora.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сглаженный шрифт интерфейса: глифы рисуются AWT в атлас и грузятся текстурой.
 *
 * Особенность 1.21.11: DrawContext.drawTexture не умеет тонировать, поэтому
 * атлас собирается под каждый цвет палитры, а произвольный цвет приближается
 * к ближайшему. Атлас рисуется в 1:1 (размер шрифта = размер на экране),
 * чтобы не терять резкость при масштабировании.
 */
public final class SmoothFont {

    private static final int BASE = 11;
    private static final int ATLAS_WIDTH = 1024;
    private static final int PAD = 2;

    private static final int[] PALETTE = {
            0xFFFFFFFF,
            0xFFE9F9F0,
            0xFF8AAAA0,
            0xFF3BE8A0,
            0xFF40D6D6,
            0xFF8CFFC8,
            0xFFB6FF8C,
            0xFFFFC24A,
            0xFFFF5A6E,
            0xFF000000,
    };

    private record Glyph(int u, int v, int w, int h, int advance) {
    }

    private static boolean ready;
    private static boolean failed;
    private static int atlasHeight;
    private static int lineHeight;
    private static final Map<Character, Glyph> glyphs = new HashMap<>();
    private static final Map<Integer, Identifier> atlases = new LinkedHashMap<>();

    private SmoothFont() {
    }

    public static boolean isReady() {
        ensureInit();
        return ready;
    }

    public static int lineHeight() {
        ensureInit();
        return lineHeight;
    }

    public static int width(String text) {
        ensureInit();
        if (!ready || text == null || text.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            if (glyph != null) {
                width += glyph.advance();
            }
        }
        return width;
    }

    private static void ensureInit() {
        if (ready || failed) {
            return;
        }
        try {
            build();
            ready = true;
        } catch (Throwable t) {
            failed = true;
            ready = false;
        }
    }

    private static Font loadFont() {
        String[] candidates = {
                "C:\\Windows\\Fonts\\segoeui.ttf",
                "C:\\Windows\\Fonts\\tahoma.ttf",
        };
        for (String path : candidates) {
            try {
                Path file = Path.of(path);
                if (Files.exists(file)) {
                    return Font.createFont(Font.TRUETYPE_FONT, file.toFile()).deriveFont(Font.PLAIN, (float) BASE);
                }
            } catch (Throwable ignored) {
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, BASE);
    }

    private static void build() {
        Font font = loadFont();

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(font);
        probeGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics metrics = probeGraphics.getFontMetrics();
        lineHeight = metrics.getHeight();
        probeGraphics.dispose();

        StringBuilder chars = new StringBuilder();
        for (char c = 32; c < 127; c++) {
            chars.append(c);
        }
        for (char c = 0x400; c < 0x460; c++) {
            chars.append(c);
        }
        chars.append("«»…—·›×＋•№");

        int maxAdvance = 0;
        for (int i = 0; i < chars.length(); i++) {
            maxAdvance = Math.max(maxAdvance, metrics.charWidth(chars.charAt(i)));
        }
        int cellWidth = maxAdvance + PAD * 2;
        int cellHeight = lineHeight + PAD * 2;

        int cols = Math.max(1, ATLAS_WIDTH / cellWidth);
        int rows = (chars.length() + cols - 1) / cols;
        atlasHeight = Math.max(64, rows * cellHeight);

        int index = 0;
        for (int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            if (glyphs.containsKey(ch)) {
                continue;
            }
            int col = index % cols;
            int row = index / cols;
            int x = col * cellWidth;
            int y = row * cellHeight;
            int advance = Math.max(2, metrics.charWidth(ch));
            glyphs.put(ch, new Glyph(x, y, advance, lineHeight, advance));
            index++;
        }

        for (int color : PALETTE) {
            BufferedImage image = new BufferedImage(ATLAS_WIDTH, atlasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setFont(font);
            graphics.setColor(new Color(color, true));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            for (Map.Entry<Character, Glyph> entry : glyphs.entrySet()) {
                Glyph glyph = entry.getValue();
                graphics.drawString(String.valueOf(entry.getKey()),
                        glyph.u(), glyph.v() + PAD + metrics.getAscent());
            }
            graphics.dispose();

            NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, ATLAS_WIDTH, atlasHeight, false);
            for (int y = 0; y < atlasHeight; y++) {
                for (int x = 0; x < ATLAS_WIDTH; x++) {
                    nativeImage.setColorArgb(x, y, image.getRGB(x, y));
                }
            }

            Identifier id = Identifier.of("aurora", "smooth_font_" + Integer.toHexString(color));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id,
                    new NativeImageBackedTexture(() -> "aurora smooth font", nativeImage));
            atlases.put(color, id);
        }
    }

    private static Identifier atlasFor(int color) {
        if (!ready || atlases.isEmpty()) {
            return null;
        }
        int rgb = color & 0x00FFFFFF;
        int best = Integer.MAX_VALUE;
        Identifier bestId = null;
        for (Map.Entry<Integer, Identifier> entry : atlases.entrySet()) {
            int candidate = entry.getKey();
            int dr = ((candidate >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
            int dg = ((candidate >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
            int db = (candidate & 0xFF) - (rgb & 0xFF);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < best) {
                best = distance;
                bestId = entry.getValue();
            }
        }
        return bestId;
    }

    public static void draw(DrawContext context, String text, float x, float y, int color) {
        ensureInit();
        if (!ready || text == null || text.isEmpty()) {
            return;
        }
        Identifier atlas = atlasFor(color);
        if (atlas == null) {
            return;
        }

        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Glyph glyph = glyphs.get(ch);
            if (glyph == null) {
                cursor += 3f;
                continue;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, atlas,
                    Math.round(cursor), Math.round(y),
                    (float) glyph.u(), (float) glyph.v(),
                    Math.max(1, glyph.w()), Math.max(1, glyph.h()),
                    ATLAS_WIDTH, atlasHeight);
            cursor += glyph.advance();
        }
    }
}
