package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import dev.aurora.util.CameraUtil;
import dev.aurora.util.TargetManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Target ESP: вращающееся 3D-кольцо вокруг цели + контур бокса.
 * Геометрия считается в мире и проецируется камерой (см. ScreenProjection).
 */
public class TargetEsp extends Module {

    private final ColorSetting color = register(new ColorSetting("Color", 0xFF3BE8A0));
    private final ColorSetting accent = register(new ColorSetting("Accent", 0xFF40D6D6));
    private final NumberSetting radius = register(new NumberSetting("Radius", 0.62, 0.3, 1.6, 0.01));
    private final NumberSetting spin = register(new NumberSetting("Spin", 1.4, 0.0, 6.0, 0.1));
    private final NumberSetting tilt = register(new NumberSetting("Tilt", 24.0, 0.0, 80.0, 1.0));
    private final NumberSetting thickness = register(new NumberSetting("Thickness", 1.2, 0.5, 5.0, 0.1));
    private final BooleanSetting box = register(new BooleanSetting("Box", true));
    private final BooleanSetting playersOnly = register(new BooleanSetting("PlayersOnly", true));

    public TargetEsp(EventBus bus) {
        super(bus, "TargetESP", "Вращающееся кольцо и контур вокруг цели", Category.COMBAT);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        LivingEntity target = TargetManager.getTarget();
        if (target == null || !target.isAlive() || mc.player == null) {
            return;
        }
        if (playersOnly.getValue() && !(target instanceof PlayerEntity)) {
            return;
        }

        Vec3d pos = CameraUtil.lerped(target);
        Vec3d center = new Vec3d(pos.x, pos.y + target.getHeight() * 0.55, pos.z);

        float time = (float) ((System.currentTimeMillis() % 1_000_000L) / 1000.0);
        float angle = time * spin.getFloat();
        float tiltRad = (float) Math.toRadians(tilt.getValue());

        int c1 = color.getArgb();
        int c2 = accent.getArgb();

        // внешнее свечение
        ScreenProjection.ring(context, center, radius.getFloat() + 0.05f, 36, tiltRad,
                angle + 0.2f, 6.0f,
                ColorUtil.withAlpha(c1, 0.16f), ColorUtil.withAlpha(c2, 0.16f));

        // основное кольцо
        ScreenProjection.ring(context, center, radius.getFloat(), 44, tiltRad, angle,
                thickness.getFloat(), c1, c2);

        // второе кольцо под углом — даёт объём
        ScreenProjection.ring(context, center, radius.getFloat() * 0.82f, 30,
                tiltRad + 1.2f, -angle * 0.7f, thickness.getFloat() * 0.8f,
                ColorUtil.withAlpha(c2, 0.85f), ColorUtil.withAlpha(c1, 0.55f));

        if (box.getValue()) {
            ScreenProjection.box(context, target.getBoundingBox(), 0.06f, 1.4f,
                    ColorUtil.withAlpha(c1, 0.8f),
                    ColorUtil.withAlpha(c2, 0.35f));
        }
    }
}
