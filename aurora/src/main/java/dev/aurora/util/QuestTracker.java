package dev.aurora.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;

/**
 * Счётчики для заданий: убийства, блоки, расстояние, прыжки, время в игре.
 * Копятся в profile.json, лаунчер показывает их как прогресс.
 */
public final class QuestTracker {

    private static double lastX;
    private static double lastZ;
    private static boolean hasPosition;
    private static int ticks;

    private QuestTracker() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            hasPosition = false;
            return;
        }

        PlayerEntity player = client.player;
        double x = player.getX();
        double z = player.getZ();

        if (hasPosition) {
            double dx = x - lastX;
            double dz = z - lastZ;
            double moved = Math.sqrt(dx * dx + dz * dz);
            // отсекаем телепорты и рывки
            if (moved > 0.0 && moved < 8.0) {
                Profile.addCounter(Quests.DISTANCE, moved);
            }
        }
        lastX = x;
        lastZ = z;
        hasPosition = true;

        ticks++;
        if (ticks % 20 == 0) {
            Profile.addCounter(Quests.PLAYTIME, 1.0);
            Quests.check();
            Profile.saveIfDirty();
        }
    }

    public static void onKill() {
        Profile.addCounter(Quests.KILLS, 1.0);
        Quests.check();
    }

    public static void onBlockBroken() {
        Profile.addCounter(Quests.BLOCKS_BROKEN, 1.0);
        Quests.check();
    }

    public static void onBlockPlaced(PlayerEntity player, Hand hand) {
        if (player == null || hand == null) {
            return;
        }
        if (!(player.getStackInHand(hand).getItem() instanceof BlockItem)) {
            return;
        }
        Profile.addCounter(Quests.BLOCKS_PLACED, 1.0);
        Quests.check();
    }

    public static void onJump() {
        Profile.addCounter(Quests.JUMPS, 1.0);
        Quests.check();
    }
}
