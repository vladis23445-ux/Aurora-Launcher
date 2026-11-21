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
import dev.aurora.util.CameraUtil;
import dev.aurora.util.SkinUtil;
import dev.aurora.util.TargetManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * TargetHUD: РїР»Р°С€РєР° СЃ РіРѕР»РѕРІРѕР№ РёРіСЂРѕРєР°, РЅРёРєРѕРј, Р·РґРѕСЂРѕРІСЊРµРј Рё Р±СЂРѕРЅС‘Р№.
 * РџРѕСЏРІР»СЏРµС‚СЃСЏ РїСЂРё Р°С‚Р°РєРµ С†РµР»Рё Рё РїР»Р°РІРЅРѕ РёСЃС‡РµР·Р°РµС‚ С‡РµСЂРµР· С‚Р°Р№РјР°СѓС‚ TargetManager.
 */
public class TargetHud extends Module {

    private final ColorSetting accent = register(new ColorSetting("Accent", 0xFF3BE8A0));
    private final ColorSetting healthColor = register(new ColorSetting("HealthColor", 0xFF4BE38A));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 0.5, 0.0, 1.0, 0.01));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 0.72, 0.0, 1.0, 0.01));
    private final BooleanSetting showArmor = register(new BooleanSetting("Armor", true));
    private final BooleanSetting showHead = register(new BooleanSetting("Head", true));

    private final Animation fade = new Animation(0f, 14f);
    private final Animation healthAnim = new Animation(1f, 10f);

    public TargetHud(EventBus bus) {
        super(bus, "TargetHUD", "Плашка цели: голова, ник, здоровье, броня", Category.HUD);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        LivingEntity target = TargetManager.getTarget();
        boolean visible = target != null && target.isAlive();

        fade.setTarget(visible ? 1f : 0f);
        float alpha = fade.update(1f / 60f);
        if (alpha <= 0.01f || !visible) {
            return;
        }

        float maxHealth = Math.max(1f, target.getMaxHealth());
        float healthRatio = Math.max(0f, Math.min(1f, target.getHealth() / maxHealth));
        healthAnim.setTarget(healthRatio);
        float animatedHealth = healthAnim.update(1f / 60f);

        float panelW = 168f;
        float panelH = 46f;
        float x = mc.getWindow().getScaledWidth() * offsetX.getFloat() - panelW / 2f;
        float y = mc.getWindow().getScaledHeight() * offsetY.getFloat();

        int back = ColorUtil.withAlpha(0xF01A1A20, alpha);
        int line = ColorUtil.withAlpha(accent.getArgb(), alpha * 0.85f);

        Render2D.roundedRect(context, x, y, panelW, panelH, 10f, back);
        Render2D.roundedBorder(context, x, y, panelW, panelH, 10f, 1f,
                ColorUtil.withAlpha(0xFFFFFF, alpha * 0.06f));

        float headSize = 28f;
        float headX = x + 8f;
        float headY = y + (panelH - headSize) / 2f;

        if (showHead.getValue()) {
            boolean drawn = drawHead(context, target, headX, headY, headSize, alpha);
            if (!drawn) {
                Render2D.roundedRect(context, headX, headY, headSize, headSize, 7f,
                        ColorUtil.withAlpha(line, alpha * 0.35f));
                Render2D.textCentered(context, SkinUtil.initials(target.getName().getString()),
                        headX + headSize / 2f, headY + headSize / 2f - 4f,
                        ColorUtil.replaceAlpha(0xFFFFFF, (int) (255 * alpha)));
            }
            Render2D.roundedBorder(context, headX, headY, headSize, headSize, 7f, 1f,
                    ColorUtil.withAlpha(0xFFFFFF, alpha * 0.12f));
        }

        float textX = headX + headSize + 9f;
        String name = target.getName().getString();
        Render2D.text(context, name, textX, y + 8f, ColorUtil.replaceAlpha(0xFFFFFF, (int) (235 * alpha)));

        String hp = String.format(java.util.Locale.ROOT, "%.1f", target.getHealth())
                + " / " + String.format(java.util.Locale.ROOT, "%.0f", maxHealth);
        Render2D.text(context, hp, x + panelW - 9f - Render2D.textWidth(hp), y + 8f,
                ColorUtil.withAlpha(healthColor.getArgb(), alpha));

        float barX = textX;
        float barY = y + 21f;
        float barW = panelW - (textX - x) - 9f;
        Render2D.roundedRect(context, barX, barY, barW, 4f, 2f,
                ColorUtil.replaceAlpha(0x000000, (int) (110 * alpha)));
        if (animatedHealth > 0.004f) {
            Render2D.roundedGradient(context, barX, barY, barW * animatedHealth, 4f, 2f,
                    ColorUtil.withAlpha(ColorUtil.lerp(healthColor.getArgb(), 0xFFFF5A6E, 1f - animatedHealth), alpha),
                    ColorUtil.withAlpha(healthColor.getArgb(), alpha));
        }

        if (showArmor.getValue()) {
            int armor = target instanceof PlayerEntity player ? armorValue(player) : target.getArmor();
            String armorText = "ARM " + armor;
            Render2D.text(context, armorText, textX, y + 29f,
                    ColorUtil.replaceAlpha(0xCCCCD6, (int) (190 * alpha)));

            if (target instanceof PlayerEntity player) {
                float itemX = textX + Render2D.textWidth(armorText) + 8f;
                for (net.minecraft.entity.EquipmentSlot slot : new net.minecraft.entity.EquipmentSlot[]{
                        net.minecraft.entity.EquipmentSlot.HEAD,
                        net.minecraft.entity.EquipmentSlot.CHEST,
                        net.minecraft.entity.EquipmentSlot.LEGS,
                        net.minecraft.entity.EquipmentSlot.FEET}) {
                    ItemStack stack = player.getEquippedStack(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    // РјР°СЂРєРµСЂ РїСЂРѕС‡РЅРѕСЃС‚Рё: 4 С‚РѕС‡РєРё-РёРЅРґРёРєР°С‚РѕСЂР° РїСЂРµРґРјРµС‚Р° Р±СЂРѕРЅРё
                    float dotY = y + 32f;
                    Render2D.circle(context, itemX + 2f, dotY, 2.2f,
                            ColorUtil.withAlpha(0xFFFFFF, alpha * 0.55f));
                    itemX += 8f;
                }
            }
        }

        // РёРЅРґРёРєР°С‚РѕСЂ, С‡С‚Рѕ С†РµР»СЊ РІ РїСЂРµРґРµР»Р°С… РІРёРґРёРјРѕСЃС‚Рё РєР°РјРµСЂС‹
        float[] screen = CameraUtil.project(
                new net.minecraft.util.math.Vec3d(target.getX(), target.getY() + target.getHeight() / 2f, target.getZ()),
                mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
        if (screen != null) {
            float dist = (float) Math.sqrt(target.squaredDistanceTo(mc.player));
            String distText = String.format(java.util.Locale.ROOT, "%.1fm", dist);
            Render2D.textCentered(context, distText, x + panelW / 2f, y - 10f,
                    ColorUtil.withAlpha(accent.getArgb(), alpha * 0.8f));
        }
    }

    private int armorValue(PlayerEntity player) {
        return player.getArmor();
    }

    /** РџСЂРѕР±СѓРµРј РѕС‚СЂРёСЃРѕРІР°С‚СЊ СЂРµР°Р»СЊРЅСѓСЋ РіРѕР»РѕРІСѓ СЃРєРёРЅР°; РµСЃР»Рё API РЅРµ РґР°Р» С‚РµРєСЃС‚СѓСЂСѓ вЂ” РІРµСЂРЅС‘Рј false. */
    private boolean drawHead(DrawContext context, LivingEntity target, float x, float y, float size, float alpha) {
        if (!(target instanceof PlayerEntity player) || mc.getNetworkHandler() == null) {
            return false;
        }
        try {
            Identifier texture = SkinUtil.headTexture(player);
            if (texture == null) {
                return false;
            }
            int px = Math.round(x);
            int py = Math.round(y);
            int s = Math.round(size);
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture,
                    px, py, 8f, 8f, s, s, 64, 64);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
