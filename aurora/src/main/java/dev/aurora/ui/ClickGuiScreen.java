package dev.aurora.ui;

import dev.aurora.Aurora;
import dev.aurora.config.ConfigManager;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.EnumSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.module.setting.Setting;
import dev.aurora.render.Animation;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Компактное меню Aurora: две страницы — список модулей и настройки выбранного.
 * Такая схема не ломается на любом GUI scale: всё рисуется в дизайн-пространстве
 * 340×260 и масштабируется целиком.
 */
public class ClickGuiScreen extends Screen {

    private static final float W = 340f;
    private static final float H = 260f;
    private static final float HEADER = 34f;
    private static final float TABS = 20f;
    private static final float PAD = 8f;
    private static final float ROW = 22f;

    private static final int PANEL = 0xD00B1512;
    private static final int CARD = 0x8C0A1411;
    private static final int STROKE = 0x229CFFD6;
    private static final int TEXT = 0xFFE9F9F0;
    private static final int SUBTLE = 0xFF8AAAA0;
    private static final int ACCENT = 0xFF3BE8A0;
    private static final int ACCENT2 = 0xFF40D6D6;
    private static final int MINT = 0xFF8CFFC8;

    private final Animation open = new Animation(0f, 9f);
    private final Map<Module, Animation> switches = new IdentityHashMap<>();
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private final Set<Setting<?>> expanded = Collections.newSetFromMap(new IdentityHashMap<>());

    private int tab;
    private boolean settingsPage;
    private Module selected;
    private Module binding;

    private boolean configMode;
    private List<String> configs = List.of();
    private List<Module> frameModules = List.of();

    private float contentHeight;
    private float scroll;
    private float settingsScroll;

    private float uiScale = 1f;
    private float originX;
    private float originY;

    private int mouseX;
    private int mouseY;
    private int currentButton;

    private String search = "";
    private boolean searchFocused;

    private Drag drag;

    private record Hitbox(float x, float y, float w, float h, Runnable action) {
        boolean inside(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static final class Drag {
        final float x;
        final float w;
        final java.util.function.Consumer<Float> consumer;

        Drag(float x, float w, java.util.function.Consumer<Float> consumer) {
            this.x = x;
            this.w = w;
            this.consumer = consumer;
        }
    }

    public ClickGuiScreen() {
        super(Text.literal("Aurora"));
    }

    @Override
    protected void init() {
        open.setTarget(1f);
        refreshConfigs();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // фон прозрачный
    }

    // ------------------------------------------------------------------ кадр

    @Override
    public void render(DrawContext context, int rawMouseX, int rawMouseY, float delta) {
        hitboxes.clear();

        float progress = open.update(delta / 20f);
        float eased = Animation.easeOutCubic(progress);

        uiScale = Math.min(1f, Math.min((this.width - 12f) / W, (this.height - 12f) / H));
        originX = this.width / 2f - W * uiScale / 2f;
        originY = this.height / 2f - H * uiScale / 2f;

        mouseX = Math.round(toDesignX(rawMouseX));
        mouseY = Math.round(toDesignY(rawMouseY));

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(originX, originY);
        matrices.scale(uiScale, uiScale);

        float y = (1f - eased) * 8f;

        Render2D.roundedRect(context, 0, y, W, H, 14f, ColorUtil.withAlpha(PANEL, eased));
        Render2D.roundedBorder(context, 0, y, W, H, 14f, 1f, ColorUtil.withAlpha(STROKE, eased));

        drawHeader(context, y, eased);

        if (settingsPage && selected != null) {
            drawSettingsPage(context, y, eased);
        } else if (configMode) {
            drawConfigPage(context, y, eased);
        } else {
            drawList(context, y, eased);
        }

        if (binding != null) {
            Render2D.roundedRect(context, W / 2f - 80f, y + H - 40f, 160f, 22f, 7f,
                    ColorUtil.withAlpha(0x0A0F0C, 0.95f));
            Render2D.textCentered(context, "нажми клавишу", W / 2f, y + H - 34f, TEXT);
        }

        matrices.popMatrix();
    }

    private void drawHeader(DrawContext context, float y, float eased) {
        Render2D.roundedGradient(context, PAD, y + 8f, 16f, 16f, 5f,
                ColorUtil.withAlpha(ACCENT, eased), ColorUtil.withAlpha(ACCENT2, eased));
        Render2D.text(context, "AURORA", PAD + 22f, y + 10f, ColorUtil.withAlpha(TEXT, eased));

        String cfg = Aurora.get().getConfig().current();
        Render2D.text(context, cfg, W - PAD - Render2D.textWidth(cfg), y + 10f,
                ColorUtil.withAlpha(SUBTLE, 0.7f * eased));

        // вкладки
        String[] labels = {"Combat", "Render", "HUD", "Misc", "Cfg"};
        float gap = 4f;
        float tabW = (W - PAD * 2f - gap * (labels.length - 1)) / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float tx = PAD + i * (tabW + gap);
            float ty = y + HEADER - 6f;
            boolean active = (i < 4 && !configMode && tab == i)
                    || (i == 4 && configMode);
            boolean hovered = inside(mouseX, mouseY, tx, ty, tabW, TABS - 4f);

            Render2D.roundedRect(context, tx, ty, tabW, TABS - 4f, 5f,
                    ColorUtil.withAlpha(active ? ACCENT : 0xFFFFFF, (active ? 0.18f : hovered ? 0.07f : 0.03f) * eased));
            if (active) {
                Render2D.roundedBorder(context, tx, ty, tabW, TABS - 4f, 5f, 1f,
                        ColorUtil.withAlpha(ACCENT, 0.45f * eased));
            }
            Render2D.textCentered(context, labels[i], tx + tabW / 2f, ty + 4f,
                    ColorUtil.withAlpha(active ? TEXT : SUBTLE, eased));

            int index = i;
            hitboxes.add(new Hitbox(tx, ty, tabW, TABS - 4f, () -> {
                settingsPage = false;
                binding = null;
                scroll = 0f;
                settingsScroll = 0f;
                if (index == 4) {
                    configMode = true;
                    refreshConfigs();
                } else {
                    configMode = false;
                    tab = index;
                }
            }));
        }
    }

