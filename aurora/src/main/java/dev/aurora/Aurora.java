package dev.aurora;

import dev.aurora.config.ConfigManager;
import dev.aurora.event.EventBus;
import dev.aurora.event.Events;
import dev.aurora.module.Module;
import dev.aurora.module.ModuleManager;
import dev.aurora.ui.ClickGuiScreen;
import dev.aurora.ui.ScreenStyler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public final class Aurora implements ClientModInitializer {

    public static final String MOD_ID = "aurora";
    public static final String NAME = "Aurora";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aurora");

    /** Right Shift. */
    public static final int GUI_KEY = 344;

    private static Aurora instance;

    private final EventBus bus = new EventBus();
    private final Set<Integer> heldKeys = new HashSet<>();
    private ModuleManager modules;
    private ConfigManager config;
    private boolean guiKeyHeld;

    public static Aurora get() {
        return instance;
    }

    public EventBus getBus() {
        return bus;
    }

    public ModuleManager getModules() {
        return modules;
    }

    public ConfigManager getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        modules = new ModuleManager(bus);
        modules.init();

        config = new ConfigManager(modules);
        config.load();

        ScreenStyler.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            bus.post(new Events.Tick());
            pollKeys(client);
            modules.onTick();
        });

        LOGGER.info("{} loaded: {} modules", NAME, modules.getModules().size());
    }

    public void saveConfig() {
        if (config != null) {
            config.save();
        }
    }

    /**
     * Клавиши опрашиваем в тике: в 1.21.11 у Keyboard больше нет onKey,
     * а InputUtil.isKeyPressed отдаёт состояние напрямую — так надёжнее миксинов.
     */
    private void pollKeys(MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }

        boolean guiDown = InputUtil.isKeyPressed(client.getWindow(), GUI_KEY);
        if (guiDown && !guiKeyHeld && client.currentScreen == null) {
            client.setScreen(new ClickGuiScreen());
        }
        guiKeyHeld = guiDown;

        if (client.currentScreen != null) {
            heldKeys.clear();
            return;
        }

        for (Module module : modules.getModules()) {
            int key = module.getKey();
            if (key <= 0) {
                continue;
            }
            boolean down = InputUtil.isKeyPressed(client.getWindow(), key);
            boolean wasDown = heldKeys.contains(key);
            if (down && !wasDown) {
                module.toggle();
            }
            if (down) {
                heldKeys.add(key);
            } else {
                heldKeys.remove(key);
            }
        }
    }

    /** Вызывается из InGameHudMixin. */
    public void onHudRender(DrawContext context, float tickDelta) {
        if (MinecraftClient.getInstance().options.hudHidden) {
            return;
        }
        modules.onHudRender(context, tickDelta);
    }
}
