package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.EnumSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.render.Animation;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

/**
 * CustomHand: положение, дистанция, размер, наклон и анимация применения руки.
 * Анимация смены предмета вынесена в отдельный модуль ItemSwitch.
 */
public class CustomHand extends Module {

    public enum HandAnimation {
        VANILLA, SMOOTH, SLIDE, FLOAT
    }

    private final EnumSetting<HandAnimation> animation =
            register(new EnumSetting<>("Animation", HandAnimation.SMOOTH));
    private final NumberSetting posX = register(new NumberSetting("X", 0.0, -1.0, 1.0, 0.01));
    private final NumberSetting posY = register(new NumberSetting("Y", 0.0, -1.0, 1.0, 0.01));
    private final NumberSetting distance = register(new NumberSetting("Distance", -0.12, -1.0, 1.0, 0.01));
    private final NumberSetting scale = register(new NumberSetting("Scale", 1.02, 0.4, 1.8, 0.01));
    private final NumberSetting rotX = register(new NumberSetting("RotX", 0.0, -90.0, 90.0, 1.0));
    private final NumberSetting rotY = register(new NumberSetting("RotY", -6.0, -90.0, 90.0, 1.0));
    private final NumberSetting rotZ = register(new NumberSetting("RotZ", 0.0, -90.0, 90.0, 1.0));
    private final BooleanSetting onlyItems = register(new BooleanSetting("OnlyItems", false));

    private final Animation animX = new Animation(0f, 16f);
    private final Animation animY = new Animation(0f, 16f);
    private final Animation animZ = new Animation(-0.12f, 16f);
    private final Animation animRotX = new Animation(0f, 16f);
    private final Animation animRotY = new Animation(-6f, 16f);
    private final Animation animRotZ = new Animation(0f, 16f);
    private final Animation animScale = new Animation(1.02f, 16f);

    public CustomHand(EventBus bus) {
        super(bus, "CustomHand", "Положение, дистанция, размер и анимация руки", Category.RENDER);
    }

    @Override
    public void onEnable() {
        updateValues(true);
    }

    /** Вызывается из миксина HeldItemRenderer. */
    public void applyTransform(MatrixStack matrices) {
        if (onlyItems.getValue()) {
            boolean holdingItem = mc.player != null && !mc.player.getMainHandStack().isEmpty();
            if (!holdingItem) {
                return;
            }
        }

        updateValues(animation.getValue() == HandAnimation.VANILLA);

        float px = animX.getValue();
        float py = animY.getValue();
        float pz = animZ.getValue();
        float s = animScale.getValue();

        matrices.translate(px, py, pz);
        matrices.scale(s, s, s);

        if (Math.abs(animRotX.getValue()) > 0.01f) {
            matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(animRotX.getValue())));
        }
        if (Math.abs(animRotY.getValue()) > 0.01f) {
            matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(animRotY.getValue())));
        }
        if (Math.abs(animRotZ.getValue()) > 0.01f) {
            matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(animRotZ.getValue())));
        }
    }

    private void updateValues(boolean instant) {
        float floatOffset = 0f;
        if (animation.getValue() == HandAnimation.FLOAT) {
            floatOffset = (float) Math.sin(System.currentTimeMillis() / 420.0) * 0.02f;
        }

        animX.setTarget(posX.getFloat());
        animY.setTarget(posY.getFloat() + floatOffset);
        animZ.setTarget(distance.getFloat());
        animRotX.setTarget(rotX.getFloat());
        animRotY.setTarget(rotY.getFloat());
        animRotZ.setTarget(rotZ.getFloat());
        animScale.setTarget(scale.getFloat());

        float speed = switch (animation.getValue()) {
            case SLIDE -> 7f;
            case FLOAT -> 10f;
            case SMOOTH -> 16f;
            case VANILLA -> 90f;
        };
        animX.setSpeed(speed);
        animY.setSpeed(speed);
        animZ.setSpeed(speed);
        animRotX.setSpeed(speed);
        animRotY.setSpeed(speed);
        animRotZ.setSpeed(speed);
        animScale.setSpeed(speed);

        if (instant) {
            animX.snap(posX.getFloat());
            animY.snap(posY.getFloat());
            animZ.snap(distance.getFloat());
            animRotX.snap(rotX.getFloat());
            animRotY.snap(rotY.getFloat());
            animRotZ.snap(rotZ.getFloat());
            animScale.snap(scale.getFloat());
            return;
        }

        float dt = 1f / 60f;
        animX.update(dt);
        animY.update(dt);
        animZ.update(dt);
        animRotX.update(dt);
        animRotY.update(dt);
        animRotZ.update(dt);
        animScale.update(dt);
    }
}
