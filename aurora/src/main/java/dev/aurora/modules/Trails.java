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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trails: СЃРІРµС‚СЏС‰РёР№СЃСЏ С€Р»РµР№С„ Р·Р° РёРіСЂРѕРєРѕРј. РўРѕС‡РєРё РїРёС€СѓС‚СЃСЏ СЂР°Р· РІ С‚РёРє,
 * СЂРµРЅРґРµСЂ вЂ” РїСЂРѕРµРєС†РёСЏ РЅР° СЌРєСЂР°РЅ СЃ РіСЂР°РґРёРµРЅС‚РѕРј Рё Р·Р°С‚СѓС…Р°РЅРёРµРј Рє С…РІРѕСЃС‚Сѓ.
 */
public class Trails extends Module {

    private final ColorSetting start = register(new ColorSetting("StartColor", 0xFF3BE8A0));
    private final ColorSetting end = register(new ColorSetting("EndColor", 0xFF40D6D6));
    private final NumberSetting length = register(new NumberSetting("Length", 26, 8, 120, 1));
    private final NumberSetting width = register(new NumberSetting("Width", 2.0, 0.8, 6.0, 0.1));
    private final NumberSetting height = register(new NumberSetting("Height", 0.95, 0.1, 2.0, 0.05));
    private final BooleanSetting self = register(new BooleanSetting("Self", true));
    private final BooleanSetting others = register(new BooleanSetting("Others", true));
    private final BooleanSetting onlyMoving = register(new BooleanSetting("OnlyMoving", true));

    private final Map<UUID, Deque<Vec3d>> trails = new HashMap<>();

    public Trails(EventBus bus) {
        super(bus, "Trails", "Светящийся шлейф с градиентом", Category.RENDER);
    }

    @Override
    public void onDisable() {
        trails.clear();
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            trails.clear();
            return;
        }

        int max = length.getInt();
        for (PlayerEntity player : mc.world.getPlayers()) {
            boolean isSelf = player == mc.player;
            if (isSelf ? !self.getValue() : !others.getValue()) {
                continue;
            }

            Vec3d point = new Vec3d(player.getX(), player.getY() + height.getValue(), player.getZ());
            Deque<Vec3d> trail = trails.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());

            Vec3d last = trail.peekLast();
            if (onlyMoving.getValue() && last != null && last.squaredDistanceTo(point) < 0.0009) {
                continue;
            }

            trail.addLast(point);
            while (trail.size() > max) {
                trail.removeFirst();
            }
        }

        trails.keySet().removeIf(uuid ->
                mc.world.getPlayerByUuid(uuid) == null && !uuid.equals(mc.player.getUuid()));
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (trails.isEmpty()) {
            return;
        }

        int colorStart = start.getArgb();
        int colorEnd = ColorUtil.withAlpha(end.getArgb(), 0f);
        float baseWidth = width.getFloat();

        for (Deque<Vec3d> trail : trails.values()) {
            if (trail.size() < 2) {
                continue;
            }
            int count = trail.size();
            int index = 0;

            float[] previous = null;
            int previousColor = 0;
            float previousWidth = 0f;

            for (Vec3d point : trail) {
                float t = index / (float) (count - 1);
                float[] screen = ScreenProjection.project(point);
                index++;
                int color = ColorUtil.lerp(colorEnd, colorStart, t);
                float lineWidth = baseWidth * (0.25f + 0.75f * t);

                if (screen != null && previous != null) {
                    ScreenProjection.gradientLine(context, previous[0], previous[1],
                            screen[0], screen[1], (lineWidth + previousWidth) / 2f,
                            previousColor, color);
                }
                previous = screen;
                previousColor = color;
                previousWidth = lineWidth;
            }
        }
    }
}
