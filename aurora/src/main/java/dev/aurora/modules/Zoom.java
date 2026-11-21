package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.NumberSetting;

/** Zoom: плавно уменьшает FOV, пока модуль включён. */
public class Zoom extends Module {

    private final NumberSetting zoomFov = register(new NumberSetting("FOV", 30.0, 10.0, 70.0, 1.0));
    private final NumberSetting speed = register(new NumberSetting("Speed", 0.25, 0.05, 1.0, 0.01));

    private Integer previousFov;
    private float current;

    public Zoom(EventBus bus) {
        super(bus, "Zoom", "Плавное приближение камеры", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (mc.options == null) {
            return;
        }
        previousFov = mc.options.getFov().getValue();
        current = previousFov;
    }

    @Override
    public void onTick() {
        if (mc.options == null) {
            return;
        }
        float target = zoomFov.getFloat();
        current += (target - current) * speed.getFloat();
        mc.options.getFov().setValue(Math.round(current));
    }

    @Override
    public void onDisable() {
        if (mc.options != null && previousFov != null) {
            mc.options.getFov().setValue(previousFov);
        }
    }
}
