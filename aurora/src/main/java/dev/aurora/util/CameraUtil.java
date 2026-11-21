package dev.aurora.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Математика камеры и проекция world → screen.
 * API 1.21.11: Camera#getCameraPos, Entity#getLerpedPos(float).
 */
public final class CameraUtil {

    private CameraUtil() {
    }

    public static Camera camera() {
        return MinecraftClient.getInstance().gameRenderer.getCamera();
    }

    public static Vector3f right() {
        return new Vector3f(1f, 0f, 0f).rotate(camera().getRotation());
    }

    public static Vector3f up() {
        return new Vector3f(0f, 1f, 0f).rotate(camera().getRotation());
    }

    /**
     * Проекция точки мира на экран: {x, y, scale} либо null, если точка за камерой.
     */
    public static float[] project(Vec3d worldPos, float screenW, float screenH) {
        Camera cam = camera();
        Vec3d camPos = cam.getCameraPos();
        Quaternionf inverse = new Quaternionf(cam.getRotation()).conjugate();

        Vector3f view = new Vector3f(
                (float) (worldPos.x - camPos.x),
                (float) (worldPos.y - camPos.y),
                (float) (worldPos.z - camPos.z)
        ).rotate(inverse);

        if (view.z >= -0.05f) {
            return null;
        }

        int fov = MinecraftClient.getInstance().options.getFov().getValue();
        double fovRad = Math.toRadians(MathHelper.clamp(fov, 30, 110));
        double focal = (screenH / 2.0) / Math.tan(fovRad / 2.0);

        float sx = (float) (screenW / 2.0 + (view.x / -view.z) * focal);
        float sy = (float) (screenH / 2.0 - (view.y / -view.z) * focal);
        float scale = (float) (focal / -view.z);
        return new float[]{sx, sy, scale};
    }

    /** Позиция сущности с интерполяцией кадра. */
    public static Vec3d lerped(Entity entity) {
        float delta;
        try {
            delta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        } catch (Throwable t) {
            delta = 1f;
        }
        return entity.getLerpedPos(delta);
    }

    /** Угол между направлением взгляда и точкой (градусы). */
    public static float angleTo(Vec3d point) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 180f;
        }
        Vec3d eye = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1f);
        Vec3d to = point.subtract(eye).normalize();
        double dot = MathHelper.clamp(look.dotProduct(to), -1.0, 1.0);
        return (float) Math.toDegrees(Math.acos(dot));
    }
}
