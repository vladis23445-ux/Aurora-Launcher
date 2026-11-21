package dev.aurora.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final E[] values;

    public EnumSetting(String name, E defaultValue) {
        super(name, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] getValues() {
        return values;
    }

    public void cycle() {
        int index = getValue().ordinal();
        setValue(values[(index + 1) % values.length]);
    }

    public void cycleBack() {
        int index = getValue().ordinal();
        setValue(values[(index - 1 + values.length) % values.length]);
    }

    public String getDisplay() {
        String raw = getValue().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue().name());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        String name = element.getAsString();
        for (E value : values) {
            if (value.name().equalsIgnoreCase(name)) {
                setValue(value);
                return;
            }
        }
    }
}
