package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.ColorSetting;
import dev.aurora.module.setting.EnumSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.ScreenProjection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * BlockHighlight: выделение блока под прицелом.
 * Плавное сглаживание по времени, заливка граней, свечение и анимации.
 */
public class BlockHighlight extends Module {

    public enum Anim {
        NONE,
        PULSE,
        RAINBOW,
        WAVE,
    }

    private final ColorSetting color = register(new ColorSetting("Color", 0xFF3BE8A0));
    private final NumberSetting width = register(new NumberSetting("Width", 1.2, 0.6, 4.0, 0.1));
    private final BooleanSetting smooth = register(new BooleanSetting("Smooth", true));
    private final NumberSetting smoothSpeed = register(new NumberSetting("SmoothSpeed", 12.0, 4.0, 30.0, 0.5));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));
    private final BooleanSetting fill = register(new BooleanSetting("Fill", true));
    private final NumberSetting fillAlpha = register(new NumberSetting("FillAlpha", 26.0, 4.0, 90.0, 1.0));
    private final EnumSetting<Anim> anim = register(new EnumSetting<>("Anim", Anim.PULSE));
    private final NumberSetting animSpeed = register(new NumberSetting("AnimSpeed", 1.0, 0.2, 5.0, 0.1));

    private final double[] current = new double[6];
    private final double[] target = new double[6];
    private boolean initialized;

    private long lastFrameNs;
    private float time;

    public BlockHighlight(EventBus bus) {
        super(bus, "BlockHighlight", "Плавное выделение блока под прицелом", Category.RENDER);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.016f : Math.min(0.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        time += dt * animSpeed.getFloat();

        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getBlockPos() == null) {
            initialized = false;
            return;
        }

        BlockPos pos = hit.getBlockPos();
        if (mc.world.getBlockState(pos).isAir()) {
            initialized = false;
            return;
        }
        if (mc.player.getEyePos().distanceTo(hit.getPos()) > 6.0) {
            initialized = false;
            return;
        }

        Box box = new Box(pos);
        target[0] = box.minX;
        target[1] = box.minY;
        target[2] = box.minZ;
        target[3] = box.maxX;
        target[4] = box.maxY;
        target[5] = box.maxZ;

        if (!initialized) {
            System.arraycopy(target, 0, current, 0, 6);
            initialized = true;
        }

        float factor = smooth.getValue()
                ? (float) (1.0 - Math.exp(-smoothSpeed.getValue() * dt))
                : 1f;
        for (int i = 0; i < 6; i++) {
            current[i] += (target[i] - current[i]) * factor;
        }

        Box animated = new Box(current[0], current[1], current[2], current[3], current[4], current[5]);
        int base = animatedColor();
        int alpha = ColorUtil.alphaOf(base);

        if (fill.getValue()) {
            int fillColor = ColorUtil.replaceAlpha(base,
                    Math.round(alpha * fillAlpha.getFloat() / 100f * 2.55f));
            ScreenProjection.fillBox(context, animated, fillColor);
        }

        if (glow.getValue()) {
            ScreenProjection.box(context, animated, 0.004f, width.getFloat() * 3.4f,
                    ColorUtil.withAlpha(base, 0.10f), ColorUtil.withAlpha(base, 0.10f));
        }

        ScreenProjection.box(context, animated, 0.004f, width.getFloat(), base,
                ColorUtil.withAlpha(base, 0.75f));
    }

    private int animatedColor() {
        int base = color.getArgb();
        switch (anim.getValue()) {
            case PULSE: {
                float wave = 0.55f + 0.45f * (float) Math.sin(time * 2.4);
                return ColorUtil.replaceAlpha(base,
                        Math.round(ColorUtil.alphaOf(base) * wave));
            }
            case RAINBOW: {
                float hue = (time * 0.15f) % 1f;
                return ColorUtil.hsb(hue, 0.65f, 1f, ColorUtil.alphaOf(base));
            }
            case WAVE: {
                float hue = ColorUtil.hueOf(base) + (float) Math.sin(time * 1.8) * 0.05f;
                float wave = 0.7f + 0.3f * (float) Math.sin(time * 3.0);
                return ColorUtil.hsb(hue, ColorUtil.saturationOf(base),
                        Math.min(1f, ColorUtil.brightnessOf(base) * wave),
                        ColorUtil.alphaOf(base));
            }
            default:
                return base;
        }
    }
}