    private float contentTop(float y) {
        return y + HEADER + TABS;
    }

    private void drawList(DrawContext context, float y, float eased) {
        Category category = Category.values()[Math.min(tab, Category.values().length - 1)];
        frameModules = search.isEmpty()
                ? Aurora.get().getModules().byCategory(category)
                : searchModules(search);

        float top = contentTop(y);
        float bottom = y + H - PAD;
        float visible = bottom - top;

        contentHeight = frameModules.size() * ROW;
        scroll = Math.max(0f, Math.min(scroll, Math.max(0f, contentHeight - visible)));

        for (int i = 0; i < frameModules.size(); i++) {
            Module module = frameModules.get(i);
            float rowY = top + i * ROW - scroll;
            if (rowY + ROW < top || rowY > bottom) {
                continue;
            }

            boolean hovered = inside(mouseX, mouseY, PAD, rowY, W - PAD * 2f, ROW - 2f);
            Animation animation = switches.computeIfAbsent(module,
                    m -> new Animation(m.isEnabled() ? 1f : 0f, 12f));
            animation.setTarget(module.isEnabled() ? 1f : 0f);
            float on = animation.update(1f / 60f);

            if (hovered) {
                Render2D.roundedRect(context, PAD, rowY, W - PAD * 2f, ROW - 2f, 6f,
                        ColorUtil.withAlpha(0xFFFFFF, 0.05f * eased));
            }

            Render2D.circle(context, PAD + 7f, rowY + (ROW - 2f) / 2f, 2.2f,
                    ColorUtil.withAlpha(on > 0.5f ? ACCENT : SUBTLE, (0.4f + 0.6f * on) * eased));
            Render2D.text(context, module.getName(), PAD + 15f, rowY + 6f,
                    ColorUtil.withAlpha(on > 0.5f ? TEXT : SUBTLE, eased));

            float toggleW = 18f;
            float toggleX = W - PAD - 22f - toggleW;
            float toggleY = rowY + 5f;
            Render2D.roundedRect(context, toggleX, toggleY, toggleW, 10f, 5f,
                    ColorUtil.withAlpha(0xFFFFFF, 0.08f * eased));
            if (on > 0.02f) {
                Render2D.roundedGradient(context, toggleX, toggleY, toggleW, 10f, 5f,
                        ColorUtil.withAlpha(ACCENT, on * eased), ColorUtil.withAlpha(ACCENT2, on * eased));
            }
            Render2D.circle(context, toggleX + 2f + on * (toggleW - 10f) + 4f, toggleY + 5f, 3.4f,
                    ColorUtil.withAlpha(0xFFFFFF, 0.9f * eased));

            Render2D.text(context, "›", W - PAD - 12f, rowY + 5f,
                    ColorUtil.withAlpha(hovered ? ACCENT2 : SUBTLE, 0.8f * eased));

            hitboxes.add(new Hitbox(PAD, rowY, W - PAD * 2f - 24f, ROW - 2f, () -> {
                if (currentButton == 1) {
                    openSettings(module);
                } else {
                    module.toggle();
                    Aurora.get().saveConfig();
                }
            }));
            hitboxes.add(new Hitbox(W - PAD - 22f, rowY, 22f, ROW - 2f, () -> openSettings(module)));
        }

        drawScrollbar(context, W - PAD + 2f, top, visible, contentHeight, eased);
    }

