using System.Diagnostics;
using System.Text.Json.Nodes;

namespace AuroraLauncher.Core;

public static class JavaManager
{
    public const int RequiredFeature = 21;

    /// <summary>Путь к java.exe: из настроек, из PATH, из папки лаунчера.</summary>
    public static string? FindJava(AppConfig config)
    {
        if (!string.IsNullOrWhiteSpace(config.JavaPath) && config.JavaPath != "auto")
        {
            string explicitPath = config.JavaPath.Trim('"');
            if (File.Exists(explicitPath))
            {
                return explicitPath;
            }
            if (Directory.Exists(explicitPath))
            {
                string candidate = Path.Combine(explicitPath, "bin", "java.exe");
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }
        }

        string bundled = Path.Combine(AppConfig.RootDir, "jdk", "bin", "java.exe");
        if (File.Exists(bundled))
        {
            return bundled;
        }

        foreach (var dir in (Environment.GetEnvironmentVariable("PATH") ?? "").Split(';'))
        {
            try
            {
                string candidate = Path.Combine(dir.Trim(), "java.exe");
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }
            catch
            {
                // игнорируем битые записи PATH
            }
        }

        string? javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrWhiteSpace(javaHome))
        {
            string candidate = Path.Combine(javaHome, "bin", "java.exe");
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        return null;
    }

    public static bool IsCompatible(string javaPath)
    {
        try
        {
            var info = new ProcessStartInfo(javaPath, "-version")
            {
                RedirectStandardError = true,
                RedirectStandardOutput = true,
                UseShellExecute = false,
                CreateNoWindow = true,
            };
            using var process = Process.Start(info);
            if (process == null)
            {
                return false;
            }
            string output = process.StandardError.ReadToEnd();
            process.WaitForExit(4000);

            foreach (var token in output.Split('"'))
            {
                if (Version.TryParse(token.Split('.').FirstOrDefault() + "." + token.Split('.').Skip(1).FirstOrDefault(),
                        out var version))
                {
                    if (version.Major >= RequiredFeature)
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>Возвращает рабочий java.exe, при необходимости скачивает Temurin JDK 21.</summary>
    public static async Task<string> EnsureAsync(AppConfig config,
        IProgress<string> log, IProgress<double> progress, CancellationToken ct)
    {
        string? existing = FindJava(config);
        if (existing != null && IsCompatible(existing))
        {
            log.Report("Java: " + existing);
            return existing;
        }

        log.Report("Java 21 не найдена — скачиваю Temurin JDK 21 (~190 МБ)...");
        string api = "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse";
        var node = JsonNode.Parse(await Http.GetStringAsync(api, ct));
        string? link = node?[0]?["binary"]?["package"]?["link"]?.GetValue<string>();
        if (string.IsNullOrEmpty(link))
        {
            throw new InvalidOperationException("Не удалось получить ссылку на JDK 21 (api.adoptium.net).");
        }

        string target = Path.Combine(AppConfig.RootDir, "jdk");
        string zip = Path.Combine(AppConfig.RootDir, "jdk21.zip");
        Directory.CreateDirectory(AppConfig.RootDir);

        await Http.DownloadAsync(link, zip, new Progress<long>(p => progress.Report(p * 0.9)), ct);
        log.Report("Распаковываю JDK...");
        Http.ExtractZip(zip, target, stripRootFolder: true);
        try
        {
            File.Delete(zip);
        }
        catch
        {
            // файл может быть занят — не критично
        }

        string java = Path.Combine(target, "bin", "java.exe");
        if (!File.Exists(java))
        {
            throw new InvalidOperationException("JDK распакован, но bin\\java.exe не найден.");
        }

        progress.Report(100);
        log.Report("Java 21 установлена: " + java);
        return java;
    }
}
