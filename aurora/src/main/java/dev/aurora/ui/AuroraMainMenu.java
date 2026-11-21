package dev.aurora.ui;

import dev.aurora.render.Animation;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Главное меню Aurora. Раскладка считается от реальной высоты экрана,
 * поэтому кнопки никогда не уезжают за границу и не наезжают на заголовок.
 */
public class AuroraMainMenu extends Screen {

    private static final int ACCENT = AuroraBackdrop.ACCENT;
    private static final int ACCENT2 = AuroraBackdrop.ACCENT2;
    private static final int MINT = AuroraBackdrop.MINT;
    private static final int TEXT = 0xFFE9F9F0;
    private static final int SUBTLE = 0xFF8AAAA0;

    private final Animation open = new Animation(0f, 10f);
    private final Animation[] hover = {
            new Animation(0f, 18f), new Animation(0f, 18f),
            new Animation(0f, 18f), new Animation(0f, 18f),
    };
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private int mouseX;
    private int mouseY;

    private record Hitbox(float x, float y, float w, float h, Runnable action) {
        boolean inside(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public AuroraMainMenu() {
        super(Text.literal("Aurora"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void init() {
        open.setTarget(1f);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // фон рисуем сами
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        hitboxes.clear();

        float progress = open.update(delta / 20f);
        AuroraBackdrop.render(context, this.width, this.height, progress, 1f);

        float centerX = this.width / 2f;
        float h = this.height;

        float markSize = Math.min(34f, h * 0.10f);
        float markY = Math.max(12f, h * 0.09f);
        float markX = centerX - markSize / 2f;

        Render2D.roundedGradient(context, markX, markY, markSize, markSize, markSize * 0.32f,
                ColorUtil.withAlpha(ACCENT, progress), ColorUtil.withAlpha(ACCENT2, progress));
        Render2D.circle(context, centerX, markY + markSize / 2f, markSize * 0.24f,
                ColorUtil.withAlpha(0xFFFFFF, 0.14f * progress));
        Render2D.roundedBorder(context, markX, markY, markSize, markSize, markSize * 0.32f, 1.2f,
                ColorUtil.withAlpha(0xFFFFFF, 0.20f * progress));

        float titleScale = 2f;
        float titleY = markY + markSize + 12f;
        float titleWidth = Render2D.textGradientWidth("AURORA") * titleScale;
        Render2D.scaledTextGradient(context, "AURORA", centerX - titleWidth / 2f, titleY,
                titleScale, ColorUtil.withAlpha(TEXT, progress),
                ColorUtil.withAlpha(ACCENT2, progress), true);

        float subtitleY = titleY + 9f * titleScale + 6f;
        Render2D.textCentered(context, "visual edition · minecraft 1.21.11", centerX, subtitleY,
                ColorUtil.withAlpha(SUBTLE, 0.75f * progress));

        // кнопки: считаем так, чтобы точно влезли
        float gap = 6f;
        float hintSpace = 26f;
        float available = h - subtitleY - 14f - hintSpace - 14f;
        float buttonH = Math.min(30f, Math.max(20f, (available - gap * 3f) / 4f));
        float buttonW = Math.min(300f, this.width - 60f);
        float total = buttonH * 4f + gap * 3f;
        float startY = Math.max(subtitleY + 14f, h - hintSpace - 10f - total);
        float x = centerX - buttonW / 2f;

        drawButton(context, 0, "ОДИНОЧНАЯ ИГРА", "solo world", x, startY, buttonW, buttonH, progress,
                () -> open(new SelectWorldScreen(this)));
        drawButton(context, 1, "СЕТЕВАЯ ИГРА", "multiplayer", x, startY + (buttonH + gap), buttonW, buttonH,
                progress, () -> open(new MultiplayerScreen(this)));
        drawButton(context, 2, "НАСТРОЙКИ", "options", x, startY + (buttonH + gap) * 2f, buttonW, buttonH,
                progress, () -> open(new OptionsScreen(this, this.client.options)));
        drawButton(context, 3, "ВЫХОД", "quit game", x, startY + (buttonH + gap) * 3f, buttonW, buttonH,
                progress, () -> this.client.scheduleStop());

        Render2D.text(context, "aurora visuals", 10f, h - 14f,
                ColorUtil.withAlpha(SUBTLE, 0.4f * progress));
        String hint = "RSHIFT — ClickGUI";
        Render2D.text(context, hint, this.width - 10f - Render2D.textWidth(hint), h - 14f,
                ColorUtil.withAlpha(SUBTLE, 0.4f * progress));
    }

    private void drawButton(DrawContext context, int index, String label, String hint,
                            float x, float y, float w, float h, float progress, Runnable action) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Animation animation = hover[index];
        animation.setTarget(hovered ? 1f : 0f);
        float amount = animation.update(1f / 60f);

        float alpha = progress * (0.85f + 0.15f * amount);

        Render2D.roundedRect(context, x, y, w, h, 8f, ColorUtil.withAlpha(0xFF0C1712, alpha));
        if (amount > 0.02f) {
            Render2D.roundedGradient(context, x, y, w, h, 8f,
                    ColorUtil.withAlpha(ACCENT, 0.20f * amount * progress),
                    ColorUtil.withAlpha(ACCENT2, 0.08f * amount * progress));
        }
        Render2D.roundedBorder(context, x, y, w, h, 8f, 1f,
                ColorUtil.withAlpha(hovered ? ACCENT : 0xFFFFFF,
                        (hovered ? 0.40f : 0.10f) * progress));

        Render2D.circle(context, x + 12f, y + h / 2f, 2.2f,
                ColorUtil.withAlpha(hovered ? MINT : SUBTLE, (0.5f + 0.5f * amount) * progress));

        boolean twoLines = h >= 26f;
        if (twoLines) {
            Render2D.text(context, label, x + 24f, y + 5f,
                    ColorUtil.withAlpha(TEXT, progress));
            Render2D.text(context, hint, x + 24f, y + 15f,
                    ColorUtil.withAlpha(SUBTLE, (0.5f + 0.3f * amount) * progress));
        } else {
            Render2D.text(context, label, x + 24f, y + h / 2f - 4f,
                    ColorUtil.withAlpha(TEXT, progress));
        }

        String arrow = "›";
        Render2D.text(context, arrow, x + w - 14f, y + h / 2f - 4f,
                ColorUtil.withAlpha(hovered ? ACCENT2 : SUBTLE, (0.5f + 0.5f * amount) * progress));

        hitboxes.add(new Hitbox(x, y, w, h, action));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (int i = hitboxes.size() - 1; i >= 0; i--) {
            Hitbox hitbox = hitboxes.get(i);
            if (hitbox.inside(click.x(), click.y())) {
                hitbox.action().run();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        // главное меню не закрывается
    }

    private void open(Screen screen) {
        if (this.client != null) {
            this.client.setScreen(screen);
        }
    }
}
