package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.util.HeldShaderState;

/**
 * HeldShader: цветная обводка предмета в руке.
 * Рисуется копия модели чуть больше оригинала, поэтому виден только контур.
 */
public class HeldShader extends Module {

    private final ColorSetting color = register(new ColorSetting("Color", 0xFF3BE8A0));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", 70.0, 10.0, 255.0, 5.0));
    private final NumberSetting scale = register(new NumberSetting("Scale", 1.00, 1.00, 1.20, 0.01));
    private final BooleanSetting pulse = register(new BooleanSetting("Pulse", false));

    private long lastFrameNs;
    private float time;

    public HeldShader(EventBus bus) {
        super(bus, "HeldShader", "Обводка предмета в руке", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onHudRender(net.minecraft.client.gui.DrawContext context, float tickDelta) {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.016f : Math.min(0.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        time += dt;

        float a = alpha.getFloat() / 255f;
        if (pulse.getValue()) {
            a *= 0.65f + 0.35f * (float) Math.sin(time * 2.6);
        }

        HeldShaderState.argb = ColorUtil.replaceAlpha(color.getArgb(), Math.round(a * 255f));
        HeldShaderState.scale = 1f;
        HeldShaderState.active = true;
    }

    @Override
    public void onDisable() {
        HeldShaderState.active = false;
        super.onDisable();
    }
}
