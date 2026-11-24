package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.NumberSetting;
import net.minecraft.client.gui.screen.DeathScreen;

/** AutoRespawn: автоматически возвращает в мир после смерти. */
public class AutoRespawn extends Module {

    private final NumberSetting delay = register(new NumberSetting("Delay", 0.4, 0.0, 5.0, 0.1));

    private long deathAt;

    public AutoRespawn(EventBus bus) {
        super(bus, "AutoRespawn", "Автоматическое возрождение после смерти", Category.MISC);
    }

    @Override
    public void onDisable() {
        deathAt = 0L;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen == null) {
            deathAt = 0L;
            return;
        }
        if (!(mc.currentScreen instanceof DeathScreen)) {
            deathAt = 0L;
            return;
        }
        if (deathAt == 0L) {
            deathAt = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - deathAt < (long) (delay.getValue() * 1000)) {
            return;
        }
        try {
            mc.player.requestRespawn();
            mc.setScreen(null);
        } catch (Throwable ignored) {
            deathAt = 0L;
        }
    }
}
