package dev.aurora.modules;

import dev.aurora.cosmetics.Cosmetic;
import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.render.ScreenProjection;
import dev.aurora.util.CameraUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetics: плащи и короны из магазина лаунчера.
 * Геометрия считается в мире и проецируется на экран (см. ScreenProjection).
 */
public class CosmeticsRender extends Module {

    private final BooleanSetting self = register(new BooleanSetting("Self", true));
    private final BooleanSetting others = register(new BooleanSetting("Others", true));
    private final BooleanSetting sway = register(new BooleanSetting("Sway", true));
    private final NumberSetting scale = register(new NumberSetting("Scale", 1.0, 0.6, 1.6, 0.05));

    private int debugFrames;

    public CosmeticsRender(EventBus bus) {
        super(bus, "Cosmetics", "Плащи и короны, купленные в лаунчере", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        Cosmetic cape = Cosmetic.equipped(Cosmetic.Type.CAPE);
        Cosmetic crown = Cosmetic.equipped(Cosmetic.Type.CROWN);
        dev.aurora.Aurora.LOGGER.info("Cosmetics: плащ={}, корона={}",
                cape == null ? "нет" : cape.id(), crown == null ? "нет" : crown.id());
    }


    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        Cosmetic cape = Cosmetic.equipped(Cosmetic.Type.CAPE);
        Cosmetic crown = Cosmetic.equipped(Cosmetic.Type.CROWN);
        if (cape == null && crown == null) {
            if (debugFrames < 3) {
                debugFrames++;
                dev.aurora.Aurora.LOGGER.info("Cosmetics: ничего не надето — проверьте profile.json");
            }
            return;
        }

        if (debugFrames < 3) {
            debugFrames++;
            Vec3d head = CameraUtil.lerped(mc.player).add(0.0, 1.6, 0.0);
            float[] projected = ScreenProjection.project(head);
            dev.aurora.Aurora.LOGGER.info("Cosmetics: плащ={}, корона={}, thirdPerson={}, projection={}",
                    cape == null ? "нет" : cape.id(),
                    crown == null ? "нет" : crown.id(),
                    mc.options.getPerspective().isFirstPerson() ? "нет" : "да",
                    projected == null ? "за камерой" : "ok");
        }


        float size = scale.getFloat();
        boolean drawOthers = others.getValue();

        if (self.getValue()) {
            draw(context, mc.player, cape, crown, size, tickDelta);
        }
        if (drawOthers) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player) {
                    draw(context, player, cape, crown, size, tickDelta);
                }
            }
        }
    }

    private void draw(DrawContext ctx, PlayerEntity player, Cosmetic cape, Cosmetic crown,
                      float size, float tickDelta) {
        Vec3d base = CameraUtil.lerped(player);
        float yaw = (float) Math.toRadians(player.getBodyYaw());
        Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d back = forward.multiply(-1.0);

        if (cape != null) {
            drawCape(ctx, base, right, back, cape, size);
        }
        if (crown != null) {
            drawCrown(ctx, base, right, forward, crown, size);
        }
    }

    private void drawCape(DrawContext ctx, Vec3d base, Vec3d right, Vec3d back,
                          Cosmetic cape, float size) {
        double time = (System.currentTimeMillis() % 1_000_000L) / 1000.0;
        double wave = sway.getValue() ? Math.sin(time * 2.1) * 0.035 : 0.0;

        double halfWidth = 0.26 * size;
        double topY = 1.42;
        double bottomY = 0.44;
        double offset = 0.16;

        Vec3d topCenter = base.add(0.0, topY, 0.0).add(back.multiply(offset));
        Vec3d bottomCenter = base.add(0.0, bottomY, 0.0).add(back.multiply(offset + 0.05 + wave));

        Vec3d[] corners = {
                topCenter.add(right.multiply(-halfWidth)),
                topCenter.add(right.multiply(halfWidth)),
                bottomCenter.add(right.multiply(halfWidth)),
                bottomCenter.add(right.multiply(-halfWidth)),
        };

        List<float[]> points = projectAll(corners);
        if (points == null) {
            return;
        }

        fillPolygon(ctx, points, cape.topColor(), cape.bottomColor());

        // тонкая кромка и «молния» по центру
        float[] topLeft = points.get(0);
        float[] topRight = points.get(1);
        float[] bottomRight = points.get(3);
        ScreenProjection.gradientLine(ctx, topLeft[0], topLeft[1], topRight[0], topRight[1],
                Math.max(1f, 1.1f * topLeft[2] * 0.02f), ColorUtil.withAlpha(cape.accent(), 0.55f),
                ColorUtil.withAlpha(cape.accent(), 0.55f));
        ScreenProjection.gradientLine(ctx, topLeft[0], topLeft[1], bottomRight[0], bottomRight[1],
                Math.max(0.8f, 1.1f * topLeft[2] * 0.012f), ColorUtil.withAlpha(cape.accent(), 0.18f),
                ColorUtil.withAlpha(cape.accent(), 0.05f));
    }

    private void drawCrown(DrawContext ctx, Vec3d base, Vec3d right, Vec3d forward,
                           Cosmetic crown, float size) {
        double bandBottom = base.y + 1.60;
        double bandTop = base.y + 1.74;
        double radius = 0.22 * size;

        Vec3d center = new Vec3d(base.x, 0.0, base.z);

        Vec3d bottomLeft = new Vec3d(center.x, bandBottom, center.z).add(right.multiply(-radius));
        Vec3d bottomRight = new Vec3d(center.x, bandBottom, center.z).add(right.multiply(radius));
        Vec3d topRight = new Vec3d(center.x, bandTop, center.z).add(right.multiply(radius));
        Vec3d topLeft = new Vec3d(center.x, bandTop, center.z).add(right.multiply(-radius));

        List<float[]> band = projectAll(new Vec3d[] { bottomLeft, bottomRight, topRight, topLeft });
        if (band == null) {
            return;
        }
        fillPolygon(ctx, band, crown.topColor(), crown.bottomColor());

        // три зубца
        float[] leftPoint = band.get(0);
        float[] rightPoint = band.get(1);
        float toothWidth = (rightPoint[0] - leftPoint[0]) / 6f;
        float baseY = band.get(2)[1];
        float scaleFactor = leftPoint[2] * 0.02f;
        float toothHeight = Math.max(5f, 14f * scaleFactor);

        for (int i = 0; i < 3; i++) {
            float cx = leftPoint[0] + (rightPoint[0] - leftPoint[0]) * (0.22f + 0.28f * i);
            float[] xs = { cx - toothWidth, cx, cx + toothWidth };
            float[] ys = { baseY, baseY - toothHeight, baseY };
            fillPolygon(ctx, toPoints(xs, ys), crown.accent(), crown.topColor());
        }

        // камень спереди
        Vec3d frontPoint = new Vec3d(center.x, (bandBottom + bandTop) / 2.0, center.z)
                .add(forward.multiply(radius * 0.96));
        float[] gem = ScreenProjection.project(frontPoint);
        if (gem != null) {
            float r = Math.max(1.6f, 3.2f * gem[2] * 0.02f);
            Render2D.circle(ctx, gem[0], gem[1], r * 1.9f, ColorUtil.withAlpha(crown.accent(), 0.25f));
            Render2D.circle(ctx, gem[0], gem[1], r, crown.accent());
        }
    }

    // ------------------------------------------------------------ рисование

    private static List<float[]> projectAll(Vec3d[] corners) {
        List<float[]> points = new ArrayList<>(corners.length);
        for (Vec3d corner : corners) {
            float[] projected = ScreenProjection.project(corner);
            if (projected == null) {
                return null;
            }
            points.add(projected);
        }
        return points;
    }

    private static List<float[]> toPoints(float[] xs, float[] ys) {
        List<float[]> points = new ArrayList<>(xs.length);
        for (int i = 0; i < xs.length; i++) {
            points.add(new float[]{xs[i], ys[i], 1f});
        }
        return points;
    }

    /** Заливка выпуклого многоугольника построчно, с вертикальным градиентом. */
    private static void fillPolygon(DrawContext ctx, List<float[]> points, int colorTop, int colorBottom) {
        int n = points.size();
        if (n < 3) {
            return;
        }

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] point : points) {
            minY = Math.min(minY, point[1]);
            maxY = Math.max(maxY, point[1]);
        }
        if (maxY - minY > 900f || Float.isNaN(minY) || Float.isNaN(maxY)) {
            return;
        }

        int y0 = (int) Math.ceil(minY);
        int y1 = (int) Math.floor(maxY);
        float[] hits = new float[n];

        for (int y = y0; y <= y1; y++) {
            float scan = y + 0.5f;
            int count = 0;
            for (int i = 0; i < n; i++) {
                float[] a = points.get(i);
                float[] b = points.get((i + 1) % n);
                float ya = a[1];
                float yb = b[1];
                if ((ya <= scan && yb > scan) || (yb <= scan && ya > scan)) {
                    float t = (scan - ya) / (yb - ya);
                    hits[count++] = a[0] + (b[0] - a[0]) * t;
                }
            }
            if (count < 2) {
                continue;
            }
            for (int i = 1; i < count; i++) {
                float value = hits[i];
                int j = i - 1;
                while (j >= 0 && hits[j] > value) {
                    hits[j + 1] = hits[j];
                    j--;
                }
                hits[j + 1] = value;
            }

            float t = (scan - minY) / Math.max(1f, maxY - minY);
            int color = ColorUtil.lerp(colorTop, colorBottom, t);
            for (int i = 0; i + 1 < count; i += 2) {
                float xa = hits[i];
                float xb = hits[i + 1];
                if (xb - xa > 0.2f) {
                    Render2D.rect(ctx, xa, y, xb - xa, 1f, color);
                }
            }
        }
    }
}
