package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.EnumSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillEffects: анимации убийства моба или игрока.
 * Несколько стилей: души, кольцо, столб света, разлёт искр.
 */
public class KillEffects extends Module {

    public enum Style {
        SOUL,
        RING,
        BEAM,
        BURST,
        MIX,
    }

    private final EnumSetting<Style> style = register(new EnumSetting<>("Style", Style.MIX));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFF3BE8A0));
    private final NumberSetting duration = register(new NumberSetting("Duration", 1.4, 0.4, 3.0, 0.1));
    private final NumberSetting power = register(new NumberSetting("Power", 1.0, 0.4, 2.5, 0.1));
    private final BooleanSetting soundless = register(new BooleanSetting("Soundless", true));

    private static KillEffects instance;

    private final List<Effect> effects = new ArrayList<>();
    private long lastFrameNs;

    private static final class Effect {
        final Vec3d origin;
        final Style style;
        final int color;
        final long startNs;
        final float duration;

        Effect(Vec3d origin, Style style, int color, long startNs, float duration) {
            this.origin = origin;
            this.style = style;
            this.color = color;
            this.startNs = startNs;
            this.duration = duration;
        }
    }

    public KillEffects(EventBus bus) {
        super(bus, "KillEffects", "Анимации убийства моба или игрока", Category.RENDER);
        instance = this;
    }

    /** Вызывается из миксина на смерть сущности. */
    public static void onEntityDeath(Vec3d pos) {
        KillEffects module = instance;
        if (module == null || !module.isEnabled()) {
            return;
        }
        module.spawn(pos);
    }

    private void spawn(Vec3d pos) {
        Style chosen = style.getValue();
        if (chosen == Style.MIX) {
            Style[] all = {Style.SOUL, Style.RING, Style.BEAM, Style.BURST};
            chosen = all[ThreadLocalRandom.current().nextInt(all.length)];
        }
        effects.add(new Effect(pos, chosen, color.getArgb(), System.nanoTime(), duration.getFloat()));
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (effects.isEmpty() || mc.world == null) {
            return;
        }

        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.016f : Math.min(0.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;

        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect effect = it.next();
            float age = (now - effect.startNs) / 1.0e9f;
            if (age >= effect.duration) {
                it.remove();
                continue;
            }
            float k = age / effect.duration;
            float p = 1f - k;
            float s = power.getFloat();

            switch (effect.style) {
                case SOUL -> renderSoul(context, effect, k, p, s);
                case RING -> renderRing(context, effect, k, p, s);
                case BEAM -> renderBeam(context, effect, k, p, s);
                case BURST -> renderBurst(context, effect, k, p, s);
                default -> {
                }
            }
        }
    }

    private void renderSoul(DrawContext context, Effect effect, float k, float p, float s) {
        int count = 9;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + k * 1.6;
            double radius = 0.22 * s * (0.5 + k);
            double x = effect.origin.x + Math.cos(angle) * radius;
            double z = effect.origin.z + Math.sin(angle) * radius;
            double y = effect.origin.y + 0.15 + k * 1.5 * s;
            ScreenProjection.orb(context, new Vec3d(x, y, z), 0.075f * p,
                    ColorUtil.withAlpha(effect.color, 0.85f * p),
                    ColorUtil.withAlpha(effect.color, 0.16f * p));
        }
    }

    private void renderRing(DrawContext context, Effect effect, float k, float p, float s) {
        float radius = 0.25f + k * 1.9f * s;
        ScreenProjection.ring(context, new Vec3d(effect.origin.x, effect.origin.y + 0.12, effect.origin.z),
                radius, 44, 1.5707f, 0f, 1.6f * p + 0.4f,
                ColorUtil.withAlpha(effect.color, 0.8f * p),
                ColorUtil.withAlpha(effect.color, 0.1f * p));
    }

    private void renderBeam(DrawContext context, Effect effect, float k, float p, float s) {
        double height = 2.4 * s;
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            double y = effect.origin.y + height * i / steps * (0.4 + k);
            float width = 0.16f * p * (1f - (float) i / steps * 0.5f);
            ScreenProjection.orb(context, new Vec3d(effect.origin.x, y, effect.origin.z), width,
                    ColorUtil.withAlpha(effect.color, 0.75f * p),
                    ColorUtil.withAlpha(effect.color, 0.12f * p));
        }
    }

    private void renderBurst(DrawContext context, Effect effect, float k, float p, float s) {
        int count = 16;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double dist = (0.2 + k * 1.7) * s;
            double x = effect.origin.x + Math.cos(angle) * dist;
            double z = effect.origin.z + Math.sin(angle) * dist;
            double y = effect.origin.y + 0.5 + Math.sin(k * Math.PI) * 0.5 - k * 0.4;
            ScreenProjection.orb(context, new Vec3d(x, y, z), 0.07f * p,
                    ColorUtil.withAlpha(effect.color, 0.9f * p),
                    ColorUtil.withAlpha(effect.color, 0.18f * p));
        }
    }

    @Override
    public void onDisable() {
        effects.clear();
        super.onDisable();
    }
}
