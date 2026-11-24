package dev.aurora.util;

import dev.aurora.Aurora;

import java.util.List;

/** Каталог заданий: за выполнение начисляются монеты. */
public final class Quests {

    public record Quest(String id, String title, String counter, double target, int reward) {
    }

    public static final String KILLS = "kills";
    public static final String BLOCKS_BROKEN = "blocks";
    public static final String BLOCKS_PLACED = "placed";
    public static final String DISTANCE = "distance";
    public static final String JUMPS = "jumps";
    public static final String PLAYTIME = "playtime";

    public static final List<Quest> ALL = List.of(
            new Quest("first_blood", "Первая кровь", KILLS, 1, 20),
            new Quest("kills_10", "Десятка", KILLS, 10, 60),
            new Quest("kills_50", "Охотник", KILLS, 50, 220),
            new Quest("mine_100", "Шахтёр", BLOCKS_BROKEN, 100, 40),
            new Quest("mine_500", "Проходчик", BLOCKS_BROKEN, 500, 140),
            new Quest("mine_1500", "Крот", BLOCKS_BROKEN, 1500, 320),
            new Quest("build_50", "Строитель", BLOCKS_PLACED, 50, 40),
            new Quest("build_300", "Архитектор", BLOCKS_PLACED, 300, 180),
            new Quest("walk_1000", "Ходок", DISTANCE, 1000, 70),
            new Quest("walk_5000", "Странник", DISTANCE, 5000, 260),
            new Quest("jump_200", "Прыгун", JUMPS, 200, 60),
            new Quest("play_hour", "Час в деле", PLAYTIME, 3600, 120),
            new Quest("play_five", "Пять часов", PLAYTIME, 18000, 450)
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
}
