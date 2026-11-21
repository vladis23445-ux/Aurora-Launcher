package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.EnumSetting;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Optimization: снижает нагрузку ванильными опциями и восстанавливает их при выключении.
 */
public class Optimization extends Module {

    private final EnumSetting<ParticlesMode> particles =
            register(new EnumSetting<>("Particles", ParticlesMode.MINIMAL));
    private final BooleanSetting noClouds = register(new BooleanSetting("NoClouds", true));
    private final BooleanSetting noShadows = register(new BooleanSetting("NoEntityShadows", true));
    private final BooleanSetting noBobbing = register(new BooleanSetting("NoViewBobbing", true));
    private final BooleanSetting lightWeather = register(new BooleanSetting("LightWeather", true));

    private ParticlesMode oldParticles;
    private CloudRenderMode oldClouds;
    private Boolean oldShadows;
    private Boolean oldBobbing;
    private Integer oldWeatherRadius;

    public Optimization(EventBus bus) {
        super(bus, "Optimization", "Снижает нагрузку: частицы, облака, тени", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (mc.options == null) {
            return;
        }
        try {
            oldParticles = mc.options.getParticles().getValue();
            oldClouds = mc.options.getCloudRenderMode().getValue();
            oldShadows = mc.options.getEntityShadows().getValue();
            oldBobbing = mc.options.getBobView().getValue();
        } catch (Throwable ignored) {
        }
        dev.aurora.render.ScreenProjection.setQuality(0.7f);
        apply();
    }

    @Override
    public void onDisable() {
        dev.aurora.render.ScreenProjection.setQuality(1f);
        if (mc.options == null) {
            return;
        }
        try {
            if (oldParticles != null) {
                mc.options.getParticles().setValue(oldParticles);
            }
            if (oldClouds != null) {
                mc.options.getCloudRenderMode().setValue(oldClouds);
            }
            if (oldShadows != null) {
                mc.options.getEntityShadows().setValue(oldShadows);
            }
            if (oldBobbing != null) {
                mc.options.getBobView().setValue(oldBobbing);
            }
            if (oldWeatherRadius != null) {
                mc.options.getWeatherRadius().setValue(oldWeatherRadius);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onTick() {
        // на случай, если настройки перезаписали извне
        dev.aurora.render.ScreenProjection.setQuality(0.7f);
        apply();
    }

    private void apply() {
        if (mc.options == null) {
            return;
        }
        try {
            mc.options.getParticles().setValue(particles.getValue());
            if (noClouds.getValue()) {
                mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            }
            mc.options.getEntityShadows().setValue(!noShadows.getValue());
            mc.options.getBobView().setValue(!noBobbing.getValue());
            if (lightWeather.getValue() && oldWeatherRadius == null) {
                oldWeatherRadius = mc.options.getWeatherRadius().getValue();
                mc.options.getWeatherRadius().setValue(5);
            }
        } catch (Throwable ignored) {
        }
    }
}