    private void drawSettingsPage(DrawContext context, float y, float eased) {
        float top = contentTop(y);
        float bottom = y + H - PAD;

        // назад
        Render2D.text(context, "‹", PAD, top + 4f, ColorUtil.withAlpha(ACCENT, eased));
        hitboxes.add(new Hitbox(PAD - 2f, top, 16f, 16f, () -> {
            settingsPage = false;
            settingsScroll = 0f;
        }));
        Render2D.text(context, selected.getName(), PAD + 14f, top + 4f, ColorUtil.withAlpha(TEXT, eased));

        float listTop = top + 20f;
        float listBottom = bottom - 18f;
        float visible = Math.max(1f, listBottom - listTop);
        float total = settingsHeight(selected);

        settingsScroll = Math.max(0f, Math.min(settingsScroll, Math.max(0f, total - visible)));

        float cursor = listTop - settingsScroll;
        for (Setting<?> setting : selected.getSettings()) {
            if (!setting.isVisible()) {
                continue;
            }
            float height = settingHeight(setting);
            boolean onScreen = cursor + height > listTop && cursor < listBottom;
            if (onScreen) {
                if (setting instanceof BooleanSetting bool) {
                    cursor = drawBoolean(context, bool, PAD, cursor, W - PAD * 2f, eased);
                } else if (setting instanceof NumberSetting number) {
                    cursor = drawNumber(context, number, PAD, cursor, W - PAD * 2f, eased);
                } else if (setting instanceof EnumSetting<?> enumeration) {
                    cursor = drawEnum(context, enumeration, PAD, cursor, W - PAD * 2f, eased);
                } else if (setting instanceof ColorSetting color) {
                    cursor = drawColor(context, color, PAD, cursor, W - PAD * 2f, eased);
                } else {
                    cursor += height;
                }
            } else {
                cursor += height;
            }
        }

        if (total > visible) {
            drawScrollbar(context, W - PAD + 2f, listTop, visible, total, eased);
        }

        String keyLabel = keyName(selected);
        if (keyLabel.isEmpty()) {
            keyLabel = "bind: нет";
        } else {
            keyLabel = "bind: " + keyLabel;
        }
        Render2D.text(context, keyLabel, PAD, bottom - 10f, ColorUtil.withAlpha(SUBTLE, 0.8f * eased));
        hitboxes.add(new Hitbox(PAD, bottom - 14f, 110f, 14f, () -> binding = selected));
    }

