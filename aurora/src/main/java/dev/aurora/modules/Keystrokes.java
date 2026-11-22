package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.Animation;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;

/**
 * Keystrokes: WASD + Р вЂєР С™Р Сљ/Р СџР С™Р Сљ/Space РЎРѓ Р С—Р В»Р В°Р Р†Р Р…Р С•Р в„– Р С—Р С•Р Т‘РЎРѓР Р†Р ВµРЎвЂљР С”Р С•Р в„– Р Р…Р В°Р В¶Р В°РЎвЂљР С‘РЎРЏ.
 */
public class Keystrokes extends Module {

    private final ColorSetting accent = register(new ColorSetting("Accent", 0xFF3BE8A0));
    private final NumberSetting size = register(new NumberSetting("Size", 18.0, 12.0, 30.0, 1.0));
    private final NumberSetting gap = register(new NumberSetting("Gap", 3.0, 1.0, 8.0, 0.5));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 6.0, 0.0, 1200.0, 1));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 0.78, 0.0, 1.0, 0.01));
    private final BooleanSetting mouse = register(new BooleanSetting("Mouse", true));
    private final BooleanSetting space = register(new BooleanSetting("Space", true));

    private final Animation w = new Animation(0f, 16f);
    private final Animation a = new Animation(0f, 16f);
    private final Animation s = new Animation(0f, 16f);
    private final Animation d = new Animation(0f, 16f);
    private final Animation lmb = new Animation(0f, 16f);
    private final Animation rmb = new Animation(0f, 16f);
    private final Animation spaceAnim = new Animation(0f, 16f);

    public Keystrokes(EventBus bus) {
        super(bus, "Keystrokes", "Индикатор нажатых клавиш", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.options == null) {
            return;
        }

        float dt = 1f / 60f;
        float cell = size.getFloat();
        float gapSize = gap.getFloat();
        float x = offsetX.getFloat();
        float y = mc.getWindow().getScaledHeight() * offsetY.getFloat();

        boolean pressedW = mc.options.forwardKey.isPressed();
        boolean pressedA = mc.options.leftKey.isPressed();
        boolean pressedS = mc.options.backKey.isPressed();
        boolean pressedD = mc.options.rightKey.isPressed();

        w.setTarget(pressedW ? 1f : 0f);
        a.setTarget(pressedA ? 1f : 0f);
        s.setTarget(pressedS ? 1f : 0f);
        d.setTarget(pressedD ? 1f : 0f);
        lmb.setTarget(mc.options.attackKey.isPressed() ? 1f : 0f);
        rmb.setTarget(mc.options.useKey.isPressed() ? 1f : 0f);
        spaceAnim.setTarget(mc.options.jumpKey.isPressed() ? 1f : 0f);

        float wv = w.update(dt);
        float av = a.update(dt);
        float sv = s.update(dt);
        float dv = d.update(dt);

        // W
        drawKey(context, "W", x + cell + gapSize, y, cell, wv);
        // A S D
        drawKey(context, "A", x, y + cell + gapSize, cell, av);
        drawKey(context, "S", x + cell + gapSize, y + cell + gapSize, cell, sv);
        drawKey(context, "D", x + (cell + gapSize) * 2, y + cell + gapSize, cell, dv);

        float bottomY = y + (cell + gapSize) * 2;

        if (mouse.getValue()) {
            float barW = cell * 1.5f + gapSize;
            drawBox(context, "LMB", x, bottomY, barW, cell * 0.8f, lmb.update(dt));
            drawBox(context, "RMB", x + barW + gapSize, bottomY, barW, cell * 0.8f, rmb.update(dt));
            bottomY += cell * 0.8f + gapSize;
        }

        if (space.getValue()) {
            float barW = cell * 3 + gapSize * 2;
            drawBox(context, "SPACE", x, bottomY, barW, cell * 0.7f, spaceAnim.update(dt));
        }
    }

    private void drawKey(DrawContext context, String label, float x, float y, float size, float press) {
        int back = ColorUtil.lerp(0x6A121218, ColorUtil.replaceAlpha(accent.getArgb(), 210), press);
        Render2D.roundedRect(context, x, y, size, size, 5f, back);
        Render2D.roundedBorder(context, x, y, size, size, 5f, 1f,
                ColorUtil.withAlpha(0xFFFFFF, 0.06f + 0.10f * press));
        Render2D.textCentered(context, label, x + size / 2f, y + size / 2f - 4f,
                ColorUtil.replaceAlpha(0xFFFFFF, (int) (150 + 105 * press)));
    }

    private void drawBox(DrawContext context, String label, float x, float y, float w, float h, float press) {
        int back = ColorUtil.lerp(0x6A121218, ColorUtil.replaceAlpha(accent.getArgb(), 190), press);
        Render2D.roundedRect(context, x, y, w, h, 4.5f, back);
        Render2D.roundedBorder(context, x, y, w, h, 4.5f, 1f,
                ColorUtil.withAlpha(0xFFFFFF, 0.06f + 0.10f * press));
        Render2D.textCentered(context, label, x + w / 2f, y + h / 2f - 4f,
                ColorUtil.replaceAlpha(0xFFFFFF, (int) (150 + 105 * press)));
    }
}
