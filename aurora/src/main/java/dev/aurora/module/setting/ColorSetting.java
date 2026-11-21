package dev.aurora.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.aurora.render.ColorUtil;

public class ColorSetting extends Setting<Integer> {

    public ColorSetting(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public ColorSetting(String name, int defaultArgb, String description) {
        super(name, defaultArgb, description);
    }

    public int getArgb() {
        return getValue();
    }

    public int getRgb() {
        return getValue() & 0x00FFFFFF;
    }

    public int withAlpha(float alpha) {
        return ColorUtil.withAlpha(getValue(), alpha);
    }

    public float getHue() {
        return ColorUtil.hueOf(getValue());
    }

    public float getSaturation() {
        return ColorUtil.saturationOf(getValue());
    }

    public float getBrightness() {
        return ColorUtil.brightnessOf(getValue());
    }

    public void setHsb(float hue, float saturation, float brightness) {
        setValue(ColorUtil.hsb(hue, saturation, brightness, ColorUtil.alphaOf(getValue())));
    }

    public void setAlpha(float alpha) {
        setValue(ColorUtil.withAlpha(getValue(), alpha));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsInt());
        }
    }
}
