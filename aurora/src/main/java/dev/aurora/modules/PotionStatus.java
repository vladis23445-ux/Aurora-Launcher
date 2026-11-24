package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** PotionStatus: активные эффекты с оставшимся временем. */
public class PotionStatus extends Module {

    private final BooleanSetting romanLevel = register(new BooleanSetting("RomanLevel", true));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 6.0, 0.0, 600.0, 1));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 300.0, 0.0, 900.0, 1));

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};

    public PotionStatus(EventBus bus) {
        super(bus, "PotionStatus", "Эффекты и их время на экране", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null) {
            return;
        }

        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
        if (effects.isEmpty()) {
            return;
        }
        effects.sort((a, b) -> Integer.compare(a.getDuration(), b.getDuration()));

        float x = offsetX.getFloat();
        float y = offsetY.getFloat();
        float rowHeight = 16f;

        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().value().getName().getString();
            String level = romanLevel.getValue() ? roman(effect.getAmplifier()) : " " + (effect.getAmplifier() + 1);
            String clock = clock(effect.getDuration());
            String label = name + level + "  " + clock;

            float width = Render2D.textWidth(label) + 14f;
            Render2D.roundedRect(context, x, y, width, 13f, 6.5f, 0x7015151A);
            Render2D.roundedBorder(context, x, y, width, 13f, 6.5f, 1f, ColorUtil.withAlpha(0xFFFFFF, 0.07f));
            Render2D.text(context, label, x + 7f, y + 3f, ColorUtil.withAlpha(0xFF9CFFD0, 0.9f));

            y += rowHeight;
        }
    }

    private static String roman(int amplifier) {
        int index = Math.max(0, Math.min(ROMAN.length - 1, amplifier));
        return amplifier <= 0 ? "" : " " + ROMAN[index];
    }

    private static String clock(int ticks) {
        int total = ticks / 20;
        int minutes = total / 60;
        int seconds = total % 60;
        return minutes > 0
                ? String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds)
                : String.format(java.util.Locale.ROOT, "%ds", seconds);
    }
}
