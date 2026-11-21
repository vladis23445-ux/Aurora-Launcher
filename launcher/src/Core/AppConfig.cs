using System.Text.Json;
using System.Text.Json.Serialization;

namespace AuroraLauncher.Core;

public sealed class Account
{
    public string Name { get; set; } = "";
    public string Uuid { get; set; } = "";
    public string Type { get; set; } = "offline";

    public override string ToString() => Name + "  (offline)";
}

public sealed class AppConfig
{
    public List<Account> Accounts { get; set; } = new();
    public string? SelectedAccount { get; set; }

    public string JavaPath { get; set; } = "auto";
    public int RamMb { get; set; } = 4096;
    public string GameDir { get; set; } = "";
    public int WindowWidth { get; set; } = 1280;
    public int WindowHeight { get; set; } = 720;
    public bool Fullscreen { get; set; }
    public string ExtraJvm { get; set; } = "";
    public string ExtraGameArgs { get; set; } = "";
    public string VersionId { get; set; } = "fabric-loader-1.21.11-0.20.1";
    public bool CloseOnLaunch { get; set; } = true;
    public bool DiscordRpc { get; set; } = true;
    public string DiscordAppId { get; set; } = "1573457370288488568";

    [JsonIgnore]
    public string ModProjectDir { get; set; } = "";

    public static string RootDir =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "AuroraLauncher");

    public static string ConfigPath => Path.Combine(RootDir, "config.json");

    public static AppConfig Load()
    {
        Directory.CreateDirectory(RootDir);
        if (File.Exists(ConfigPath))
        {
            try
            {
                var loaded = JsonSerializer.Deserialize<AppConfig>(File.ReadAllText(ConfigPath));
                if (loaded != null)
                {
                    loaded.Normalize();
                    return loaded;
                }
            }
            catch
            {
                // битый конфиг — начинаем с чистого
            }
        }

        var config = new AppConfig();
        config.Normalize();
        return config;
    }

    private void Normalize()
    {
        if (string.IsNullOrWhiteSpace(GameDir))
        {
            GameDir = Path.Combine(RootDir, "game");
        }
        if (RamMb < 1024)
        {
            RamMb = 1024;
        }
        if (string.IsNullOrWhiteSpace(ModProjectDir))
        {
            ModProjectDir = FindModProject();
        }
        Directory.CreateDirectory(GameDir);
    }

    /// <summary>Ищем папку мода рядом с лаунчером (..\aurora или .\aurora).</summary>
    public static string FindModProject()
    {
        string baseDir = AppContext.BaseDirectory;
        string[] candidates =
        {
            Path.GetFullPath(Path.Combine(baseDir, "..", "aurora")),
            Path.GetFullPath(Path.Combine(baseDir, "aurora")),
            Path.GetFullPath(Path.Combine(baseDir, "..", "..", "aurora")),
        };
        foreach (var candidate in candidates)
        {
            if (File.Exists(Path.Combine(candidate, "build.gradle")))
            {
                return candidate;
            }
        }
        return "";
    }

    public void Save()
    {
        Directory.CreateDirectory(RootDir);
        var json = JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(ConfigPath, json);
    }

    public static string OfflineUuid(string name)
    {
        using var md5 = System.Security.Cryptography.MD5.Create();
        var hash = md5.ComputeHash(System.Text.Encoding.UTF8.GetBytes("OfflinePlayer:" + name));
        hash[6] = (byte)((hash[6] & 0x0F) | 0x30);
        hash[8] = (byte)((hash[8] & 0x3F) | 0x80);
        return new Guid(
            BitConverter.ToInt32(hash, 0),
            BitConverter.ToInt16(hash, 4),
            BitConverter.ToInt16(hash, 6),
            hash[8], hash[9], hash[10], hash[11], hash[12], hash[13], hash[14], hash[15]
        ).ToString("N");
    }
}
