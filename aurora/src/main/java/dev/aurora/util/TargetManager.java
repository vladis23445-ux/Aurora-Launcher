package dev.aurora.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;

import java.util.Comparator;

public final class TargetManager {

    private static final long TIMEOUT_MS = 4000L;

    private static LivingEntity target;
    private static long lastAttack;

    private TargetManager() {
    }

    public static void setTarget(LivingEntity entity) {
        target = entity;
        lastAttack = System.currentTimeMillis();
    }

    public static void clear() {
        target = null;
        lastAttack = 0;
    }

    /** Последняя атакованная цель, если она ещё жива и не «протухла». */
    public static LivingEntity getTarget() {
        if (target == null) {
            return null;
        }
        if (!target.isAlive() || target.isRemoved()
                || System.currentTimeMillis() - lastAttack > TIMEOUT_MS) {
            target = null;
            return null;
        }
        return target;
    }

    /** Ближайший игрок в радиусе (резерв, если цель не была выбрана атакой). */
    public static PlayerEntity nearestPlayer(double range) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return null;
        }
        return client.world.getPlayers().stream()
                .filter(p -> p != client.player && p.isAlive() && !p.isSpectator())
                .filter(p -> p.squaredDistanceTo(client.player) <= range * range)
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(client.player)))
                .orElse(null);
    }

    public static boolean isRecentlyAttacked() {
        return System.currentTimeMillis() - lastAttack <= TIMEOUT_MS;
    }
}
