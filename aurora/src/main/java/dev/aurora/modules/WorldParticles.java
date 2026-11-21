package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.EnumSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import dev.aurora.util.CameraUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/**
 * Мирные частицы вокруг игрока: светлячки и снежинки.
 * Позиции выводятся из времени и индекса, поэтому нет хранения и GC-мусора.
 */
public class WorldParticles extends Module {

    public enum Mode {
        FIREFLY, SNOW, BOTH
    }

    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", Mode.FIREFLY));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFF9CFFC8));
    private final NumberSetting count = register(new NumberSetting("Count", 28, 8, 200, 1));
    private final NumberSetting radius = register(new NumberSetting("Radius", 9.0, 3.0, 24.0, 0.5));
    private final NumberSetting size = register(new NumberSetting("Size", 0.06, 0.01, 0.2, 0.005));
    private final NumberSetting speed = register(new NumberSetting("Speed", 1.0, 0.1, 4.0, 0.1));

    public WorldParticles(EventBus bus) {
        super(bus, "WorldParticles", "Светлячки и снежинки в мире", Category.RENDER);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null) {
            return;
        }

        Vec3d origin = CameraUtil.lerped(mc.player);
        double time = (System.currentTimeMillis() % 1_000_000L) / 1000.0 * speed.getValue();
        int total = count.getInt();
        float worldSize = size.getFloat();

        boolean firefly = mode.getValue() != Mode.SNOW;
        boolean snow = mode.getValue() != Mode.FIREFLY;

        if (firefly) {
            int base = color.getArgb();
            for (int i = 0; i < total; i++) {
                double seed = i * 2.399963229728653;
                double r = radius.getValue() * Math.sqrt((i + 1) / (double) total);
                double angle = seed + time * 0.25;
                double x = origin.x + Math.cos(angle) * r;
                double z = origin.z + Math.sin(angle) * r;
                double bob = Math.sin(time * 1.4 + i * 0.7) * 0.55;
                double y = origin.y + 1.1 + bob + Math.sin(i * 1.7) * 1.2;

                float pulse = 0.55f + 0.45f * (float) Math.sin(time * 3.1 + i * 1.3);
                int core = ColorUtil.withAlpha(base, 0.25f + 0.55f * pulse);
                int glow = ColorUtil.withAlpha(base, 0.05f + 0.10f * pulse);

                ScreenProjection.particle(context, new Vec3d(x, y, z),
                        worldSize * (0.7f + pulse * 0.6f), core, glow);
            }
        }

        if (snow) {
            int white = 0xFFEAF2FF;
            int snowCount = mode.getValue() == Mode.BOTH ? total / 2 : total;
            for (int i = 0; i < snowCount; i++) {
                double r = radius.getValue() * 0.8 * Math.sqrt((i + 1) / (double) snowCount);
                double angle = i * 2.399963229728653 - time * 0.05;
                double fall = (time * 1.6 + i * 0.9) % 8.0;
                double x = origin.x + Math.cos(angle) * r + Math.sin(time * 0.6 + i) * 0.4;
                double z = origin.z + Math.sin(angle) * r + Math.cos(time * 0.5 + i) * 0.4;
                double y = origin.y + 7.0 - fall;

                float fade = (float) (1.0 - Math.abs(fall - 4.0) / 4.0);
                int c = ColorUtil.withAlpha(white, Math.max(0.08f, fade * 0.75f));

                ScreenProjection.particle(context, new Vec3d(x, y, z),
                        worldSize * 0.55f, c, c);
            }
        }
    }
}
