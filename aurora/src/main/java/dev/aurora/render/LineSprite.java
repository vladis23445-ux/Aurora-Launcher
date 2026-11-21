package dev.aurora.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Гладкие линии вместо «пиксельных» квадратов.
 *
 * Идея: линия — это один повёрнутый текстурированный квад со спрайтом,
 * у которого альфа плавно затухает к краям. Получается мягкая
 * антиалиасная линия без ступенек, ценой одного draw call на отрезок.
 *
 * Цвет и альфа «запекаются» в маленькую текстуру 64×8 (DrawContext не умеет
 * тонировать), альфа квантуется на 4 уровня, чтобы не плодить текстуры.
 */
public final class LineSprite {

    private static final int TEX_W = 64;
    private static final int TEX_H = 8;
    private static final int MAX_CACHE = 128;

    private static final Map<Integer, Identifier> CACHE = new HashMap<>();
    private static boolean failed;

    private LineSprite() {
    }

    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2,
                            float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.4f) {
            return;
        }

        Identifier texture = textureFor(color);
        if (texture == null) {
            fallback(ctx, x1, y1, x2, y2, width, color);
            return;
        }

        float angle = (float) Math.atan2(dy, dx);
        int height = Math.max(2, Math.round(width * 3.2f));

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x1, y1);
        matrices.rotate(angle);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture,
                0, Math.round(-height / 2f),
                0f, 0f, Math.round(length), height, TEX_W, TEX_H);
        matrices.popMatrix();
    }

    private static void fallback(DrawContext ctx, float x1, float y1, float x2, float y2,
                                 float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, Math.min(24, Math.round((float) Math.sqrt(dx * dx + dy * dy) / 5f)));
        float half = Math.max(0.6f, width / 2f);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float px = x1 + dx * t;
            float py = y1 + dy * t;
            ctx.fill(Math.round(px - half), Math.round(py - half),
                    Math.round(px + half) + 1, Math.round(py + half) + 1, color);
        }
    }

    private static Identifier textureFor(int color) {
        if (failed) {
            return null;
        }

        int alpha = (color >>> 24) & 0xFF;
        int quantized = Math.max(64, (alpha / 64) * 64);
        int key = (color & 0x00FFFFFF) | (quantized << 24);

        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            if (CACHE.size() > MAX_CACHE) {
                CACHE.clear();
            }

            int rgb = key & 0x00FFFFFF;
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, TEX_W, TEX_H, false);
            float center = TEX_H / 2f;

            for (int y = 0; y < TEX_H; y++) {
                float distance = Math.abs(y + 0.5f - center) / center;
                float falloff = Math.max(0f, 1f - distance);
                int a = (int) (quantized * falloff * falloff);
                int argb = (a << 24) | rgb;
                for (int x = 0; x < TEX_W; x++) {
                    image.setColorArgb(x, y, argb);
                }
            }

            Identifier id = Identifier.of("aurora", "line_" + Integer.toHexString(key));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id,
                    new NativeImageBackedTexture(() -> "aurora line", image));
            CACHE.put(key, id);
            return id;
        } catch (Throwable t) {
            failed = true;
            return null;
        }
    }
}
