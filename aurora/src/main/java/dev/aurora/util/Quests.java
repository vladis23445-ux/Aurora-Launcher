package dev.aurora.util;

import dev.aurora.Aurora;

import java.util.ArrayList;
import java.util.List;

/** Каталог заданий: что сделать и сколько монет за это дают. */
public final class Quests {

    public record Quest(String id, String title, String desc, String counter, double target, int reward) {
    }

    public static final String KILLS = "kills";
    public static final String BLOCKS_BROKEN = "blocks";
    public static final String BLOCKS_PLACED = "placed";
    public static final String DISTANCE = "distance";
    public static final String JUMPS = "jumps";
    public static final String PLAYTIME = "playtime";

    public static final List<Quest> ALL = List.of(
            new Quest("first_blood", "Первая кровь", "Убейте любого моба или игрока", KILLS, 1, 20),
            new Quest("kills_10", "Десятка", "Убейте 10 мобов или игроков", KILLS, 10, 60),
            new Quest("kills_50", "Охотник", "Убейте 50 мобов или игроков", KILLS, 50, 220),
            new Quest("mine_100", "Шахтёр", "Сломайте 100 блоков", BLOCKS_BROKEN, 100, 40),
            new Quest("mine_500", "Проходчик", "Сломайте 500 блоков", BLOCKS_BROKEN, 500, 140),
            new Quest("mine_1500", "Крот", "Сломайте 1500 блоков", BLOCKS_BROKEN, 1500, 320),
            new Quest("build_50", "Строитель", "Поставьте 50 блоков", BLOCKS_PLACED, 50, 40),
            new Quest("build_300", "Архитектор", "Поставьте 300 блоков", BLOCKS_PLACED, 300, 180),
            new Quest("walk_1000", "Ходок", "Пройдите 1000 метров", DISTANCE, 1000, 70),
            new Quest("walk_5000", "Странник", "Пройдите 5000 метров", DISTANCE, 5000, 260),
            new Quest("jump_200", "Прыгун", "Прыгните 200 раз", JUMPS, 200, 60),
            new Quest("play_hour", "Час в деле", "Играйте 1 час", PLAYTIME, 3600, 120),
            new Quest("play_five", "Пять часов", "Играйте 5 часов", PLAYTIME, 18000, 450)
    );

    private Quests() {
    }

    /** Проверяет выполнение и начисляет монеты за новые задания. */
    public static void check() {
        for (Quest quest : ALL) {
            if (Profile.isClaimed(quest.id())) {
                continue;
            }
            if (Profile.counter(quest.counter()) + 0.0001 < quest.target()) {
                continue;
            }
            if (Profile.claim(quest.id(), quest.reward())) {
                Aurora.LOGGER.info("Задание выполнено: {} (+{} монет)", quest.title(), quest.reward());
            }
        }
    }

    public static double progress(Quest quest) {
        return Math.min(quest.target(), Profile.counter(quest.counter()));
    }

    public static float fraction(Quest quest) {
        return (float) Math.max(0.0, Math.min(1.0, progress(quest) / quest.target()));
    }

    /** Ближайшие незакрытые задания — для HUD и лаунчера. */
    public static List<Quest> active(int limit) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : ALL) {
            if (Profile.isClaimed(quest.id())) {
                continue;
            }
            result.add(quest);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public static String formatProgress(Quest quest) {
        return formatProgress(quest, progress(quest));
    }

    public static String formatProgress(Quest quest, double value) {
        switch (quest.counter()) {
            case DISTANCE:
                return String.format(java.util.Locale.ROOT, "%.0f / %.0f м", value, quest.target());
            case PLAYTIME:
                return String.format(java.util.Locale.ROOT, "%.0f / %.0f мин", value / 60.0, quest.target() / 60.0);
            default:
                return String.format(java.util.Locale.ROOT, "%.0f / %.0f", value, quest.target());
        }
    }
}
