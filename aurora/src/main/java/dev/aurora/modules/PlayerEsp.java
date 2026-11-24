package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.render.ScreenProjection;
import dev.aurora.util.CameraUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** PlayerESP: рамки игроков с ником и здоровьем. */
public class PlayerEsp extends Module {

    private final NumberSetting range = register(new NumberSetting("Range", 48.0, 8.0, 128.0, 1));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFF7C5CFF));
    private final BooleanSetting names = register(new BooleanSetting("Names", true));
    private final BooleanSetting health = register(new BooleanSetting("Health", true));
    private final BooleanSetting box = register(new BooleanSetting("Box", true));

    public PlayerEsp(EventBus bus) {
        super(bus, "PlayerESP", "Рамки игроков с ником и HP", Category.RENDER);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        int base = color.getArgb();
        double limit = range.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) {
                continue;
            }
            if (mc.player.squaredDistanceTo(player) > limit * limit) {
                continue;
            }

            Box bounds = player.getBoundingBox();
            if (box.getValue()) {
                ScreenProjection.box(context, bounds, 0.02f, 1.4f,
                        ColorUtil.withAlpha(base, 0.85f), ColorUtil.withAlpha(base, 0.25f));
            }

            if (!names.getValue() && !health.getValue()) {
                continue;
            }

            Vec3d head = CameraUtil.lerped(player).add(0.0, player.getHeight() + 0.42, 0.0);
            float[] screen = ScreenProjection.project(head);
            if (screen == null) {
                continue;
            }

            String name = player.getName().getString();
            String hp = health.getValue()
                    ? String.format(java.util.Locale.ROOT, "%.0f \u2665", player.getHealth())
                    : "";

            float nameWidth = Render2D.textWidth(name);
            float hpWidth = hp.isEmpty() ? 0f : Render2D.textWidth(hp);
            float width = Math.max(nameWidth, hpWidth) + 8f;
            float height = hp.isEmpty() ? 11f : 20f;
            Render2D.roundedRect(context, screen[0] - width / 2f, screen[1] - 2f, width, height + 2f,
                    5f, 0x66000000);
            Render2D.textCentered(context, name, screen[0], screen[1], ColorUtil.withAlpha(0xFFFFFFFF, 0.92f));
            if (!hp.isEmpty()) {
                Render2D.textCentered(context, hp, screen[0], screen[1] + 9f,
                        ColorUtil.withAlpha(0xFF6BFF9C, 0.9f));
            }
        }
    }
}
