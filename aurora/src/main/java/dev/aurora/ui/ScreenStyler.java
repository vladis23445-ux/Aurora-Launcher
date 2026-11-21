package dev.aurora.ui;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;

/**
 * Оформление ванильных экранов в стиле Aurora: фон-сияние и акцентная рамка
 * вокруг инвентаря, паузы и настроек. Ничего не ломает — только добавляет
 * прозрачные слои поверх ванильной отрисовки.
 */
public final class ScreenStyler {

    private static final int ACCENT = AuroraBackdrop.ACCENT;
    private static final int ACCENT2 = AuroraBackdrop.ACCENT2;

    private ScreenStyler() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ClickGuiScreen || screen instanceof AuroraMainMenu) {
                return;
            }
            if (!isStyled(screen)) {
                return;
            }
            ScreenEvents.afterBackground(screen).register(ScreenStyler::afterBackground);
            ScreenEvents.afterRender(screen).register(ScreenStyler::afterRender);
        });
    }

    private static boolean isStyled(Screen screen) {
        // весь ванильный интерфейс в нашем стиле, кроме собственных экранов мода
        return !(screen instanceof ClickGuiScreen)
                && !(screen instanceof AuroraMainMenu);
    }

    private static void afterBackground(Screen screen, DrawContext context,
                                        int mouseX, int mouseY, float delta) {
        if (screen instanceof HandledScreen<?>) {
            // инвентарь/сундуки/творческий режим: ванильную панель не перекрываем,
            // иначе пропадают вкладки категорий и слоты
            return;
        }

        // свой фон только для настоящих меню: на экранах подключения,
        // загрузки мира и перенастройки остаётся ванильный прогресс
        if (!isMenuScreen(screen)) {
            return;
        }
        // полностью перекрываем ванильный фон (земля/панорама) своим
        context.fill(0, 0, screen.width, screen.height, 0xF2050B09);
        AuroraBackdrop.render(context, screen.width, screen.height, 1f, 1f);
        context.fill(0, 0, screen.width, screen.height, 0x30040A08);
    }

    private static boolean isMenuScreen(Screen screen) {
        return screen instanceof OptionsScreen
                || screen instanceof GameMenuScreen
                || screen instanceof net.minecraft.client.gui.screen.world.SelectWorldScreen
                || screen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
                || screen instanceof net.minecraft.client.gui.screen.world.CreateWorldScreen
                || screen instanceof net.minecraft.client.gui.screen.option.ControlsOptionsScreen
                || screen instanceof net.minecraft.client.gui.screen.pack.PackScreen
                || screen instanceof net.minecraft.client.gui.screen.StatsScreen
                || screen instanceof net.minecraft.client.gui.screen.advancement.AdvancementsScreen
                || screen instanceof net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
    }

    private static void afterRender(Screen screen, DrawContext context,
                                    int mouseX, int mouseY, float delta) {
        if (screen instanceof InventoryScreen) {
            int w = 176;
            int h = 166;
            int x = (screen.width - w) / 2;
            int y = (screen.height - h) / 2;
            drawPanelFrame(context, x, y, w, h);
            return;
        }
        if (screen instanceof HandledScreen<?>) {
            drawEdges(context, screen.width, screen.height);
            return;
        }
        drawEdges(context, screen.width, screen.height);
    }

    private static void drawPanelFrame(DrawContext context, int x, int y, int w, int h) {
        Render2D.roundedBorder(context, x - 3f, y - 3f, w + 6f, h + 6f, 7f, 1.2f,
                ColorUtil.withAlpha(ACCENT, 0.55f));
        Render2D.gradientH(context, x - 3f, y - 7f, w + 6f, 1.4f,
                ColorUtil.withAlpha(ACCENT, 0.55f), ColorUtil.withAlpha(ACCENT2, 0.18f));

        drawCorner(context, x - 3f, y - 3f, 1, 1);
        drawCorner(context, x + w + 3f, y - 3f, -1, 1);
        drawCorner(context, x - 3f, y + h + 3f, 1, -1);
        drawCorner(context, x + w + 3f, y + h + 3f, -1, -1);
    }

    private static void drawCorner(DrawContext context, float x, float y, int dx, int dy) {
        float length = 10f;
        float thickness = 2f;
        context.fill(Math.round(dx > 0 ? x : x - length), Math.round(y),
                Math.round(dx > 0 ? x + length : x), Math.round(y + thickness),
                ColorUtil.withAlpha(ACCENT2, 0.75f));
        context.fill(Math.round(x), Math.round(dy > 0 ? y : y - length),
                Math.round(x + thickness), Math.round(dy > 0 ? y + length : y),
                ColorUtil.withAlpha(ACCENT2, 0.75f));
    }

    private static void drawEdges(DrawContext context, int width, int height) {
        Render2D.gradientH(context, 0, 2f, width, 1.5f,
                ColorUtil.withAlpha(ACCENT, 0.35f), ColorUtil.withAlpha(ACCENT2, 0.12f));
        Render2D.gradientH(context, 0, height - 3f, width, 1.5f,
                ColorUtil.withAlpha(ACCENT2, 0.10f), ColorUtil.withAlpha(ACCENT, 0.30f));
    }
}
