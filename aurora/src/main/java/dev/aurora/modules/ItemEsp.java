package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * ItemEsp: подсветка дропнутых предметов — заливка «шейдером» и обводка.
 */
public class ItemEsp extends Module {

    private final ColorSetting color = register(new ColorSetting("Color", 0xFF3BE8A0));
    private final BooleanSetting fill = register(new BooleanSetting("Fill", true));
    private final NumberSetting fillAlpha = register(new NumberSetting("FillAlpha", 22.0, 4.0, 90.0, 1.0));
    private final BooleanSetting outline = register(new BooleanSetting("Outline", true));
    private final NumberSetting width = register(new NumberSetting("Width", 1.2, 0.6, 4.0, 0.1));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));
    private final BooleanSetting pulse = register(new BooleanSetting("Pulse", true));
    private final NumberSetting range = register(new NumberSetting("Range", 16.0, 4.0, 64.0, 1.0));

    private long lastFrameNs;
    private float time;

    public ItemEsp(EventBus bus) {
        super(bus, "ItemEsp", "Подсветка предметов на земле: заливка и обводка", Category.RENDER);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.016f : Math.min(0.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        time += dt;

        double r = range.getValue();
        List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class,
                mc.player.getBoundingBox().expand(r), e -> e.isAlive() && !e.getStack().isEmpty());
        if (items.isEmpty()) {
            return;
        }

        int base = color.getArgb();
        if (pulse.getValue()) {
            float wave = 0.65f + 0.35f * (float) Math.sin(time * 2.6);
            base = ColorUtil.replaceAlpha(base, Math.round(ColorUtil.alphaOf(base) * wave));
        }

        for (ItemEntity item : items) {
            Box box = item.getBoundingBox().expand(0.02);
            if (fill.getValue()) {
                int fillColor = ColorUtil.replaceAlpha(base,
                        Math.round(ColorUtil.alphaOf(base) * fillAlpha.getFloat() / 100f * 2.55f));
                ScreenProjection.fillBox(context, box, fillColor);
            }
            if (glow.getValue()) {
                ScreenProjection.box(context, box, 0.006f, width.getFloat() * 3.0f,
                        ColorUtil.withAlpha(base, 0.10f), ColorUtil.withAlpha(base, 0.10f));
            }
            if (outline.getValue()) {
                ScreenProjection.box(context, box, 0.006f, width.getFloat(), base,
                        ColorUtil.withAlpha(base, 0.8f));
            }
        }
    }
}
