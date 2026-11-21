package dev.aurora.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0.01 : step;
        setValue(defaultValue);
    }

    @Override
    public void setValue(Double value) {
        super.setValue(round(value == null ? min : value));
    }

    private double round(double v) {
        double clamped = Math.max(min, Math.min(max, v));
        double steps = Math.round(clamped / step);
        double result = steps * step;
        return Math.max(min, Math.min(max, result));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public float getFloat() {
        return getValue().floatValue();
    }

    public int getInt() {
        return (int) Math.round(getValue());
    }

    /** Доля 0..1 для отрисовки слайдера. */
    public float getProgress() {
        return (float) ((getValue() - min) / (max - min));
    }

    public void setProgress(float progress) {
        setValue(min + (max - min) * Math.max(0f, Math.min(1f, progress)));
    }

    public String getDisplay() {
        double v = getValue();
        if (step >= 1) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsDouble());
        }
    }
}
