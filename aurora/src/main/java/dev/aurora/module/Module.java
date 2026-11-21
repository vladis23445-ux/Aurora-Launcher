package dev.aurora.module;

import dev.aurora.event.EventBus;
import dev.aurora.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected final EventBus bus;

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = -1;

    protected Module(EventBus bus, String name, String description, Category category) {
        this.bus = bus;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Module bind(int key) {
        this.key = key;
        return this;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    protected <S extends Setting<?>> S register(S setting) {
        settings.add(setting);
        return setting;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onHudRender(DrawContext context, float tickDelta) {
    }
}
