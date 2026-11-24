package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;

/** Coords: координаты, измерение и время в игре на HUD. */
public class Coords extends Module {

    private final BooleanSetting showBiome = register(new BooleanSetting("Biome", true));
    private final BooleanSetting showNether = register(new BooleanSetting("NetherCoords", true));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 6.0, 0.0, 600.0, 1));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 264.0, 0.0, 900.0, 1));

    public Coords(EventBus bus) {
        super(bus, "Coords", "Координаты и измерение на экране", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        int x = (int) Math.floor(mc.player.getX());
        int y = (int) Math.floor(mc.player.getY());
        int z = (int) Math.floor(mc.player.getZ());

        StringBuilder text = new StringBuilder();
        text.append(x).append("  ").append(y).append("  ").append(z).append("   ");
        text.append(dimension());

        if (showNether.getValue() && mc.world.getRegistryKey().getValue().getPath().equals("overworld")) {
            text.append("   ·   ад ").append(x / 8).append(" ").append(z / 8);
        }

        float pad = 7f;
        float width = Render2D.textWidth(text.toString()) + pad * 2f;
        float height = 15f;
        float px = offsetX.getFloat();
        float py = offsetY.getFloat();

        Render2D.roundedRect(context, px, py, width, height, 7.5f, 0x7015151A);
        Render2D.roundedBorder(context, px, py, width, height, 7.5f, 1f, ColorUtil.withAlpha(0xFFFFFF, 0.07f));
        Render2D.text(context, text.toString(), px + pad, py + 4f, ColorUtil.withAlpha(0xFFFFFFFF, 0.85f));

        if (showBiome.getValue()) {
            String biome = mc.world.getBiome(mc.player.getBlockPos()).getKey()
                    .map(key -> key.getValue().getPath().replace('_', ' '))
                    .orElse("?");
            Render2D.text(context, biome, px + pad, py + height + 3f, ColorUtil.withAlpha(0xFFFFFF, 0.35f));
        }
    }

    private String dimension() {
        String path = mc.world.getRegistryKey().getValue().getPath();
        switch (path) {
            case "overworld":
                return "верх";
            case "the_nether":
                return "ад";
            case "the_end":
                return "энд";
            default:
                return path;
        }
    }
}
