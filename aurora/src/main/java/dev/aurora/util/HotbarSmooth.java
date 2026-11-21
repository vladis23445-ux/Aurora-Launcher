package dev.aurora.util;

/** Плавное перемещение ванильного выделения слота в хотбаре. */
public final class HotbarSmooth {

    public static boolean active;
    public static float speed = 18f;
    public static float pos = -1f;

    private static long lastNs;

    private HotbarSmooth() {
    }

    public static int apply(int targetX) {
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 0.016f : Math.min(0.1f, (float) ((now - lastNs) / 1.0e9));
        lastNs = now;

        if (pos < 0f || Math.abs(targetX - pos) > 200f) {
            pos = targetX;
        }

        float factor = 1f - (float) Math.exp(-speed * dt);
        pos += (targetX - pos) * factor;
        return Math.round(pos);
    }

    public static void reset() {
        pos = -1f;
        lastNs = 0L;
    }
}
