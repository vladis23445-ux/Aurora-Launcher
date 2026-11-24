package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.setting.BooleanSetting;
import dev.aurora.module.setting.EnumSetting;
import dev.aurora.module.setting.NumberSetting;
import dev.aurora.util.TargetManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.Comparator;

/**
 * KillAura: автоматически бьёт ближайшую цель в радиусе.
 * Два режима — по откату удара (как у игрока) и по фиксированному CPS.
 */
public class KillAura extends Module {

    public enum Mode {
        LEGIT,
        FAST,
    }

    private final NumberSetting range = register(new NumberSetting("Range", 3.4, 2.5, 6.0, 0.1));
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", Mode.LEGIT));
    private final NumberSetting cps = register(new NumberSetting("CPS", 10, 1, 20, 1));
    private final BooleanSetting players = register(new BooleanSetting("Players", true));
    private final BooleanSetting mobs = register(new BooleanSetting("Mobs", true));
    private final BooleanSetting onlyVisible = register(new BooleanSetting("OnlyVisible", true));
    private final BooleanSetting swing = register(new BooleanSetting("Swing", true));
    private final NumberSetting angle = register(new NumberSetting("Angle", 70, 20, 180, 5));

    private long lastAttack;

    public KillAura(EventBus bus) {
        super(bus, "KillAura", "Автоматическая атака ближайшей цели", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        lastAttack = 0L;
    }

    @Override
    public void onDisable() {
        lastAttack = 0L;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        if (mc.currentScreen != null || mc.player.isSpectator() || !mc.player.isAlive()) {
            return;
        }

        LivingEntity target = findTarget();
        if (target == null) {
            return;
        }
        if (!inFront(target)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (mode.getValue() == Mode.LEGIT) {
            if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) {
                return;
            }
        } else {
            long delay = (long) (1000.0 / Math.max(1.0, cps.getValue()));
            if (now - lastAttack < delay) {
                return;
            }
        }

        try {
            mc.interactionManager.attackEntity(mc.player, target);
            if (swing.getValue()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            TargetManager.setTarget(target);
            lastAttack = now;
        } catch (Throwable ignored) {
        }
    }

    private LivingEntity findTarget() {
        double maxDistance = range.getValue();
        double maxSquared = maxDistance * maxDistance;
        boolean allowPlayers = players.getValue();
        boolean allowMobs = mobs.getValue();
        boolean visibleOnly = onlyVisible.getValue();

        return mc.world.getEntitiesByClass(LivingEntity.class,
                        mc.player.getBoundingBox().expand(maxDistance),
                        entity -> isValid(entity, allowPlayers, allowMobs))
                .stream()
                .filter(entity -> mc.player.squaredDistanceTo(entity) <= maxSquared)
                .filter(entity -> !visibleOnly || mc.player.canSee(entity))
                .min(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)))
                .orElse(null);
    }

    /** Бьём только то, что перед нами: угол между взглядом и целью не больше Angle. */
    private boolean inFront(LivingEntity target) {
        double maxAngle = Math.toRadians(angle.getValue());
        if (maxAngle >= Math.PI - 0.01) {
            return true;
        }
        net.minecraft.util.math.Vec3d eye = mc.player.getEyePos();
        net.minecraft.util.math.Vec3d to = new net.minecraft.util.math.Vec3d(
                target.getX() - eye.x,
                target.getY() + target.getHeight() * 0.5 - eye.y,
                target.getZ() - eye.z);
        double length = to.length();
        if (length < 0.001) {
            return true;
        }
        double dot = mc.player.getRotationVec(1f).dotProduct(to.multiply(1.0 / length));
        return dot >= Math.cos(maxAngle);
    }

    private boolean isValid(Entity entity, boolean allowPlayers, boolean allowMobs) {
        if (!(entity instanceof LivingEntity living) || living == mc.player) {
            return false;
        }
        if (!living.isAlive() || living.isRemoved() || living.isInvulnerable()) {
            return false;
        }
        if (living instanceof PlayerEntity player) {
            return allowPlayers && !player.isSpectator() && !player.isCreative();
        }
        return allowMobs;
    }
}
