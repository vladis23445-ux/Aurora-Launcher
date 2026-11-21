package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;

/**
 * Watermark: Р С”Р С•Р СР С—Р В°Р С”РЎвЂљР Р…Р В°РЎРЏ Р С—Р В»Р В°РЎв‚¬Р С”Р В° РЎРѓ Р Р…Р В°Р В·Р Р†Р В°Р Р…Р С‘Р ВµР С Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР В°, FPS Р С‘ Р С—Р С‘Р Р…Р С–Р С•Р С.
 */
public class Watermark extends Module {

    private final ColorSetting first = register(new ColorSetting("ColorA", 0xFF3BE8A0));
    private final ColorSetting second = register(new ColorSetting("ColorB", 0xFF40D6D6));
    private final BooleanSetting showFps = register(new BooleanSetting("FPS", true));
    private final BooleanSetting showPing = register(new BooleanSetting("Ping", true));
    private final BooleanSetting showBuild = register(new BooleanSetting("Version", true));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 6.0, 0.0, 400.0, 1));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 6.0, 0.0, 400.0, 1));

    public Watermark(EventBus bus) {
        super(bus, "Watermark", "Плашка клиента с FPS и пингом", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        String name = "AURORA";
        String version = "v1.0";
        String fps = mc.getCurrentFps() + " fps";

        StringBuilder info = new StringBuilder();
        if (showFps.getValue()) {
            info.append(fps);
        }
        if (showPing.getValue()) {
            if (info.length() > 0) {
                info.append("  ");
            }
            info.append(ping()).append(" ms");
        }

        float nameWidth = Render2D.textGradientWidth(name);

        float padX = 9f;
        float gap = 10f;
        float height = 17f;
        float infoWidth = Render2D.textWidth(info.toString());
        float versionWidth = showBuild.getValue() ? Render2D.textWidth(version) + 7f : 0f;
        float width = padX * 2 + nameWidth + (info.length() > 0 ? gap + infoWidth : 0f) + versionWidth;

        float x = offsetX.getFloat();
        float y = offsetY.getFloat();

        Render2D.roundedRect(context, x, y, width, height, 8.5f, 0xE615151A);
        Render2D.roundedBorder(context, x, y, width, height, 8.5f, 1f, ColorUtil.withAlpha(0xFFFFFF, 0.07f));
        Render2D.roundedRect(context, x + 4.5f, y + 4.5f, 2.5f, height - 9f, 1.25f, first.getArgb());

        // Р Р…Р В°Р В·Р Р†Р В°Р Р…Р С‘Р Вµ Р С—Р С•РЎРѓР С‘Р СР Р†Р С•Р В»РЎРЉР Р…Р С• РЎРѓ Р С–РЎР‚Р В°Р Т‘Р С‘Р ВµР Р…РЎвЂљР С•Р С
        float cursor = x + padX;
        Render2D.textGradient(context, name, cursor, y + 5f, first.getArgb(), second.getArgb(), false);
        cursor += nameWidth;

        if (showBuild.getValue()) {
            Render2D.text(context, version, cursor + 4f, y + 6f, ColorUtil.withAlpha(0xFFFFFF, 0.35f));
            cursor += versionWidth;
        }

        if (info.length() > 0) {
            Render2D.text(context, info.toString(), cursor + gap - 2f, y + 5f,
                    ColorUtil.withAlpha(0xFFFFFF, 0.72f));
        }
    }

    private int ping() {
        try {
            if (mc.player != null && mc.getNetworkHandler() != null) {
                net.minecraft.client.network.PlayerListEntry entry =
                        mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) {
                    return entry.getLatency();
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
