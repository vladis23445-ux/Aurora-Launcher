package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;

/** Crosshair: свой прицел — точка, крест или кольцо. */
public class Crosshair extends Module {

    public enum Style {
        DOT,
        CROSS,
        RING,
    }

    private final dev.aurora.module.setting.EnumSetting<Style> style =
            register(new dev.aurora.module.setting.EnumSetting<>("Style", Style.CROSS));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFFEDEFF9));
    private final NumberSetting size = register(new NumberSetting("Size", 6.0, 2.0, 16.0, 1));
    private final NumberSetting thickness = register(new NumberSetting("Thickness", 1.5, 0.5, 4.0, 0.5));
    private final BooleanSetting dot = register(new BooleanSetting("CenterDot", true));

    public Crosshair(EventBus bus) {
        super(bus, "Crosshair", "Свой прицел с настройкой формы и цвета", Category.RENDER);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        int argb = color.getArgb();
        float s = size.getFloat();
        float t = thickness.getFloat();

        switch (style.getValue()) {
            case DOT -> Render2D.roundedRect(context, cx - t, cy - t, t * 2f, t * 2f, t, argb);
            case RING -> {
                int segments = 28;
                float radius = s;
                float previousX = 0f;
                float previousY = 0f;
                for (int i = 0; i <= segments; i++) {
                    double angle = Math.PI * 2.0 * i / segments;
                    float px = cx + (float) Math.cos(angle) * radius;
                    float py = cy + (float) Math.sin(angle) * radius;
                    if (i > 0) {
                        Render2D.rect(context, Math.min(previousX, px), Math.min(previousY, py),
                                Math.max(t, Math.abs(px - previousX)), Math.max(t, Math.abs(py - previousY)), argb);
                    }
                    previousX = px;
                    previousY = py;
                }
            }
            default -> {
                Render2D.rect(context, cx - s, cy - t / 2f, s * 2f, t, argb);
                Render2D.rect(context, cx - t / 2f, cy - s, t, s * 2f, argb);
            }
        }

        if (dot.getValue() && style.getValue() != Style.DOT) {
            Render2D.roundedRect(context, cx - 1f, cy - 1f, 2f, 2f, 1f, argb);
        }
    }
}