    private void drawConfigPage(DrawContext context, float y, float eased) {
        float top = contentTop(y);
        float bottom = y + H - PAD;

        for (int i = 0; i < configs.size(); i++) {
            String name = configs.get(i);
            float rowY = top + i * ROW;
            if (rowY > bottom - ROW) {
                break;
            }
            boolean current = name.equalsIgnoreCase(Aurora.get().getConfig().current());
            boolean hovered = inside(mouseX, mouseY, PAD, rowY, W - PAD * 2f, ROW - 2f);

            if (current || hovered) {
                Render2D.roundedRect(context, PAD, rowY, W - PAD * 2f, ROW - 2f, 6f,
                        ColorUtil.withAlpha(current ? ACCENT : 0xFFFFFF, (current ? 0.14f : 0.05f) * eased));
            }
            Render2D.text(context, name, PAD + 8f, rowY + 6f,
                    ColorUtil.withAlpha(current ? TEXT : SUBTLE, eased));

            hitboxes.add(new Hitbox(PAD, rowY, W - PAD * 2f - 20f, ROW - 2f, () -> {
                Aurora.get().getConfig().load(name);
                refreshConfigs();
            }));
            hitboxes.add(new Hitbox(W - PAD - 18f, rowY, 16f, ROW - 2f, () -> {
                Aurora.get().getConfig().delete(name);
                refreshConfigs();
            }));
        }

        float btnY = bottom - 22f;
        float btnW = (W - PAD * 2f - 8f) / 2f;
        Render2D.roundedRect(context, PAD, btnY, btnW, 18f, 5f, ColorUtil.withAlpha(0xFFFFFF, 0.06f));
        Render2D.textCentered(context, "＋ Новый", PAD + btnW / 2f, btnY + 5f, ColorUtil.withAlpha(ACCENT, eased));
        hitboxes.add(new Hitbox(PAD, btnY, btnW, 18f, () -> {
            Aurora.get().getConfig().createNew();
            refreshConfigs();
        }));

        Render2D.roundedRect(context, PAD + btnW + 8f, btnY, btnW, 18f, 5f, ColorUtil.withAlpha(0xFFFFFF, 0.06f));
        Render2D.textCentered(context, "Сохранить", PAD + btnW + 8f + btnW / 2f, btnY + 5f,
                ColorUtil.withAlpha(ACCENT, eased));
        hitboxes.add(new Hitbox(PAD + btnW + 8f, btnY, btnW, 18f, () -> {
            Aurora.get().getConfig().save();
            refreshConfigs();
        }));
    }

    private void drawScrollbar(DrawContext context, float x, float top, float visible,
                               float content, float eased) {
        if (content <= visible) {
            return;
        }
        float offset = settingsPage ? settingsScroll : scroll;
        float trackH = visible;
        float thumbH = Math.max(14f, trackH * visible / content);
        float thumbY = top + (trackH - thumbH) * (offset / Math.max(1f, content - visible));
        Render2D.roundedRect(context, x, top, 1.5f, trackH, 0.75f, ColorUtil.withAlpha(0xFFFFFF, 0.05f * eased));
        Render2D.roundedRect(context, x, thumbY, 1.5f, thumbH, 0.75f, ColorUtil.withAlpha(ACCENT, 0.5f * eased));
    }

    // -------------------------------------------------------------- настройки

    private float drawBoolean(DrawContext context, BooleanSetting setting,
                              float x, float y, float w, float eased) {
        Render2D.text(context, setting.getName(), x + 6f, y + 2f, ColorUtil.withAlpha(TEXT, 0.9f * eased));
        float toggleW = 18f;
        float tx = x + w - toggleW - 6f;
        float ty = y + 1f;
        boolean on = setting.getValue();
        Render2D.roundedRect(context, tx, ty, toggleW, 10f, 5f, ColorUtil.withAlpha(0xFFFFFF, 0.08f * eased));
        if (on) {
            Render2D.roundedGradient(context, tx, ty, toggleW, 10f, 5f,
                    ColorUtil.withAlpha(ACCENT, 0.9f * eased), ColorUtil.withAlpha(ACCENT2, 0.9f * eased));
        }
        Render2D.circle(context, tx + 2f + (on ? toggleW - 10f : 0f) + 4f, ty + 5f, 3.4f,
                ColorUtil.withAlpha(0xFFFFFF, 0.9f * eased));
        hitboxes.add(new Hitbox(x, y - 2f, w, 16f, () -> {
            setting.toggle();
            Aurora.get().saveConfig();
        }));
        return y + 18f;
    }

