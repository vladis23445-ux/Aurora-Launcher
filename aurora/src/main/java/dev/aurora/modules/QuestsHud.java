package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.util.Profile;
import dev.aurora.util.Quests;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Quests: панель в игре — баланс монет и ближайшие задания с прогрессом.
 */
public class QuestsHud extends Module {

    private static final int ACCENT = 0xFF7C5CFF;
    private static final int ACCENT_2 = 0xFFFF5C9D;

    private final BooleanSetting showCoins = register(new BooleanSetting("Coins", true));
    private final NumberSetting rows = register(new NumberSetting("Rows", 4, 1, 8, 1));
    private final NumberSetting offsetX = register(new NumberSetting("OffsetX", 6.0, 0.0, 400.0, 1));
    private final NumberSetting offsetY = register(new NumberSetting("OffsetY", 32.0, 0.0, 600.0, 1));

    public QuestsHud(EventBus bus) {
        super(bus, "Quests", "Монеты и задания прямо в игре", Category.HUD);
        setEnabled(true);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        List<Quests.Quest> active = Quests.active(rows.getInt());
        int coins = Profile.coins();

        float width = 214f;
        float rowHeight = 25f;
        float titleHeight = 20f;
        float height = titleHeight + active.size() * rowHeight + 6f;
        if (active.isEmpty()) {
            height = titleHeight + 18f;
        }

        float x = offsetX.getFloat();
        float y = offsetY.getFloat();

        Render2D.roundedRect(context, x, y, width, height, 10f, 0x8C12141F);
        Render2D.roundedBorder(context, x, y, width, height, 10f, 1f, ColorUtil.withAlpha(0xFFFFFF, 0.10f));
        Render2D.roundedRect(context, x + 9f, y + 7f, 2.5f, 9f, 1.25f, ACCENT);

        Render2D.text(context, "ЗАДАНИЯ", x + 17f, y + 6f, ColorUtil.withAlpha(0xFFFFFF, 0.55f));
        if (showCoins.getValue()) {
            String money = coins + " ◈";
            float moneyWidth = Render2D.textWidth(money);
            Render2D.text(context, money, x + width - 10f - moneyWidth, y + 6f, 0xFFFFE08C);
        }

        float cursor = y + titleHeight;
        if (active.isEmpty()) {
            Render2D.text(context, "Все задания выполнены", x + 12f, cursor + 2f,
                    ColorUtil.withAlpha(0x3BE8A0, 0.85f));
            return;
        }

        for (Quests.Quest quest : active) {
            float frac = Quests.fraction(quest);

            Render2D.text(context, quest.title(), x + 12f, cursor + 2f,
                    ColorUtil.withAlpha(0xFFFFFF, 0.86f));

            String progress = Quests.formatProgress(quest);
            float progressWidth = Render2D.textWidth(progress);
            Render2D.text(context, progress, x + width - 10f - progressWidth, cursor + 2f,
                    ColorUtil.withAlpha(0xFFFFFF, 0.42f));

            float barX = x + 12f;
            float barY = cursor + 14f;
            float barWidth = width - 52f;
            Render2D.rect(context, barX, barY, barWidth, 3f, ColorUtil.withAlpha(0xFFFFFF, 0.10f));
            if (frac > 0.01f) {
                Render2D.gradientH(context, barX, barY, barWidth * frac, 3f, ACCENT, ACCENT_2);
            }

            cursor += rowHeight;
        }
    }
}
