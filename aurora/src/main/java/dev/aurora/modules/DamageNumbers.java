package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.event.Events;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.util.CameraUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Damage Numbers: цифры урона/лечения над сущностями.
 * Мир → экран считается вручную (CameraUtil.project), текст рисуется в HUD-проходе,
 * поэтому модуль не зависит от 3D-текстового API, который часто меняется.
 */
public class DamageNumbers extends Module {

    private final ColorSetting damageColor = register(new ColorSetting("DamageColor", 0xFFFF5A6E));
    private final ColorSetting critColor = register(new ColorSetting("CritColor", 0xFFFFC24A));
    private final ColorSetting healColor = register(new ColorSetting("HealColor", 0xFF4BE38A));
    private final NumberSetting lifetime = register(new NumberSetting("Lifetime", 1.1, 0.3, 3.0, 0.1));
    private final NumberSetting rise = register(new NumberSetting("Rise", 0.9, 0.1, 3.0, 0.1));
    private final BooleanSetting onlyPlayers = register(new BooleanSetting("PlayersOnly", false));
    private final BooleanSetting critScale = register(new BooleanSetting("CritBigger", true));

    private final List<Floating> items = new ArrayList<>();

    public DamageNumbers(EventBus bus) {
        super(bus, "DamageNumbers", "Всплывающие цифры урона и лечения", Category.RENDER);
        bus.subscribe(Events.HealthChange.class, this::onHealthChange);
    }

    private void onHealthChange(Events.HealthChange event) {
        if (!isEnabled()) {
            return;
        }
        LivingEntity entity = event.entity();
        if (entity == mc.player || !entity.isAlive()) {
            return;
        }
        if (onlyPlayers.getValue() && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
            return;
        }

        float delta = event.delta();
        boolean heal = delta < 0;
        float value = Math.abs(delta);
        if (value < 0.5f) {
            return;
        }

        boolean critical = !heal && value >= 6.5f;
        int color = heal ? healColor.getArgb() : (critical ? critColor.getArgb() : damageColor.getArgb());

        String text = (heal ? "+" : "-") + format(value);
        Vec3d base = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() + 0.35, entity.getZ());

        items.add(new Floating(base, text, color, critical, (float) (System.currentTimeMillis() / 1000.0)));
    }

    private String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    @Override
    public void onDisable() {
        items.clear();
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (items.isEmpty()) {
            return;
        }

        double now = System.currentTimeMillis() / 1000.0;
        double life = lifetime.getValue();
        items.removeIf(f -> now - f.born > life);

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        for (Floating f : items) {
            double age = now - f.born;
            float progress = (float) (age / life);
            float alpha = progress < 0.15f ? progress / 0.15f : 1f - (progress - 0.15f) / 0.85f;
            alpha = Math.max(0f, Math.min(1f, alpha));

            Vec3d pos = f.position.add(0, rise.getValue() * progress, 0);
            float[] screen = CameraUtil.project(pos, screenW, screenH);
            if (screen == null) {
                continue;
            }

            int color = ColorUtil.withAlpha(f.color, alpha * 0.95f);
            int shadow = ColorUtil.replaceAlpha(0x000000, (int) (110 * alpha));

            float scaleFactor = f.critical && critScale.getValue() ? 1.35f : 1f;
            String text = f.text;

            float width = Render2D.textWidth(text) * scaleFactor;
            float x = screen[0] - width / 2f;
            float y = screen[1];

            // псевдо-масштаб: дублируем текст со смещением, если нужен крупный кегль
            if (scaleFactor > 1.2f) {
                Render2D.textShadow(context, text, x + 1, y + 1, shadow);
                Render2D.text(context, text, x, y, color);
                Render2D.text(context, text, x + 0.7f, y, ColorUtil.withAlpha(color, alpha * 0.6f));
            } else {
                Render2D.textShadow(context, text, x + 1, y + 1, shadow);
                Render2D.text(context, text, x, y, color);
            }
        }
    }

    private record Floating(Vec3d position, String text, int color, boolean critical, double born) {
    }
}