    private float drawNumber(DrawContext context, NumberSetting setting,
                             float x, float y, float w, float eased) {
        Render2D.text(context, setting.getName(), x + 6f, y, ColorUtil.withAlpha(TEXT, 0.9f * eased));
        String value = setting.getDisplay();
        Render2D.text(context, value, x + w - 6f - Render2D.textWidth(value), y,
                ColorUtil.withAlpha(ACCENT2, 0.95f * eased));

        float trackY = y + 12f;
        float trackW = w - 12f;
        float trackX = x + 6f;
        Render2D.roundedRect(context, trackX, trackY, trackW, 3f, 1.5f, ColorUtil.withAlpha(0xFFFFFF, 0.08f * eased));
        float fill = trackW * setting.getProgress();
        Render2D.roundedGradient(context, trackX, trackY, Math.max(1.5f, fill), 3f, 1.5f,
                ColorUtil.withAlpha(ACCENT, 0.95f * eased), ColorUtil.withAlpha(ACCENT2, 0.95f * eased));
        Render2D.circle(context, trackX + fill, trackY + 1.5f, 3f, 0xFFEAF7F0);

        hitboxes.add(new Hitbox(trackX, y - 2f, trackW, 18f,
                () -> drag = new Drag(trackX, trackW, setting::setProgress)));
        return y + 22f;
    }

    private float drawEnum(DrawContext context, EnumSetting<?> setting,
                           float x, float y, float w, float eased) {
        Render2D.text(context, setting.getName(), x + 6f, y + 1f, ColorUtil.withAlpha(TEXT, 0.9f * eased));
        String value = setting.getDisplay();
        float bw = Render2D.textWidth(value) + 12f;
        float bx = x + w - bw - 6f;
        Render2D.roundedRect(context, bx, y - 1f, bw, 13f, 6f, ColorUtil.withAlpha(0xFFFFFF, 0.06f * eased));
        Render2D.textCentered(context, value, bx + bw / 2f, y + 2f, ColorUtil.withAlpha(ACCENT2, 0.95f * eased));
        hitboxes.add(new Hitbox(x, y - 2f, w, 16f, () -> {
            setting.cycle();
            Aurora.get().saveConfig();
        }));
        return y + 18f;
    }

    private float drawColor(DrawContext context, ColorSetting setting,
                            float x, float y, float w, float eased) {
        boolean open = expanded.contains(setting);
        Render2D.text(context, setting.getName(), x + 6f, y + 2f, ColorUtil.withAlpha(TEXT, 0.9f * eased));

        float sw = 30f;
        float sx = x + w - sw - 6f;
        Render2D.roundedRect(context, sx, y - 1f, sw, 11f, 5f, setting.getArgb());
        Render2D.roundedBorder(context, sx, y - 1f, sw, 11f, 5f, 1f, ColorUtil.withAlpha(0xFFFFFF, 0.18f));
        hitboxes.add(new Hitbox(sx, y - 1f, sw, 11f, () -> {
            if (!expanded.remove(setting)) {
                expanded.add(setting);
            }
        }));

        if (!open) {
            return y + 18f;
        }

        float barW = w - 12f;
        float row = y + 14f;
        for (int i = 0; i < 30; i++) {
            int hue = ColorUtil.hsb(i / 29f, 0.85f, 1f, 255);
            context.fill(Math.round(x + 6f + barW * i / 30f), Math.round(row),
                    Math.round(x + 6f + barW * (i + 1) / 30f) + 1, Math.round(row + 5f),
                    ColorUtil.withAlpha(hue, eased));
        }
        Render2D.circle(context, x + 6f + setting.getHue() * barW, row + 2.5f, 2.8f, 0xFFF2FFF8);
        float hueBarX = x + 6f;
        hitboxes.add(new Hitbox(hueBarX, row - 2f, barW, 10f, () -> drag = new Drag(hueBarX, barW,
                f -> setting.setHsb(f, setting.getSaturation(), setting.getBrightness()))));

        float satY = row + 9f;
        Render2D.gradientH(context, x + 6f, satY, barW, 5f,
                ColorUtil.hsb(setting.getHue(), 0f, setting.getBrightness(), 255),
                ColorUtil.hsb(setting.getHue(), 1f, setting.getBrightness(), 255));
        Render2D.circle(context, x + 6f + setting.getSaturation() * barW, satY + 2.5f, 2.8f, 0xFFF2FFF8);
        float satX = x + 6f;
        hitboxes.add(new Hitbox(satX, satY - 2f, barW, 10f, () -> drag = new Drag(satX, barW,
                f -> setting.setHsb(setting.getHue(), f, setting.getBrightness()))));

        float alphaY = satY + 9f;
        Render2D.gradientH(context, x + 6f, alphaY, barW, 5f,
                ColorUtil.withAlpha(setting.getRgb(), 0.05f),
                ColorUtil.withAlpha(setting.getRgb(), 1f));
        Render2D.circle(context, x + 6f + ColorUtil.alphaOf(setting.getArgb()) / 255f * barW, alphaY + 2.5f,
                2.8f, 0xFFF2FFF8);
        float alphaX = x + 6f;
        hitboxes.add(new Hitbox(alphaX, alphaY - 2f, barW, 10f, () -> drag = new Drag(alphaX, barW,
                setting::setAlpha)));

        return alphaY + 10f;
    }

