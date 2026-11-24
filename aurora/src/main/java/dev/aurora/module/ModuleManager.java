package dev.aurora.module;

import dev.aurora.Aurora;
import dev.aurora.event.EventBus;
import dev.aurora.modules.ArmorHud;
import dev.aurora.modules.AutoSprint;
import dev.aurora.modules.BlockHighlight;
import dev.aurora.modules.FlatItems;
import dev.aurora.modules.Fullbright;
import dev.aurora.modules.Zoom;
import dev.aurora.modules.CosmeticsRender;
import dev.aurora.modules.CustomHand;
import dev.aurora.modules.QuestsHud;
import dev.aurora.modules.DamageNumbers;
import dev.aurora.modules.HitParticles;
import dev.aurora.modules.JumpCircles;
import dev.aurora.modules.HeldShader;
import dev.aurora.modules.ItemEsp;
import dev.aurora.modules.KillAura;
import dev.aurora.modules.KillEffects;
import dev.aurora.modules.ItemSwitch;
import dev.aurora.modules.Keystrokes;
import dev.aurora.modules.Optimization;
import dev.aurora.modules.SmoothHotbar;
import dev.aurora.modules.TargetEsp;
import dev.aurora.modules.TargetHud;
import dev.aurora.modules.TargetInfo;
import dev.aurora.modules.Trails;
import dev.aurora.modules.Watermark;
import dev.aurora.modules.WorldParticles;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {

    private final EventBus bus;
    private final List<Module> modules = new ArrayList<>();
    private final Map<Category, List<Module>> byCategory = new EnumMap<>(Category.class);

    public ModuleManager(EventBus bus) {
        this.bus = bus;
    }

    public void init() {
        register(new TargetEsp(bus));
        register(new TargetHud(bus));
        register(new TargetInfo(bus));
        register(new Trails(bus));
        register(new BlockHighlight(bus));
        register(new FlatItems(bus));
        register(new ItemEsp(bus));
        register(new HeldShader(bus));
        register(new KillAura(bus));
        register(new KillEffects(bus));
        register(new WorldParticles(bus));
        register(new HitParticles(bus));
        register(new ItemSwitch(bus));
        register(new CosmeticsRender(bus));
        register(new CustomHand(bus));
        register(new DamageNumbers(bus));
        register(new JumpCircles(bus));
        register(new QuestsHud(bus));
        register(new Watermark(bus));
        register(new ArmorHud(bus));
        register(new Keystrokes(bus));
        register(new SmoothHotbar(bus));
        register(new Optimization(bus));
        register(new Fullbright(bus));
        register(new Zoom(bus));
        register(new AutoSprint(bus));

        for (Category category : Category.values()) {
            List<Module> list = new ArrayList<>();
            for (Module module : modules) {
                if (module.getCategory() == category) {
                    list.add(module);
                }
            }
            byCategory.put(category, Collections.unmodifiableList(list));
        }
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /** Списки по категориям собираются один раз при инициализации. */
    public List<Module> byCategory(Category category) {
        return byCategory.getOrDefault(category, List.of());
    }

    public Module byName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    public void onTick() {
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            if (!module.isEnabled()) {
                continue;
            }
            try {
                module.onTick();
            } catch (Throwable t) {
                Aurora.LOGGER.error("Tick failed in module {}", module.getName(), t);
            }
        }
    }

    public void onHudRender(DrawContext context, float tickDelta) {
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            if (!module.isEnabled()) {
                continue;
            }
            try {
                module.onHudRender(context, tickDelta);
            } catch (Throwable t) {
                Aurora.LOGGER.error("HUD render failed in module {}", module.getName(), t);
            }
        }
    }
}
