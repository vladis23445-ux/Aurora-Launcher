package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.util.FlatItemsState;

/**
 * FlatItems: дропнутые предметы лежат на месте, без вращения и «крутилки».
 */
public class FlatItems extends Module {

    public FlatItems(EventBus bus) {
        super(bus, "FlatItems", "Предметы на земле не крутятся", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        FlatItemsState.active = true;
    }

    @Override
    public void onDisable() {
        FlatItemsState.active = false;
    }
}
