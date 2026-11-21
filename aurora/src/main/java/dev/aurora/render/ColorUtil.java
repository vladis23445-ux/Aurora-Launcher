package dev.aurora.render;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    public static int rgb(int red, int green, int blue) {
        return argb(255, red, green, blue);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static int alphaOf(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int redOf(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int greenOf(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blueOf(int color) {
        return color & 0xFF;
    }

    /** Умножает существующую альфу на коэффициент 0..1. */
    public static int withAlpha(int color, float multiplier) {
        int a = (int) (alphaOf(color) * Math.max(0f, Math.min(1f, multiplier)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int replaceAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    public static int lerp(int from, int to, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        int a = (int) (alphaOf(from) + (alphaOf(to) - alphaOf(from)) * f);
        int r = (int) (redOf(from) + (redOf(to) - redOf(from)) * f);
        int g = (int) (greenOf(from) + (greenOf(to) - greenOf(from)) * f);
        int b = (int) (blueOf(from) + (blueOf(to) - blueOf(from)) * f);
        return argb(a, r, g, b);
    }

    public static int hsb(float hue, float saturation, float brightness, int alpha) {
        int rgbValue = java.awt.Color.HSBtoRGB(hue, clamp01(saturation), clamp01(brightness));
        return argb(alpha, redOf(rgbValue), greenOf(rgbValue), blueOf(rgbValue));
    }

    public static float hueOf(int color) {
        return java.awt.Color.RGBtoHSB(redOf(color), greenOf(color), blueOf(color), null)[0];
    }

    public static float saturationOf(int color) {
        return java.awt.Color.RGBtoHSB(redOf(color), greenOf(color), blueOf(color), null)[1];
    }

    public static float brightnessOf(int color) {
        return java.awt.Color.RGBtoHSB(redOf(color), greenOf(color), blueOf(color), null)[2];
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** Плавный градиент по времени (используется для Watermark/Trails). */
    public static int rainbow(long speed, int offset, int alpha) {
        float hue = (System.currentTimeMillis() % (long) (360 * speed) / (float) (360 * speed) + offset / 360f) % 1f;
        return hsb(hue, 0.65f, 1f, alpha);
    }
}
