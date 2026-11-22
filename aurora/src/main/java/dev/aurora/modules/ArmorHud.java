package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * ArmorHUD: РІРµСЂС‚РёРєР°Р»СЊРЅС‹Р№ СЃРїРёСЃРѕРє Р±СЂРѕРЅРё СЃ РїРѕР»РѕСЃР°РјРё РїСЂРѕС‡РЅРѕСЃС‚Рё Рё РїСЂРѕС†РµРЅС‚Р°РјРё.
 */
public class ArmorHud extends Module {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final BooleanSetting durability = register(new BooleanSetting("Durability", true));
    private final BooleanSetting hand = register(new BooleanSetting("Hand", true));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 6.0, 0.0, 1200.0, 1));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 0.55, 0.0, 1.0, 0.01));

    public ArmorHud(EventBus bus) {
        super(bus, "ArmorHUD", "Прочность брони", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null) {
            return;
        }

        float x = offsetX.getFloat();
        float y = mc.getWindow().getScaledHeight() * offsetY.getFloat();
        float rowH = 18f;
        float panelW = 74f;

        float total = 0f;
        for (EquipmentSlot slot : SLOTS) {
            if (!mc.player.getEquippedStack(slot).isEmpty()) {
                total += rowH;
            }
        }
        ItemStack mainHand = mc.player.getMainHandStack();
        boolean handVisible = hand.getValue() && !mainHand.isEmpty() && mainHand.isDamageable();
        if (handVisible) {
            total += rowH;
        }
        if (total <= 0) {
            return;
        }

        Render2D.roundedRect(context, x, y, panelW, total + 8f, 9f, 0x7015151A);
        Render2D.roundedBorder(context, x, y, panelW, total + 8f, 9f, 1f,
                ColorUtil.withAlpha(0xFFFFFF, 0.06f));

        float cursor = y + 4f;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            drawRow(context, stack, x + 6f, cursor, panelW - 12f);
            cursor += rowH;
        }

        if (handVisible) {
            drawRow(context, mainHand, x + 6f, cursor, panelW - 12f);
        }
    }

    private void drawRow(DrawContext context, ItemStack stack, float x, float y, float width) {
        int max = Math.max(1, stack.getMaxDamage());
        int left = max - stack.getDamage();
        float ratio = Math.max(0f, Math.min(1f, left / (float) max));

        int color = ratio > 0.6f ? 0xFF4BE38A : ratio > 0.3f ? 0xFFFFC24A : 0xFFFF5A6E;

        // РёРєРѕРЅРєР°-СЃР»РѕС‚: РјРёРЅРёР°С‚СЋСЂР° РїСЂРµРґРјРµС‚Р° Р·Р°РјРµРЅРµРЅР° РЅР° С†РІРµС‚РЅРѕР№ РјР°СЂРєРµСЂ С‚РёРїР° СЃР»РѕС‚Р°
        Render2D.roundedRect(context, x, y, 11f, 11f, 3.5f, ColorUtil.withAlpha(color, 0.22f));
        Render2D.circle(context, x + 5.5f, y + 5.5f, 2.2f, color);

        Render2D.text(context, shortName(stack), x + 15f, y + 1.5f, 0xFFDDDDE6);

        if (durability.getValue()) {
            float barW = width - 15f;
            float barY = y + 12f;
            Render2D.roundedRect(context, x + 15f, barY, barW, 2.5f, 1.25f,
                    ColorUtil.replaceAlpha(0x000000, 110));
            Render2D.roundedRect(context, x + 15f, barY, barW * ratio, 2.5f, 1.25f, color);
        }
    }

    private String shortName(ItemStack stack) {
        String name = stack.getName().getString();
        if (name.length() > 12) {
            name = name.substring(0, 12) + "вЂ¦";
        }
        if (durability.getValue() && stack.isDamageable()) {
            int left = Math.max(0, stack.getMaxDamage() - stack.getDamage());
            return name + " " + left;
        }
        return name;
    }
}
