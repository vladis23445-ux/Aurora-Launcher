using System.Diagnostics;
using System.Text.Json.Nodes;

namespace AuroraLauncher.Core;

/// <summary>Сборка мода Aurora: gradle distribution + gradlew не нужен, зовём gradle напрямую.</summary>
public static class ModBuilder
{
    public static string? FindBuiltJar(AppConfig config)
    {
        if (string.IsNullOrWhiteSpace(config.ModProjectDir))
        {
            return null;
        }
        string libs = Path.Combine(config.ModProjectDir, "build", "libs");
        if (!Directory.Exists(libs))
        {
            return null;
        }

        return Directory.GetFiles(libs, "*.jar")
            .Where(f => !f.Contains("-sources", StringComparison.OrdinalIgnoreCase)
                        && !f.Contains("-dev", StringComparison.OrdinalIgnoreCase)
                        && !f.Contains("-javadoc", StringComparison.OrdinalIgnoreCase))
            .OrderByDescending(File.GetLastWriteTimeUtc)
            .FirstOrDefault();
    }

    public static async Task<string> BuildAsync(AppConfig config, string javaPath,
        IProgress<string> log, IProgress<double> progress, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(config.ModProjectDir) || !Directory.Exists(config.ModProjectDir))
        {
            throw new InvalidOperationException(
                "Не найдена папка проекта мода. Укажите её в настройках (папка с build.gradle).");
        }

        string gradleHome = await EnsureGradleAsync(log, progress, ct);
        string gradleBat = Path.Combine(gradleHome, "bin", "gradle.bat");
        if (!File.Exists(gradleBat))
        {
            throw new InvalidOperationException("gradle.bat не найден: " + gradleBat);
        }

        string javaHome = Path.GetFullPath(Path.Combine(Path.GetDirectoryName(javaPath)!, ".."));
        log.Report("gradle build (это может занять несколько минут, качаются маппинги и Minecraft)...");

        var startInfo = new ProcessStartInfo("cmd.exe")
        {
            WorkingDirectory = config.ModProjectDir,
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            CreateNoWindow = true,
            Arguments = $"/c \"\"{gradleBat}\" build --no-daemon --console=plain\"",
        };
        startInfo.Environment["JAVA_HOME"] = javaHome;

        using var process = new Process { StartInfo = startInfo, EnableRaisingEvents = true };
        process.OutputDataReceived += (_, e) =>
        {
            if (!string.IsNullOrWhiteSpace(e.Data))
            {
                log.Report("  " + e.Data);
            }
        };
        process.ErrorDataReceived += (_, e) =>
        {
            if (!string.IsNullOrWhiteSpace(e.Data))
            {
                log.Report("  ! " + e.Data);
            }
        };

        process.Start();
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();
        await process.WaitForExitAsync(ct);

        if (process.ExitCode != 0)
        {
            throw new InvalidOperationException(
                "gradle build завершился с кодом " + process.ExitCode + ". Смотрите лог выше.");
        }

        progress.Report(100);
        string? jar = FindBuiltJar(config);
        if (jar == null)
        {
            throw new InvalidOperationException("Сборка прошла, но jar в build/libs не найден.");
        }
        log.Report("Собрано: " + Path.GetFileName(jar));
        return jar;
    }

    private static async Task<string> EnsureGradleAsync(IProgress<string> log,
        IProgress<double> progress, CancellationToken ct)
    {
        string root = Path.Combine(AppConfig.RootDir, "gradle");
        Directory.CreateDirectory(root);

        string version = "8.14.3";
        try
        {
            var current = JsonNode.Parse(await Http.GetStringAsync("https://services.gradle.org/versions/current", ct));
            version = current?["version"]?.GetValue<string>() ?? version;
        }
        catch
        {
            // остаёмся на проверенной версии
        }

        string home = Path.Combine(root, "gradle-" + version);
        if (File.Exists(Path.Combine(home, "bin", "gradle.bat")))
        {
            return home;
        }

        string zip = Path.Combine(root, $"gradle-{version}-bin.zip");
        log.Report($"Скачиваю Gradle {version}...");
        await Http.DownloadAsync($"https://services.gradle.org/distributions/gradle-{version}-bin.zip",
            zip, new Progress<long>(p => progress.Report(p * 0.8)), ct);

        log.Report("Распаковываю Gradle...");
        Http.ExtractZip(zip, root, stripRootFolder: false);
        try
        {
            File.Delete(zip);
        }
        catch
        {
        }

        if (!File.Exists(Path.Combine(home, "bin", "gradle.bat")))
        {
            throw new InvalidOperationException("Gradle распакован не туда: " + home);
        }
        return home;
    }
}
