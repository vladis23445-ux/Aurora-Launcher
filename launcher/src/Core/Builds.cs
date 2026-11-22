using System.Text;

namespace AuroraLauncher.Core;

/// <summary>Сборка запуска: набор JVM-флагов и подстройка графики в options.txt.</summary>
public sealed class BuildProfile
{
    public required string Id { get; init; }
    public required string Name { get; init; }
    public required string Summary { get; init; }
    public string[] JvmArgs { get; init; } = Array.Empty<string>();
    public bool TuneGameOptions { get; init; }
}

/// <summary>Каталог сборок лаунчера.</summary>
public static class Builds
{
    public const string StandardId = "standard";
    public const string OptimizedId = "optimized";

    public static readonly BuildProfile Standard = new()
    {
        Id = StandardId,
        Name = "Стандарт",
        Summary = "Обычный запуск: настройки графики Minecraft не меняются.",
    };

    public static readonly BuildProfile Optimized = new()
    {
        Id = OptimizedId,
        Name = "Оптимизация",
        Summary = "Максимум FPS: G1GC, быстрый пресет графики (8 чанков, fast, без облаков).",
        JvmArgs = new[]
        {
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=40",
            "-XX:G1NewSizePercent=27",
            "-XX:G1MaxNewSizePercent=45",
            "-XX:G1HeapRegionSize=16M",
            "-XX:G1ReservePercent=20",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+DisableExplicitGC",
            "-XX:+PerfDisableSharedMem",
            "-Dsun.rmi.dgc.server.gcInterval=2147483646",
        },
        TuneGameOptions = true,
    };

    public static BuildProfile Get(string? id) =>
        string.Equals(id, OptimizedId, StringComparison.OrdinalIgnoreCase) ? Optimized : Standard;

    private static readonly (string Key, string Value)[] OptimizedOptions =
    {
        ("renderDistance", "8"),
        ("simulationDistance", "6"),
        ("graphicsMode", "fast"),
        ("particles", "minimal"),
        ("clouds", "false"),
        ("entityShadows", "false"),
        ("mipmapLevels", "0"),
        ("biomeBlendRadius", "0"),
        ("entityDistanceScaling", "0.5"),
        ("ao", "true"),
        ("maxFps", "260"),
        ("enableVsync", "false"),
        ("fancyGraphics", "false"),
    };

    /// <summary>
    /// Проставляет оптимизированные значения в options.txt, сохраняя остальные настройки.
    /// Исходный файл один раз копируется в options.txt.before-optimization.
    /// </summary>
    public static string ApplyGameOptions(string gameDir)
    {
        if (string.IsNullOrWhiteSpace(gameDir) || !Directory.Exists(gameDir))
        {
            return "Папка игры не найдена — пресет графики не применён";
        }

        string optionsPath = Path.Combine(gameDir, "options.txt");
        string backupPath = Path.Combine(gameDir, "options.txt.before-optimization");

        var lines = new List<string>();
        if (File.Exists(optionsPath))
        {
            if (!File.Exists(backupPath))
            {
                try
                {
                    File.Copy(optionsPath, backupPath, false);
                }
                catch
                {
                    // резервная копия не критична
                }
            }
            lines.AddRange(File.ReadAllLines(optionsPath, Encoding.UTF8));
        }

        int changed = 0;
        foreach (var (key, value) in OptimizedOptions)
        {
            int index = lines.FindIndex(line =>
                line.StartsWith(key + ":", StringComparison.Ordinal));
            string entry = key + ":" + value;
            if (index >= 0)
            {
                if (lines[index] != entry)
                {
                    lines[index] = entry;
                    changed++;
                }
            }
            else
            {
                lines.Add(entry);
                changed++;
            }
        }

        try
        {
            File.WriteAllLines(optionsPath, lines, new UTF8Encoding(false));
        }
        catch (Exception e)
        {
            return "Не удалось обновить options.txt: " + e.Message;
        }

        return changed == 0
            ? "Графика уже в оптимизированном пресете"
            : $"Пресет графики применён (изменено настроек: {changed})";
    }
}