    private float settingHeight(Setting<?> setting) {
        if (setting instanceof BooleanSetting) {
            return 18f;
        }
        if (setting instanceof NumberSetting) {
            return 22f;
        }
        if (setting instanceof EnumSetting<?>) {
            return 18f;
        }
        if (setting instanceof ColorSetting color) {
            return expanded.contains(color) ? 44f : 18f;
        }
        return 18f;
    }

    private float settingsHeight(Module module) {
        float total = 0f;
        for (Setting<?> setting : module.getSettings()) {
            if (setting.isVisible()) {
                total += settingHeight(setting);
            }
        }
        return total;
    }

    // ------------------------------------------------------------ вспомогательное

    private void openSettings(Module module) {
        selected = module;
        settingsPage = true;
        configMode = false;
        settingsScroll = 0f;
    }

    private void refreshConfigs() {
        ConfigManager manager = Aurora.get().getConfig();
        configs = manager.list();
    }

    private List<Module> searchModules(String query) {
        String lower = query.toLowerCase(java.util.Locale.ROOT);
        List<Module> result = new ArrayList<>();
        for (Module module : Aurora.get().getModules().getModules()) {
            String description = module.getDescription() == null ? "" : module.getDescription();
            if (module.getName().toLowerCase(java.util.Locale.ROOT).contains(lower)
                    || description.toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                result.add(module);
            }
        }
        return result;
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float toDesignX(double x) {
        return (float) ((x - originX) / Math.max(0.0001f, uiScale));
    }

    private float toDesignY(double y) {
        return (float) ((y - originY) / Math.max(0.0001f, uiScale));
    }

    // ------------------------------------------------------------- ввод

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = toDesignX(click.x());
        double my = toDesignY(click.y());
        currentButton = click.button();

        if (binding != null) {
            binding = null;
            return true;
        }

        for (int i = hitboxes.size() - 1; i >= 0; i--) {
            Hitbox hitbox = hitboxes.get(i);
            if (hitbox.inside(mx, my)) {
                hitbox.action().run();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (drag != null) {
            float t = (toDesignX(click.x()) - drag.x) / Math.max(1f, drag.w);
            drag.consumer.accept(Math.max(0f, Math.min(1f, t)));
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (drag != null) {
            drag = null;
            Aurora.get().saveConfig();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double horizontal, double vertical) {
        double mx = toDesignX(rawX);
        double my = toDesignY(rawY);
        if (mx < 0 || mx > W || my < 0 || my > H) {
            return super.mouseScrolled(rawX, rawY, horizontal, vertical);
        }
        if (settingsPage) {
            settingsScroll -= (float) vertical * 14f;
        } else if (!configMode) {
            scroll -= (float) vertical * 14f;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        int codepoint = input.codepoint();
        if (searchFocused && codepoint >= 32 && codepoint != 127 && search.length() < 16) {
            search += (char) codepoint;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (binding != null) {
            binding.setKey(keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
            binding = null;
            Aurora.get().saveConfig();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        drag = null;
        Aurora.get().saveConfig();
        super.removed();
    }

    @Override
    public void close() {
        Aurora.get().saveConfig();
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private static String keyName(Module module) {
        int key = module.getKey();
        if (key <= 0) {
            return "";
        }
        try {
            String name = GLFW.glfwGetKeyName(key, 0);
            if (name != null) {
                return name.toUpperCase(java.util.Locale.ROOT);
            }
        } catch (Throwable ignored) {
        }
        return "KEY" + key;
    }
}
