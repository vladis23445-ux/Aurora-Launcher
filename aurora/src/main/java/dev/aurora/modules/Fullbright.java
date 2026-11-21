package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.util.FullbrightState;

/**
 * Fullbright: видно всё в пещерах и ночью.
 * Гамма подменяется на чтении (ванильный ползунок ограничен единицей),
 * darkness-эффект отключается отдельным миксином на lightmap.
 */
public class Fullbright extends Module {

    private final NumberSetting level = register(new NumberSetting("Gamma", 15.0, 1.0, 20.0, 0.5));
    private final BooleanSetting noDarkness = register(new BooleanSetting("NoDarkness", true));

    public Fullbright(EventBus bus) {
        super(bus, "Fullbright", "Видно всё в темноте без зелий", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        FullbrightState.active = true;
        apply();
    }

    @Override
    public void onTick() {
        if (isEnabled()) {
            apply();
        }
    }

    private void apply() {
        FullbrightState.gamma = level.getValue();
        FullbrightState.active = true;
    }

    @Override
    public void onDisable() {
        FullbrightState.active = false;
        super.onDisable();
    }
}
