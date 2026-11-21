package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.event.Events;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Jump Circles: расширяющиеся светящиеся кольца на земле в точке прыжка.
 * Хранится только (позиция, время старта) — анимация считается в рендере.
 */
public class JumpCircles extends Module {

    private final ColorSetting color = register(new ColorSetting("Color", 0xFF3BE8A0));
    private final ColorSetting second = register(new ColorSetting("SecondColor", 0xFF8CFFC8));
    private final NumberSetting maxRadius = register(new NumberSetting("MaxRadius", 1.6, 0.4, 4.0, 0.1));
    private final NumberSetting duration = register(new NumberSetting("Duration", 0.85, 0.2, 2.5, 0.05));
    private final NumberSetting thickness = register(new NumberSetting("Thickness", 1.4, 0.5, 5.0, 0.1));

    private final List<Circle> circles = new ArrayList<>();

    public JumpCircles(EventBus bus) {
        super(bus, "JumpCircles", "Расширяющиеся круги при прыжке", Category.RENDER);
        bus.subscribe(Events.Jump.class, this::onJump);
    }

    private void onJump(Events.Jump event) {
        if (!isEnabled()) {
            return;
        }
        circles.add(new Circle(new Vec3d(event.position().x, event.position().y + 0.02, event.position().z),
                System.currentTimeMillis(), color.getArgb(), second.getArgb()));
    }

    @Override
    public void onDisable() {
        circles.clear();
    }

    @Override
    public void onTick() {
        if (circles.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long life = (long) (duration.getValue() * 1000);
        circles.removeIf(c -> now - c.born > life);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (circles.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        float life = (float) (duration.getValue() * 1000);

        for (Circle circle : circles) {
            float progress = Math.min(1f, (now - circle.born) / life);
            float eased = (float) (1f - Math.pow(1f - progress, 3f));
            float radius = 0.15f + maxRadius.getFloat() * eased;
            float fade = 1f - progress;

            ScreenProjection.ring(context, circle.position, radius, 34, 0f, progress * 1.5f,
                    thickness.getFloat(),
                    ColorUtil.withAlpha(circle.colorA, fade * 0.95f),
                    ColorUtil.withAlpha(circle.colorB, fade * 0.65f));

            if (progress > 0.2f) {
                float secondProgress = (progress - 0.2f) / 0.8f;
                float secondRadius = 0.15f + maxRadius.getFloat() * 0.75f
                        * (float) (1f - Math.pow(1f - secondProgress, 3f));
                float secondFade = 1f - secondProgress;
                ScreenProjection.ring(context, circle.position, secondRadius, 28, 0f, -progress * 2f,
                        thickness.getFloat() * 0.7f,
                        ColorUtil.withAlpha(circle.colorB, secondFade * 0.55f),
                        ColorUtil.withAlpha(circle.colorA, secondFade * 0.30f));
            }
        }
    }

    private static final class Circle {
        final Vec3d position;
        final long born;
        final int colorA;
        final int colorB;

        Circle(Vec3d position, long born, int colorA, int colorB) {
            this.position = position;
            this.born = born;
            this.colorA = colorA;
            this.colorB = colorB;
        }
    }
}
