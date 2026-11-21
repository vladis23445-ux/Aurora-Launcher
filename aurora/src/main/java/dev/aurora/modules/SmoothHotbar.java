package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.util.HotbarSmooth;

/**
 * SmoothHotbar: ванильное выделение слота едет плавно, а не прыгает.
 * Своих плашек модуль не рисует — двигается сама игровая рамка.
 */
public class SmoothHotbar extends Module {

    private final NumberSetting smoothing = register(new NumberSetting("Smoothing", 14.0, 4.0, 30.0, 1.0));

    public SmoothHotbar(EventBus bus) {
        super(bus, "SmoothHotbar", "Плавное выделение слота в хотбаре", Category.HUD);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        HotbarSmooth.active = true;
        HotbarSmooth.pos = -1f;
    }

    @Override
    public void onTick() {
        HotbarSmooth.speed = smoothing.getFloat();
    }

    @Override
    public void onDisable() {
        HotbarSmooth.active = false;
        super.onDisable();
    }
}
