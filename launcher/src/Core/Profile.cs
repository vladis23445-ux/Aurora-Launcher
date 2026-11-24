using System.Text.Json;
using System.Text.Json.Serialization;

namespace AuroraLauncher.Core;

/// <summary>Общий профиль игрока: монеты, счётчики заданий, косметика.</summary>
public sealed class ProfileData
{
    [JsonPropertyName("coins")]
    public int Coins { get; set; }

    [JsonPropertyName("counters")]
    public Dictionary<string, double> Counters { get; set; } = new();

    [JsonPropertyName("claimed")]
    public List<string> Claimed { get; set; } = new();

    [JsonPropertyName("owned")]
    public List<string> Owned { get; set; } = new();

    [JsonPropertyName("equipped")]
    public Dictionary<string, string> Equipped { get; set; } = new();

    public double Counter(string key) => Counters.TryGetValue(key, out double value) ? value : 0.0;

    public bool IsClaimed(string questId) => Claimed.Contains(questId);

    public bool Owns(string cosmeticId) => Owned.Contains(cosmeticId);

    public string EquippedOf(string type) =>
        Equipped.TryGetValue(type, out string? value) ? value ?? "" : "";
}

/// <summary>Чтение и запись profile.json, который пишет мод из игры.</summary>
public static class Profile
{
    private static readonly JsonSerializerOptions Options = new()
    {
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
    };

    public static string FilePath => Path.Combine(AppConfig.RootDir, "profile.json");

    public static ProfileData Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                string json = File.ReadAllText(FilePath);
                var data = JsonSerializer.Deserialize<ProfileData>(json, Options);
                if (data != null)
                {
                    data.Counters ??= new Dictionary<string, double>();
                    data.Claimed ??= new List<string>();
                    data.Owned ??= new List<string>();
                    data.Equipped ??= new Dictionary<string, string>();
                    ApplyDefaults(data);
                    return data;
                }
            }
        }
        catch
        {
            // повреждённый файл перезапишем стартовым профилем
        }

        var fresh = new ProfileData();
        ApplyDefaults(fresh);
        Save(fresh);
        return fresh;
    }

    /// <summary>Бесплатный плащ выдаётся сразу и надет по умолчанию.</summary>
    private static void ApplyDefaults(ProfileData data)
    {
        var freeCape = Cosmetics.All.FirstOrDefault(c => c.IsFree);
        if (freeCape == null)
        {
            return;
        }
        if (!data.Owned.Contains(freeCape.Id))
        {
            data.Owned.Add(freeCape.Id);
        }
        if (string.IsNullOrEmpty(data.EquippedOf(freeCape.Key)))
        {
            data.Equipped[freeCape.Key] = freeCape.Id;
        }
    }

    public static void Save(ProfileData data)
    {
        try
        {
            Directory.CreateDirectory(AppConfig.RootDir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(data, Options));
        }
        catch
        {
            // не критично: файл перезапишет игра
        }
    }

    public static bool Buy(ProfileData data, Cosmetic cosmetic)
    {
        if (data.Owns(cosmetic.Id))
        {
            return false;
        }
        if (data.Coins < cosmetic.Price)
        {
            return false;
        }
        data.Coins -= cosmetic.Price;
        data.Owned.Add(cosmetic.Id);
        data.Equipped[cosmetic.Key] = cosmetic.Id;
        Save(data);
        return true;
    }

    public static void ToggleEquip(ProfileData data, Cosmetic cosmetic)
    {
        if (!data.Owns(cosmetic.Id))
        {
            return;
        }
        data.Equipped[cosmetic.Key] = data.EquippedOf(cosmetic.Key) == cosmetic.Id ? "" : cosmetic.Id;
        Save(data);
    }
}
