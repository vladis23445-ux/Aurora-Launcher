package dev.aurora.module.setting;

import com.google.gson.JsonElement;

public abstract class Setting<T> {

    private final String name;
    private final String description;
    private T value;
    private boolean visible = true;

    protected Setting(String name, T defaultValue) {
        this(name, defaultValue, "");
    }

    protected Setting(String name, T defaultValue, String description) {
        this.name = name;
        this.value = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visible;
    }

    public Setting<T> setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
