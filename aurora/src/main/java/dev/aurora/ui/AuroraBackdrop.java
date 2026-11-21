package dev.aurora.ui;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;

/**
 * Анимированный aurora-фон: глубокий зелёный градиент + струящиеся полотна.
 * Общий для главного меню и ClickGUI. strength уменьшает насыщенность,
 * чтобы за GUI был виден мир, а не «шторка на весь экран».
 */
public final class AuroraBackdrop {

    public static final int ACCENT = 0xFF3BE8A0;
    public static final int ACCENT2 = 0xFF40D6D6;
    public static final int MINT = 0xFF8CFFC8;

    private AuroraBackdrop() {
    }

    public static void render(DrawContext context, int width, int height, float progress) {
        render(context, width, height, progress, 1f);
    }

    public static void render(DrawContext context, int width, int height, float progress, float strength) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float factor = Math.max(0f, Math.min(1f, strength));

        Render2D.gradientV(context, 0, 0, width, height,
                ColorUtil.argb((int) (200 * progress * factor), 3, 9, 7),
                ColorUtil.argb((int) (230 * progress * factor), 6, 17, 13));

        double time = (System.currentTimeMillis() % 1_000_000L) / 1000.0;

        curtain(context, width, height, time, 0.0, 0.34, 0.20f, 0.030f, 0.0120f, ACCENT, progress, 0.17f * factor);
        curtain(context, width, height, time, 1.9, 0.27, 0.31f, 0.042f, 0.0090f, ACCENT2, progress, 0.13f * factor);
        curtain(context, width, height, time, 3.4, 0.41, 0.13f, 0.026f, 0.0160f, MINT, progress, 0.10f * factor);
        curtain(context, width, height, time, 5.1, 0.22, 0.44f, 0.048f, 0.0070f, 0xFF18A06A, progress, 0.11f * factor);

        float drift = (float) Math.sin(time * 0.31) * 30f;
        Render2D.glow(context, width * 0.2f + drift, -height * 0.1f,
                width * 0.42f, height * 0.55f, ACCENT, 0.30f * progress * factor, 3);
        Render2D.glow(context, width * 0.85f - drift, height * 0.98f,
                width * 0.36f, height * 0.45f, ACCENT2, 0.18f * progress * factor, 3);
    }

    private static void curtain(DrawContext context, int width, int height, double time,
                                double phase, double speed, float baseFactor, float amplitudeFactor,
                                float frequency, int color, float progress, float alpha) {
        int step = 6;
        int segments = 10;
        float baseY = height * baseFactor;
        float amplitude = height * amplitudeFactor;
        float thickness = height * 0.26f;

        for (int x = 0; x < width; x += step) {
            double wx = x * frequency + phase + time * speed;
            float wave = (float) (Math.sin(wx) * amplitude
                    + Math.sin(wx * 0.47 - time * speed * 0.6) * amplitude * 0.45);
            float topY = baseY + wave;

            for (int s = 0; s < segments; s++) {
                float t = s / (float) segments;
                float a = alpha * progress * (1f - t * 0.85f);
                if (a <= 0.002f) {
                    continue;
                }
                int y0 = Math.round(topY + thickness * t);
                int y1 = Math.round(topY + thickness * (s + 1) / segments);
                if (y1 > y0) {
                    context.fill(x, y0, x + step + 1, y1, ColorUtil.withAlpha(color, a));
                }
            }
        }
    }
}
