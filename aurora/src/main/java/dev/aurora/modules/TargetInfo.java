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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

/**
 * TargetInfo: плашка с именем, здоровьем и бронёй сущности под прицелом.
 */
public class TargetInfo extends Module {

    private final ColorSetting accent = register(new ColorSetting("Accent", 0xFF3BE8A0));
    private final BooleanSetting showArmor = register(new BooleanSetting("Armor", true));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 0.72, 0.4, 0.95, 0.01));

    public TargetInfo(EventBus bus) {
        super(bus, "TargetInfo", "Инфо о сущности под прицелом", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }
        Entity entity = hit.getEntity();
        if (entity == null || entity == mc.player) {
            return;
        }

        String name = entity.getName().getString();
        float health = -1f;
        float maxHealth = -1f;
        int armor = 0;

        if (entity instanceof LivingEntity living) {
            health = living.getHealth();
            maxHealth = Math.max(1f, living.getMaxHealth());
            armor = living.getArmor();
        }

        int width = Math.max(120, Render2D.textWidth(name) + 26);
        int height = health >= 0f ? 46 : 28;
        int x = mc.getWindow().getScaledWidth() / 2 - width / 2;
        int y = Math.round(mc.getWindow().getScaledHeight() * offsetY.getFloat()) - height;

        int accentColor = accent.getArgb();

        Render2D.roundedRect(context, x, y, width, height, 8f, 0xD00A1411);
        Render2D.roundedBorder(context, x, y, width, height, 8f, 1f,
                ColorUtil.withAlpha(accentColor, 0.55f));

        Render2D.circle(context, x + 10f, y + 12f, 2.4f, accentColor);
        Render2D.text(context, name, x + 18f, y + 7f, 0xFFE9F9F0);

        if (health >= 0f) {
            String hp = String.format(java.util.Locale.ROOT, "%.1f / %.0f", health, maxHealth);
            Render2D.text(context, hp, x + width - 10f - Render2D.textWidth(hp), y + 7f,
                    ColorUtil.withAlpha(accentColor, 0.95f));

            float barWidth = width - 20f;
            float ratio = Math.max(0f, Math.min(1f, health / maxHealth));
            Render2D.roundedRect(context, x + 10f, y + 22f, barWidth, 4f, 2f, 0x66000000);
            if (ratio > 0.01f) {
                Render2D.roundedRect(context, x + 10f, y + 22f, barWidth * ratio, 4f, 2f, accentColor);
            }

            if (showArmor.getValue() && armor > 0) {
                String armorText = "ARM " + armor;
                Render2D.text(context, armorText, x + 10f, y + 32f,
                        ColorUtil.withAlpha(0xFFB6FF8C, 0.9f));
            }
        }
    }
}
