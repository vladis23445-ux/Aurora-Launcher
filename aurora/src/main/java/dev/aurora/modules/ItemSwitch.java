package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.Animation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import org.joml.Quaternionf;

/**
 * ItemSwitch: плавная анимация смены предмета в руке.
 * При смене слота рука мягко проседает вниз и возвращается, с лёгким наклоном.
 */
public class ItemSwitch extends Module {

    private final NumberSetting duration = register(new NumberSetting("Duration", 0.30, 0.08, 1.0, 0.01));
    private final NumberSetting amplitude = register(new NumberSetting("Amplitude", 0.32, 0.05, 0.8, 0.01));
    private final NumberSetting tilt = register(new NumberSetting("Tilt", 10.0, 0.0, 45.0, 1.0));

    private Item lastItem;
    private float progress = 1f;

    public ItemSwitch(EventBus bus) {
        super(bus, "ItemSwitch", "Плавная анимация смены предмета в руке", Category.RENDER);
    }

    @Override
    public void onDisable() {
        progress = 1f;
        lastItem = null;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        Item item = mc.player.getMainHandStack().getItem();
        if (lastItem != null && item != lastItem) {
            progress = 0f;
        }
        lastItem = item;

        if (progress < 1f) {
            progress = Math.min(1f, progress + (1f / 20f) / Math.max(0.05f, duration.getFloat()));
        }
    }

    /** Вызывается из HeldItemRendererMixin. */
    public void apply(MatrixStack matrices) {
        if (progress >= 1f) {
            return;
        }
        float eased = Animation.easeOutCubic(progress);
        float dip = (1f - eased) * -amplitude.getFloat();
        matrices.translate(0f, dip, 0f);

        if (tilt.getFloat() > 0.1f) {
            float angle = (1f - eased) * (float) Math.toRadians(-tilt.getFloat());
            matrices.multiply(new Quaternionf().rotationZ(angle));
        }
    }

    public float getProgress() {
        return progress;
    }
}
