namespace AuroraLauncher.Core;

/// <summary>Задание: счётчик, цель и награда монетами. Совпадает с каталогом в моде.</summary>
public sealed class Quest
{
    public required string Id { get; init; }
    public required string Title { get; init; }
    public required string Counter { get; init; }
    public required double Target { get; init; }
    public required int Reward { get; init; }
    public required string Unit { get; init; }
    public string Hint { get; init; } = "";
}

public static class Quests
{
    public const string Kills = "kills";
    public const string BlocksBroken = "blocks";
    public const string BlocksPlaced = "placed";
    public const string Distance = "distance";
    public const string Jumps = "jumps";
    public const string Playtime = "playtime";

    public static readonly List<Quest> All = new()
    {
        new Quest { Id = "first_blood", Title = "Первая кровь", Counter = Kills, Target = 1, Reward = 20, Unit = "убийство" },
        new Quest { Id = "kills_10", Title = "Десятка", Counter = Kills, Target = 10, Reward = 60, Unit = "убийств" },
        new Quest { Id = "kills_50", Title = "Охотник", Counter = Kills, Target = 50, Reward = 220, Unit = "убийств" },
        new Quest { Id = "mine_100", Title = "Шахтёр", Counter = BlocksBroken, Target = 100, Reward = 40, Unit = "блоков" },
        new Quest { Id = "mine_500", Title = "Проходчик", Counter = BlocksBroken, Target = 500, Reward = 140, Unit = "блоков" },
        new Quest { Id = "mine_1500", Title = "Крот", Counter = BlocksBroken, Target = 1500, Reward = 320, Unit = "блоков" },
        new Quest { Id = "build_50", Title = "Строитель", Counter = BlocksPlaced, Target = 50, Reward = 40, Unit = "блоков" },
        new Quest { Id = "build_300", Title = "Архитектор", Counter = BlocksPlaced, Target = 300, Reward = 180, Unit = "блоков" },
        new Quest { Id = "walk_1000", Title = "Ходок", Counter = Distance, Target = 1000, Reward = 70, Unit = "м" },
        new Quest { Id = "walk_5000", Title = "Странник", Counter = Distance, Target = 5000, Reward = 260, Unit = "м" },
        new Quest { Id = "jump_200", Title = "Прыгун", Counter = Jumps, Target = 200, Reward = 60, Unit = "прыжков" },
        new Quest { Id = "play_hour", Title = "Час в деле", Counter = Playtime, Target = 3600, Reward = 120, Unit = "с" },
        new Quest { Id = "play_five", Title = "Пять часов", Counter = Playtime, Target = 18000, Reward = 450, Unit = "с" },
    };

    public static double Progress(ProfileData data, Quest quest) =>
        Math.Min(quest.Target, data.Counter(quest.Counter));

    public static double Fraction(ProfileData data, Quest quest) =>
        Math.Clamp(Progress(data, quest) / quest.Target, 0, 1);

    /// <summary>Следующее невыполненное задание — для карточки в окне.</summary>
    public static Quest? NextQuest(ProfileData data) =>
        All.Where(q => !data.IsClaimed(q.Id)).OrderBy(q => q.Target).FirstOrDefault();

    public static int ClaimedCount(ProfileData data) =>
        All.Count(q => data.IsClaimed(q.Id));

    public static string FormatProgress(Quest quest, double value)
    {
        return quest.Counter switch
        {
            Distance => $"{value:0} / {quest.Target:0} м",
            Playtime => $"{value / 60:0} / {quest.Target / 60:0} мин",
            _ => $"{value:0} / {quest.Target:0}",
        };
    }
}
