package dev.aurora.render;

/** Экспоненциально сглаженное значение — основа всех анимаций GUI и HUD. */
public final class Animation {

    private float value;
    private float target;
    private float speed;

    public Animation(float start, float speed) {
        this.value = start;
        this.target = start;
        this.speed = speed;
    }

    public Animation(float start) {
        this(start, 12f);
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public float getTarget() {
        return target;
    }

    public float getValue() {
        return value;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
    }

    public float update(float deltaSeconds) {
        float factor = 1f - (float) Math.exp(-speed * Math.max(0.0001f, deltaSeconds));
        value += (target - value) * factor;
        if (Math.abs(target - value) < 0.0005f) {
            value = target;
        }
        return value;
    }

    public boolean isFinished() {
        return Math.abs(target - value) < 0.001f;
    }

    public static float easeOutCubic(float t) {
        float x = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - x * x * x;
    }

    public static float easeInOutQuad(float t) {
        float x = Math.max(0f, Math.min(1f, t));
        return x < 0.5f ? 2f * x * x : 1f - (float) Math.pow(-2f * x + 2f, 2) / 2f;
    }
}
