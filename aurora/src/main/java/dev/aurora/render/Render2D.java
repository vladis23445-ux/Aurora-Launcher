package dev.aurora.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D-примитивы, оптимизированные под слабые ПК.
 *
 * Главное правило: НЕ рисовать по одному кваду на пиксель. Градиенты рисуются
 * полосами (шаг 3-4 px), скругления — только в угловых зонах, остальное одним
 * прямоугольником. Это снижает число вызовов fill на кадр с десятков тысяч
 * до сотен.
 */
public final class Render2D {

    private Render2D() {
    }

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    // ---------------------------------------------------------------- база

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        ctx.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
    }

    public static void roundedRect(DrawContext ctx, float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float r = Math.min(radius, Math.min(w, h) / 2f);
        int y0 = Math.round(y);
        int y1 = Math.round(y + h);
        int cornerTop = Math.round(y + r);
        int cornerBottom = Math.round(y + h - r);

        if (cornerBottom > cornerTop) {
            ctx.fill(Math.round(x), cornerTop, Math.round(x + w), cornerBottom, color);
        }
        cornerRows(ctx, x, y, w, h, r, color, y0, Math.min(cornerTop, y1));
        cornerRows(ctx, x, y, w, h, r, color, Math.max(cornerBottom, y0), y1);
    }

    private static void cornerRows(DrawContext ctx, float x, float y, float w, float h, float r,
                                   int color, int from, int to) {
        for (int row = from; row < to; row++) {
            float center = row + 0.5f;
            float cy = center < y + r ? y + r : y + h - r;
            float dy = center - cy;
            float inset = r - (float) Math.sqrt(Math.max(0f, r * r - dy * dy));
            int left = Math.round(x + inset);
            int right = Math.round(x + w - inset);
            if (right > left) {
                ctx.fill(left, row, right, row + 1, color);
            }
        }
    }

    public static void roundedBorder(DrawContext ctx, float x, float y, float w, float h,
                                     float radius, float thickness, int color) {
        float t = Math.max(0.5f, thickness);
        float r = Math.min(radius, Math.min(w, h) / 2f);
        int y0 = Math.round(y);
        int y1 = Math.round(y + h);
        for (int row = y0; row < y1; row++) {
            float center = row + 0.5f;
            float cy = Math.max(y + r, Math.min(y + h - r, center));
            float dy = center - cy;
            float inset = r - (float) Math.sqrt(Math.max(0f, r * r - dy * dy));
            boolean topBottom = center < y + t * 0.8f || center > y + h - t * 0.8f;
            int left = Math.round(x + inset);
            int right = Math.round(x + w - inset);
            if (right <= left) {
                continue;
            }
            if (topBottom) {
                ctx.fill(left, row, right, row + 1, color);
            } else {
                ctx.fill(left, row, Math.round(left + t), row + 1, color);
                ctx.fill(Math.round(right - t), row, right, row + 1, color);
            }
        }
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        ellipse(ctx, cx, cy, radius, radius, color, Math.max(1, Math.round(radius / 8f)));
    }

    /** Эллипс построчно с шагом; шаг растёт вместе с радиусом. */
    public static void ellipse(DrawContext ctx, float cx, float cy, float rx, float ry, int color, int step) {
        if (rx <= 0 || ry <= 0) {
            return;
        }
        int stride = Math.max(1, step);
        int y0 = Math.round(cy - ry);
        int y1 = Math.round(cy + ry);
        for (int row = y0; row < y1; row += stride) {
            float dy = (row + stride * 0.5f) - cy;
            float k = 1f - (dy * dy) / (ry * ry);
            if (k <= 0f) {
                continue;
            }
            float half = rx * (float) Math.sqrt(k);
            if (half > 0.4f) {
                ctx.fill(Math.round(cx - half), row,
                        Math.round(cx + half), Math.min(row + stride, y1), color);
            }
        }
    }

    // ------------------------------------------------------------- градиенты

    /** Вертикальный градиент полосами (≈40 полос вместо одной на строку). */
    public static void gradientV(DrawContext ctx, float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int strips = Math.max(4, Math.min(48, Math.round(h / 12f)));
        float step = h / strips;
        for (int i = 0; i < strips; i++) {
            int c = ColorUtil.lerp(top, bottom, i / (float) (strips - 1));
            ctx.fill(Math.round(x), Math.round(y + i * step),
                    Math.round(x + w), Math.round(y + (i + 1) * step) + 1, c);
        }
    }

    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int strips = Math.max(4, Math.min(48, Math.round(w / 12f)));
        float step = w / strips;
        for (int i = 0; i < strips; i++) {
            int c = ColorUtil.lerp(left, right, i / (float) (strips - 1));
            ctx.fill(Math.round(x + i * step), Math.round(y),
                    Math.round(x + (i + 1) * step) + 1, Math.round(y + h), c);
        }
    }

    /**
     * Скруглённая плашка с горизонтальным градиентом: углы заливаются базовым
     * цветом, градиент — полосами только в средней зоне. Дёшево и незаметно.
     */
    public static void roundedGradient(DrawContext ctx, float x, float y, float w, float h,
                                       float radius, int left, int right) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float r = Math.min(radius, Math.min(w, h) / 2f);
        roundedRect(ctx, x, y, w, h, r, left);

        float top = y + Math.min(r, h / 2f);
        float bottom = y + h - Math.min(r, h / 2f);
        if (bottom - top <= 0.5f) {
            return;
        }
        int strips = Math.max(6, Math.min(24, Math.round(w / 16f)));
        float step = w / strips;
        for (int i = 0; i < strips; i++) {
            int c = ColorUtil.lerp(left, right, i / (float) (strips - 1));
            ctx.fill(Math.round(x + i * step), Math.round(top),
                    Math.round(x + (i + 1) * step) + 1, Math.round(bottom), c);
        }
    }

    public static void roundedGradientV(DrawContext ctx, float x, float y, float w, float h,
                                        float radius, int top, int bottom) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float r = Math.min(radius, Math.min(w, h) / 2f);
        roundedRect(ctx, x, y, w, h, r, top);

        int strips = Math.max(6, Math.min(24, Math.round(h / 12f)));
        float step = h / strips;
        for (int i = 0; i < strips; i++) {
            int c = ColorUtil.lerp(top, bottom, i / (float) (strips - 1));
            ctx.fill(Math.round(x + r), Math.round(y + i * step),
                    Math.round(x + w - r), Math.round(y + (i + 1) * step) + 1, c);
        }
    }

    // ------------------------------------------------------------------ glow

    /** Мягкое свечение: 2 слоя вместо 4 — в 4 раза дешевле. */
    public static void glow(DrawContext ctx, float cx, float cy, float rx, float ry,
                            int color, float strength, int layers) {
        int used = Math.min(2, Math.max(1, layers));
        for (int layer = 0; layer < used; layer++) {
            float scale = 1f - layer * 0.45f;
            float alpha = strength * (0.18f + layer * 0.35f);
            ellipse(ctx, cx, cy, rx * scale, ry * scale, ColorUtil.withAlpha(color, alpha), 10);
        }
    }

    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float radius, float strength) {
        for (int i = 2; i >= 1; i--) {
            float grow = i * 3f;
            int alpha = (int) (strength * 22 / i);
            roundedRect(ctx, x - grow, y - grow + 2, w + grow * 2, h + grow * 2, radius + grow,
                    ColorUtil.argb(alpha, 0, 0, 0));
        }
    }

    // ----------------------------------------------------------------- текст

    public static int textWidth(String text) {
        return font().getWidth(text);
    }

    public static void text(DrawContext ctx, String text, float x, float y, int color) {
        ctx.drawText(font(), text, Math.round(x), Math.round(y), color, false);
    }

    public static void textShadow(DrawContext ctx, String text, float x, float y, int color) {
        ctx.drawText(font(), text, Math.round(x), Math.round(y), color, true);
    }

    public static void textCentered(DrawContext ctx, String text, float centerX, float y, int color) {
        ctx.drawText(font(), text, Math.round(centerX - textWidth(text) / 2f), Math.round(y), color, false);
    }

    public static float scaledText(DrawContext ctx, String text, float x, float y,
                                   float scale, int color, boolean shadow) {
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        if (shadow) {
            ctx.drawText(font(), text, 1, 1, ColorUtil.withAlpha(0x000000, 0.55f), false);
        }
        ctx.drawText(font(), text, 0, 0, color, false);
        matrices.popMatrix();
        return textWidth(text) * scale;
    }

    public static float scaledTextGradient(DrawContext ctx, String text, float x, float y,
                                           float scale, int from, int to, boolean shadow) {
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        float width = textGradient(ctx, text, 0, 0, from, to, shadow);
        matrices.popMatrix();
        return width * scale;
    }

    public static void scaledTextCentered(DrawContext ctx, String text, float centerX, float y,
                                          float scale, int color, boolean shadow) {
        float width = textWidth(text) * scale;
        scaledText(ctx, text, centerX - width / 2f, y, scale, color, shadow);
    }

    /** Текст с горизонтальным градиентом (посимвольно). */
    public static float textGradient(DrawContext ctx, String text, float x, float y,
                                     int from, int to, boolean shadow) {
        float cursor = x;
        int length = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int color = ColorUtil.lerp(from, to, i / (float) length);
            if (shadow) {
                ctx.drawText(font(), ch, Math.round(cursor) + 1, Math.round(y) + 1,
                        ColorUtil.withAlpha(0x000000, 0.55f), false);
            }
            ctx.drawText(font(), ch, Math.round(cursor), Math.round(y), color, false);
            cursor += font().getWidth(ch) + 0.35f;
        }
        return cursor - x;
    }

    public static float textGradientWidth(String text) {
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            width += textWidth(String.valueOf(text.charAt(i)));
        }
        return width;
    }

    public static List<String> wrap(String text, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxLines <= 0) {
            return lines;
        }
        StringBuilder current = new StringBuilder();
        boolean overflow = false;

        for (String word : text.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (textWidth(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(word);
            if (lines.size() >= maxLines) {
                overflow = true;
                break;
            }
        }

        if (!overflow && current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
        }
        if (overflow && !lines.isEmpty()) {
            int last = lines.size() - 1;
            String value = lines.get(last);
            while (!value.isEmpty() && textWidth(value + "…") > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            lines.set(last, value + "…");
        }
        return lines;
    }

    public static String trimTo(String text, float maxWidth) {
        if (textWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (textWidth(builder.toString() + c + "…") > maxWidth) {
                break;
            }
            builder.append(c);
        }
        return builder + "…";
    }
}
