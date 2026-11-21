package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.event.Events;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HitParticles: объёмный эффект удара — сферы, звёздочки, расходящееся кольцо
 * и хитмаркер в центре экрана. Никаких ванильных квадратных частиц.
 */
public class HitParticles extends Module {

    private final ColorSetting normalColor = register(new ColorSetting("NormalColor", 0xFF5CF0C0));
    private final ColorSetting critColor = register(new ColorSetting("CritColor", 0xFFFFC24A));
    private final NumberSetting amount = register(new NumberSetting("Amount", 18, 4, 80, 1));
    private final NumberSetting strength = register(new NumberSetting("Strength", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting life = register(new NumberSetting("Life", 0.75, 0.2, 2.0, 0.05));
    private final NumberSetting size = register(new NumberSetting("Size", 0.075, 0.01, 0.2, 0.005));
    private final BooleanSetting stars = register(new BooleanSetting("Stars", true));
    private final BooleanSetting ring = register(new BooleanSetting("Ring", true));
    private final BooleanSetting marker = register(new BooleanSetting("HitMarker", true));

    private final List<Particle> particles = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private long hitAt;
    private int hitColor = 0xFF5CF0C0;

    public HitParticles(EventBus bus) {
        super(bus, "HitParticles", "Сферы, звёзды и кольцо при ударе", Category.COMBAT);
        bus.subscribe(Events.Attack.class, this::onAttack);
    }

    private void onAttack(Events.Attack event) {
        if (!isEnabled()) {
            return;
        }
        int palette = event.critical() ? critColor.getArgb() : normalColor.getArgb();
        int total = amount.getInt();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Vec3d center = new Vec3d(
                event.target().getX(),
                event.target().getY() + event.target().getHeight() * 0.6,
                event.target().getZ()
        );

        for (int i = 0; i < total; i++) {
            double theta = random.nextDouble(0, Math.PI * 2);
            double phi = random.nextDouble(-0.6, 1.0);
            double speed = (0.12 + random.nextDouble() * 0.28) * strength.getValue();

            Vec3d velocity = new Vec3d(
                    Math.cos(theta) * Math.cos(phi) * speed,
                    Math.sin(phi) * speed + 0.06,
                    Math.sin(theta) * Math.cos(phi) * speed
            );

            int color = ColorUtil.lerp(palette, 0xFFFFFFFF, (float) random.nextDouble(0, 0.55));
            boolean star = stars.getValue() && random.nextFloat() < 0.35f;
            particles.add(new Particle(center, velocity, color, star,
                    (float) (life.getValue() * random.nextDouble(0.7, 1.3)),
                    size.getFloat()));
        }

        if (ring.getValue()) {
            rings.add(new Ring(center, System.currentTimeMillis(), palette));
        }

        hitAt = System.currentTimeMillis();
        hitColor = palette;
    }

    @Override
    public void onDisable() {
        particles.clear();
        rings.clear();
    }

    @Override
    public void onTick() {
        if (!particles.isEmpty()) {
            particles.removeIf(p -> {
                p.age += 1f / 20f;
                p.velocity = p.velocity.multiply(0.90).add(0, -0.055, 0);
                p.position = p.position.add(p.velocity);
                return p.age >= p.maxAge;
            });
        }
        if (!rings.isEmpty()) {
            long now = System.currentTimeMillis();
            rings.removeIf(r -> now - r.born > 420L);
        }
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        long now = System.currentTimeMillis();

        if (marker.getValue() && now - hitAt < 170L) {
            float fade = 1f - (now - hitAt) / 170f;
            drawHitMarker(context, fade, hitColor);
        }

        for (Ring ring : rings) {
            float progress = Math.min(1f, (now - ring.born) / 420f);
            float radius = 0.15f + progress * 1.1f;
            int color = ColorUtil.withAlpha(ring.color, (1f - progress) * 0.7f);
            ScreenProjection.ring(context, ring.center, radius, 30, 0f, progress * 2f,
                    2.2f, color, color);
        }

        if (particles.isEmpty()) {
            return;
        }

        if (mc.player != null) {
            particles.sort(Comparator.comparingDouble(p -> -p.position.squaredDistanceTo(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        }

        for (Particle p : particles) {
            float progress = p.age / Math.max(0.001f, p.maxAge);
            float fade = 1f - progress;
            float currentSize = p.size * (0.4f + fade * 0.9f);

            int core = ColorUtil.withAlpha(p.color, fade);
            int glow = ColorUtil.withAlpha(p.color, fade * 0.25f);

            if (p.star) {
                ScreenProjection.star(context, p.position, currentSize, core, glow);
            } else {
                ScreenProjection.orb(context, p.position, currentSize, core, glow);
            }
        }
    }

    private void drawHitMarker(DrawContext context, float fade, int color) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        int c = ColorUtil.withAlpha(color, fade * 0.9f);
        float inner = 3f + (1f - fade) * 4f;
        float outer = 8f + (1f - fade) * 6f;

        ScreenProjection.gradientLine(context, cx - outer, cy - outer, cx - inner, cy - inner, 1.6f, c, c);
        ScreenProjection.gradientLine(context, cx + inner, cy + inner, cx + outer, cy + outer, 1.6f, c, c);
        ScreenProjection.gradientLine(context, cx - outer, cy + outer, cx - inner, cy + inner, 1.6f, c, c);
        ScreenProjection.gradientLine(context, cx + inner, cy - inner, cx + outer, cy - outer, 1.6f, c, c);
    }

    private static final class Ring {
        final Vec3d center;
        final long born;
        final int color;

        Ring(Vec3d center, long born, int color) {
            this.center = center;
            this.born = born;
            this.color = color;
        }
    }

    private static final class Particle {
        Vec3d position;
        Vec3d velocity;
        final int color;
        final boolean star;
        final float maxAge;
        final float size;
        float age;

        Particle(Vec3d position, Vec3d velocity, int color, boolean star, float maxAge, float size) {
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.star = star;
            this.maxAge = maxAge;
            this.size = size;
        }
    }
}
