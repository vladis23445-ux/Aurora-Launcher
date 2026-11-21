using System.Diagnostics;
using System.Text.Json.Nodes;

namespace AuroraLauncher.Core;

/// <summary>Собирает командную строку запуска (ваниль + Fabric) и стартует процесс игры.</summary>
public static class GameLauncher
{
    public static async Task<Process> LaunchAsync(AppConfig config, Account account, string javaPath,
        IProgress<string> log, CancellationToken ct)
    {
        var fabric = Meta.TryLoadVersion(config, config.VersionId)
                     ?? throw new InvalidOperationException("Профиль не установлен: " + config.VersionId);
        string baseId = fabric["inheritsFrom"]?.GetValue<string>() ?? Meta.GameVersion;
        var vanilla = Meta.TryLoadVersion(config, baseId)
                      ?? throw new InvalidOperationException("Не установлена ванильная версия " + baseId);

        string librariesDir = Meta.LibrariesDir(config);
        string clientJar = Meta.ClientJar(config, baseId);
        if (!File.Exists(clientJar))
        {
            throw new InvalidOperationException("Нет client.jar — сначала выполните установку.");
        }

        var libraries = new Dictionary<string, Meta.Library>(StringComparer.OrdinalIgnoreCase);
        foreach (var library in Meta.Libraries(vanilla))
        {
            libraries[library.Path] = library;
        }
        foreach (var library in Meta.Libraries(fabric))
        {
            libraries[library.Path] = library;
        }

        int downloaded = 0;
        int repaired = 0;
        foreach (var library in libraries.Values)
        {
            string path = Path.Combine(librariesDir, library.Path);
            if (File.Exists(path) && Http.IsValidJar(path))
            {
                continue;
            }
            bool existed = File.Exists(path);
            if (await Http.DownloadJarAsync(library.Url, path, log, ct))
            {
                downloaded++;
                if (existed)
                {
                    repaired++;
                    log.Report("Перекачана повреждённая библиотека: " + library.Path);
                }
            }
            else
            {
                log.Report("Не удалось скачать библиотеку: " + library.Path);
            }
        }
        if (downloaded > 0)
        {
            log.Report($"Библиотек догружено: {downloaded}, из них восстановлено: {repaired}");
        }

        string nativesDir = Path.Combine(Meta.VersionDir(config, config.VersionId), "natives");
        Directory.CreateDirectory(nativesDir);
        foreach (var library in libraries.Values.Where(l => l.IsNative))
        {
            string path = Path.Combine(librariesDir, library.Path);
            if (!File.Exists(path))
            {
                continue;
            }
            try
            {
                Http.ExtractZip(path, nativesDir);
            }
            catch (Exception e)
            {
                log.Report("Не удалось распаковать natives " + library.Path + ": " + e.Message);
            }
        }

        var classpath = new List<string>();
        foreach (var library in libraries.Values.Where(l => !l.IsNative))
        {
            classpath.Add(Path.Combine(librariesDir, library.Path));
        }
        classpath.Add(clientJar);

        string assetsId = vanilla["assetIndex"]?["id"]?.GetValue<string>() ?? "";
        string mainClass = fabric["mainClass"]?.GetValue<string>()
                           ?? vanilla["mainClass"]?.GetValue<string>()
                           ?? "net.minecraft.client.main.Main";

        var variables = new Dictionary<string, string>
        {
            ["natives_directory"] = nativesDir,
            ["launcher_name"] = "AuroraLauncher",
            ["launcher_version"] = "1.0.0",
            ["classpath"] = string.Join(';', classpath),
            ["classpath_separator"] = ";",
            ["library_directory"] = librariesDir,
            ["version_name"] = config.VersionId,
            ["version_type"] = "release",
            ["game_directory"] = config.GameDir,
            ["assets_root"] = Meta.AssetsDir(config),
            ["assets_index_name"] = assetsId,
            ["auth_player_name"] = account.Name,
            ["auth_uuid"] = account.Uuid,
            ["auth_access_token"] = "0",
            ["auth_session"] = "0",
            ["clientid"] = "0",
            ["auth_xuid"] = "0",
            ["user_type"] = "legacy",
            ["user_properties"] = "{}",
            ["game_assets"] = Path.Combine(Meta.AssetsDir(config), "virtual", "legacy"),
            ["resolution_width"] = config.WindowWidth.ToString(),
            ["resolution_height"] = config.WindowHeight.ToString(),
        };

        var jvmArguments = new List<string>();
        jvmArguments.AddRange(Meta.Arguments(vanilla, "jvm", variables));
        jvmArguments.AddRange(Meta.Arguments(fabric, "jvm", variables));

        jvmArguments.RemoveAll(a => a.StartsWith("-Xmx", StringComparison.OrdinalIgnoreCase)
                                    || a.StartsWith("-Xms", StringComparison.OrdinalIgnoreCase));
        jvmArguments.Add($"-Xmx{config.RamMb}M");
        jvmArguments.Add($"-Xms{Math.Min(config.RamMb, 2048)}M");
        if (!string.IsNullOrWhiteSpace(config.ExtraJvm))
        {
            jvmArguments.AddRange(SplitArguments(config.ExtraJvm));
        }

        var gameArguments = new List<string>();
        gameArguments.AddRange(Meta.Arguments(vanilla, "game", variables));
        gameArguments.AddRange(Meta.Arguments(fabric, "game", variables));

        bool hasResolution = gameArguments.Contains("--width") || gameArguments.Contains("--height");
        if (!hasResolution)
        {
            gameArguments.Add("--width");
            gameArguments.Add(config.WindowWidth.ToString());
            gameArguments.Add("--height");
            gameArguments.Add(config.WindowHeight.ToString());
        }
        if (config.Fullscreen)
        {
            gameArguments.Add("--fullscreen");
        }
        if (!string.IsNullOrWhiteSpace(config.ExtraGameArgs))
        {
            gameArguments.AddRange(SplitArguments(config.ExtraGameArgs));
        }

        var startInfo = new ProcessStartInfo(javaPath)
        {
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            WorkingDirectory = config.GameDir,
            CreateNoWindow = true,
        };
        foreach (var argument in jvmArguments)
        {
            startInfo.ArgumentList.Add(argument);
        }
        startInfo.ArgumentList.Add(mainClass);
        foreach (var argument in gameArguments)
        {
            startInfo.ArgumentList.Add(argument);
        }

        log.Report("Запуск: Java " + javaPath);
        log.Report($"Память: {config.RamMb} МБ, окно {config.WindowWidth}x{config.WindowHeight}");

        var process = new Process { StartInfo = startInfo, EnableRaisingEvents = true };
        process.OutputDataReceived += (_, e) =>
        {
            if (!string.IsNullOrWhiteSpace(e.Data))
            {
                log.Report(e.Data);
            }
        };
        process.ErrorDataReceived += (_, e) =>
        {
            if (!string.IsNullOrWhiteSpace(e.Data))
            {
                log.Report("! " + e.Data);
            }
        };

        process.Start();
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();
        return process;
    }

    public static List<string> SplitArguments(string line)
    {
        var result = new List<string>();
        var current = new System.Text.StringBuilder();
        bool inQuotes = false;

        foreach (char c in line)
        {
            if (c == '"')
            {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ' ' && !inQuotes)
            {
                if (current.Length > 0)
                {
                    result.Add(current.ToString());
                    current.Clear();
                }
                continue;
            }
            current.Append(c);
        }
        if (current.Length > 0)
        {
            result.Add(current.ToString());
        }
        return result;
    }
}
