package dev.aurora.render;

import dev.aurora.util.CameraUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Мировые эффекты в экранном пространстве.
 *
 * Оптимизация: число шагов линий ограничено, частицы — два прямоугольника
 * вместо построчных кругов, толщина колец масштабируется общим quality-фактором.
 */
public final class ScreenProjection {

    /** 1.0 — обычное качество, 0.5 — экономный режим из модуля Optimization. */
    private static float quality = 1f;

    private ScreenProjection() {
    }

    public static void setQuality(float value) {
        quality = Math.max(0.35f, Math.min(1.5f, value));
    }

    public static float quality() {
        return quality;
    }

    public static float[] project(Vec3d pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return null;
        }
        return CameraUtil.project(pos, client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight());
    }

    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2,
                            float width, int color) {
        gradientLine(ctx, x1, y1, x2, y2, width, color, color);
    }

    /** Отрезок: один повёрнутый спрайт с мягкими краями — без «пиксельных» ступенек. */
    public static void gradientLine(DrawContext ctx, float x1, float y1, float x2, float y2,
                                    float width, int colorFrom, int colorTo) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5f) {
            return;
        }
        LineSprite.line(ctx, x1, y1, x2, y2, width, ColorUtil.lerp(colorFrom, colorTo, 0.5f));
    }

    /** 3D-кольцо: тонкая яркая линия + широкое свечение под ней. */
    public static void ring(DrawContext ctx, Vec3d center, float radius, int segments,
                            float tilt, float spin, float width,
                            int colorStart, int colorEnd) {
        int count = Math.max(12, Math.round(segments * quality));
        float glowWidth = width * 3.2f;

        ringPass(ctx, center, radius, count, tilt, spin, glowWidth,
                ColorUtil.withAlpha(colorStart, 0.16f), ColorUtil.withAlpha(colorEnd, 0.16f));
        ringPass(ctx, center, radius, count, tilt, spin, width, colorStart, colorEnd);
    }

    private static void ringPass(DrawContext ctx, Vec3d center, float radius, int segments,
                                 float tilt, float spin, float width, int colorStart, int colorEnd) {
        float sinT = (float) Math.sin(tilt);
        float cosT = (float) Math.cos(tilt);

        float[] prev = null;
        int prevColor = colorStart;

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments + spin;
            float lx = (float) Math.cos(angle) * radius;
            float lz = (float) Math.sin(angle) * radius;
            float ly = -lz * sinT;
            float rz = lz * cosT;

            float[] screen = project(center.add(lx, ly, rz));
            int color = ColorUtil.lerp(colorStart, colorEnd, (float) i / segments);

            if (screen != null && prev != null) {
                gradientLine(ctx, prev[0], prev[1], screen[0], screen[1], width, prevColor, color);
            }
            prev = screen;
            prevColor = color;
        }
    }

    public static void box(DrawContext ctx, Box box, float expand, float width,
                           int topColor, int bottomColor) {
        float minX = (float) (box.minX - expand);
        float minY = (float) (box.minY - expand);
        float minZ = (float) (box.minZ - expand);
        float maxX = (float) (box.maxX + expand);
        float maxY = (float) (box.maxY + expand);
        float maxZ = (float) (box.maxZ + expand);

        Vec3d[] corners = {
                new Vec3d(minX, minY, minZ), new Vec3d(maxX, minY, minZ),
                new Vec3d(maxX, minY, maxZ), new Vec3d(minX, minY, maxZ),
                new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ),
                new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ),
        };

        float[][] proj = new float[8][];
        for (int i = 0; i < 8; i++) {
            proj[i] = project(corners[i]);
        }

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7},
        };

        for (int[] edge : edges) {
            float[] a = proj[edge[0]];
            float[] b = proj[edge[1]];
            if (a == null || b == null) {
                continue;
            }
            boolean vertical = edge[0] < 4 && edge[1] >= 4;
            int from = vertical ? bottomColor : topColor;
            gradientLine(ctx, a[0], a[1], b[0], b[1], width, from, topColor);
        }
    }

    /**
     * Частица: два прямоугольника (мягкий ореол + ядро) вместо построчного круга.
     * Визуально почти неотличимо, но в десятки раз дешевле.
     */
    /** Круглая сфера: ореол + ядро. */
    /** Заливка граней бокса: проекция 8 углов и заливка каждой грани построчно. */
    public static void fillBox(DrawContext ctx, Box box, int color) {
        if (ColorUtil.alphaOf(color) < 4) {
            return;
        }
        Vec3d[] corners = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX : box.maxX;
            double y = (i & 2) == 0 ? box.minY : box.maxY;
            double z = (i & 4) == 0 ? box.minZ : box.maxZ;
            corners[i] = new Vec3d(x, y, z);
        }

        float[][] p = new float[8][];
        for (int i = 0; i < 8; i++) {
            p[i] = project(corners[i]);
            if (p[i] == null) {
                return;
            }
        }

        int[][] faces = {
                {0, 1, 3, 2}, {4, 5, 7, 6},
                {0, 1, 5, 4}, {2, 3, 7, 6},
                {0, 2, 6, 4}, {1, 3, 7, 5},
        };
        for (int[] f : faces) {
            fillQuad(ctx, p[f[0]], p[f[1]], p[f[2]], p[f[3]], color);
        }
    }

    private static void fillQuad(DrawContext ctx, float[] a, float[] b, float[] c, float[] d, int color) {
        float minYf = Math.min(Math.min(a[1], b[1]), Math.min(c[1], d[1]));
        float maxYf = Math.max(Math.max(a[1], b[1]), Math.max(c[1], d[1]));
        int y0 = (int) Math.floor(minYf);
        int y1 = (int) Math.ceil(maxYf);
        if (y1 - y0 > 480 || y1 < 0) {
            return;
        }
        y0 = Math.max(y0, 0);
        y1 = Math.min(y1, ctx.getScaledWindowHeight());

        float[] xs = {a[0], b[0], c[0], d[0]};
        float[] ys = {a[1], b[1], c[1], d[1]};

        for (int y = y0; y <= y1; y++) {
            float py = y + 0.5f;
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                float ya = ys[i];
                float yb = ys[j];
                if (ya == yb) {
                    continue;
                }
                if ((py >= ya && py < yb) || (py >= yb && py < ya)) {
                    float t = (py - ya) / (yb - ya);
                    float x = xs[i] + (xs[j] - xs[i]) * t;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
            if (minX < maxX) {
                ctx.fill((int) minX, y, (int) maxX, y + 1, color);
            }
        }
    }

    public static void orb(DrawContext ctx, Vec3d pos, float worldSize, int core, int glow) {
        float[] screen = project(pos);
        if (screen == null) {
            return;
        }
        float radius = Math.max(1f, Math.min(18f, worldSize * screen[2] * 0.5f));
        Render2D.circle(ctx, screen[0], screen[1], radius * 2.0f, glow);
        Render2D.circle(ctx, screen[0], screen[1], radius, core);
    }

    /** Звёздочка: две пересекающиеся линии + диагонали. */
    public static void star(DrawContext ctx, Vec3d pos, float worldSize, int core, int glow) {
        float[] screen = project(pos);
        if (screen == null) {
            return;
        }
        float radius = Math.max(1.5f, Math.min(20f, worldSize * screen[2] * 0.6f));
        float x = screen[0];
        float y = screen[1];

        gradientLine(ctx, x - radius, y, x + radius, y, 1.6f, glow, glow);
        gradientLine(ctx, x, y - radius, x, y + radius, 1.6f, glow, glow);
        float diagonal = radius * 0.7f;
        gradientLine(ctx, x - diagonal, y - diagonal, x + diagonal, y + diagonal, 1f, core, core);
        gradientLine(ctx, x - diagonal, y + diagonal, x + diagonal, y - diagonal, 1f, core, core);
    }

    public static void particle(DrawContext ctx, Vec3d pos, float worldSize, int core, int glow) {
        float[] screen = project(pos);
        if (screen == null) {
            return;
        }
        float radius = Math.max(0.9f, Math.min(22f, worldSize * screen[2] * 0.5f));

        if (radius > 2.2f) {
            float halo = radius * 1.8f;
            ctx.fill(Math.round(screen[0] - halo), Math.round(screen[1] - halo),
                    Math.round(screen[0] + halo), Math.round(screen[1] + halo), glow);
        }
        ctx.fill(Math.round(screen[0] - radius), Math.round(screen[1] - radius),
                Math.round(screen[0] + radius), Math.round(screen[1] + radius), core);
    }
}
