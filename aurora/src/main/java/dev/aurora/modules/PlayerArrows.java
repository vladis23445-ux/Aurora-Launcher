package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.render.ScreenProjection;
import dev.aurora.util.CameraUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * PlayerArrows: указатели на игроков рядом.
 * Если игрок в кадре — метка над ним, если нет — стрелка у края экрана.
 */
public class PlayerArrows extends Module {

    private final NumberSetting range = register(new NumberSetting("Range", 64.0, 8.0, 256.0, 1));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFF7C5CFF));
    private final BooleanSetting distance = register(new BooleanSetting("Distance", true));
    private final NumberSetting edge = register(new NumberSetting("EdgeInset", 34.0, 12.0, 120.0, 1));

    public PlayerArrows(EventBus bus) {
        super(bus, "PlayerArrows", "Стрелки на игроков рядом с краёв экрана", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null || mc.gameRenderer.getCamera() == null) {
            return;
        }

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        float centerX = screenW / 2f;
        float centerY = screenH / 2f;
        float inset = edge.getFloat();
        double limit = range.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) {
                continue;
            }
            double dist = Math.sqrt(mc.player.squaredDistanceTo(player));
            if (dist > limit) {
                continue;
            }

            Vec3d target = CameraUtil.lerped(player).add(0.0, player.getHeight() * 0.9, 0.0);
            float[] screen = ScreenProjection.project(target);
            int argb = color.getArgb();

            boolean onScreen = screen != null
                    && screen[0] > inset && screen[0] < screenW - inset
                    && screen[1] > inset && screen[1] < screenH - inset;

            if (onScreen) {
                float x = screen[0];
                float y = screen[1];
                Render2D.roundedRect(context, x - 2.5f, y - 2.5f, 5f, 5f, 2.5f, argb);
                Render2D.roundedBorder(context, x - 6f, y - 6f, 12f, 12f, 6f, 1f,
                        ColorUtil.withAlpha(argb, 0.35f));
                if (distance.getValue()) {
                    String label = String.format(java.util.Locale.ROOT, "%.0f м", dist);
                    Render2D.textCentered(context, label, x, y + 9f,
                            ColorUtil.withAlpha(0xFFFFFFFF, 0.7f));
                }
                continue;
            }

            // направление в экранных осях камеры
            Vec3d eye = CameraUtil.camera().getCameraPos();
            Vec3d toPlayer = target.subtract(eye);
            org.joml.Vector3f rightVector = CameraUtil.right();
            org.joml.Vector3f upVector = CameraUtil.up();
            Vec3d right = new Vec3d(rightVector.x, rightVector.y, rightVector.z);
            Vec3d up = new Vec3d(upVector.x, upVector.y, upVector.z);

            double side = toPlayer.dotProduct(right);
            double vertical = toPlayer.dotProduct(up);
            double depth = toPlayer.dotProduct(mc.player.getRotationVec(1f));

            double angle = Math.atan2(vertical, side);
            // чтобы стрелка всегда была у края, а не «внутри» экрана
            float radiusX = centerX - inset;
            float radiusY = centerY - inset;
            float dirX = (float) Math.cos(angle);
            float dirY = (float) Math.sin(angle);
            float scale = Math.min(
                    radiusX / Math.max(0.0001f, Math.abs(dirX)),
                    radiusY / Math.max(0.0001f, Math.abs(dirY)));
            float ax = centerX + dirX * scale;
            float ay = centerY + dirY * scale;

            if (depth > 0) {
                // игрок за спиной — переворачиваем направление
                ax = centerX - dirX * scale;
                ay = centerY - dirY * scale;
            }

            drawArrow(context, ax, ay, angle, argb, depth > 0);
            if (distance.getValue()) {
                String label = String.format(java.util.Locale.ROOT, "%.0f м", dist);
                Render2D.textCentered(context, label, ax - Math.signum(dirX) * 22f, ay + 12f,
                        ColorUtil.withAlpha(0xFFFFFFFF, 0.7f));
            }
        }
    }

    private void drawArrow(DrawContext context, float x, float y, double angle, int argb, boolean flipped) {
        float baseAngle = (float) angle + (flipped ? (float) Math.PI : 0f);
        float size = 7f;
        float tipX = x + (float) Math.cos(baseAngle) * size;
        float tipY = y + (float) Math.sin(baseAngle) * size;
        float leftX = x + (float) Math.cos(baseAngle + 2.5f) * size;
        float leftY = y + (float) Math.sin(baseAngle + 2.5f) * size;
        float rightX = x + (float) Math.cos(baseAngle - 2.5f) * size;
        float rightY = y + (float) Math.sin(baseAngle - 2.5f) * size;

        Render2D.glow(context, x, y, 9f, 9f, ColorUtil.withAlpha(argb, 0.35f), 1f, 2);
        fillTriangle(context, tipX, tipY, leftX, leftY, rightX, rightY, argb);
    }

    private void fillTriangle(DrawContext context, float x1, float y1, float x2, float y2,
                              float x3, float y3, int color) {
        float minY = Math.min(y1, Math.min(y2, y3));
        float maxY = Math.max(y1, Math.max(y2, y3));
        for (float y = minY; y <= maxY; y += 1f) {
            float[] hits = new float[3];
            int count = 0;
            count = addHit(hits, count, y, x1, y1, x2, y2);
            count = addHit(hits, count, y, x2, y2, x3, y3);
            count = addHit(hits, count, y, x3, y3, x1, y1);
            if (count < 2) {
                continue;
            }
            float xa = Math.min(hits[0], hits[1]);
            float xb = Math.max(hits[0], hits[1]);
            if (xb - xa > 0.3f) {
                Render2D.rect(context, xa, y, xb - xa, 1f, color);
            }
        }
    }

    private int addHit(float[] hits, int count, float y, float xa, float ya, float xb, float yb) {
        if ((ya <= y && yb > y) || (yb <= y && ya > y)) {
            float t = (y - ya) / (yb - ya);
            hits[count] = xa + (xb - xa) * t;
            return count + 1;
        }
        return count;
    }
}
