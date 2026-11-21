using System.Reflection;

namespace AuroraLauncher.Core;

/// <summary>
/// Мод Aurora вшит в лаунчер: при первом запуске он сам кладёт jar в mods,
/// поэтому другу достаточно одного exe.
/// </summary>
public static class BundledMod
{
    public const string FileName = "aurora-1.0.0.jar";

    public static void Install(AppConfig config, Action<string> log)
    {
        try
        {
            using var stream = Assembly.GetExecutingAssembly()
                .GetManifestResourceStream("aurora-mod.jar");
            if (stream == null)
            {
                return;
            }

            string mods = Path.Combine(config.GameDir, "mods");
            Directory.CreateDirectory(mods);
            string target = Path.Combine(mods, FileName);

            if (File.Exists(target) && new FileInfo(target).Length == stream.Length)
            {
                return;
            }

            string temp = target + ".part";
            using (var file = File.Create(temp))
            {
                stream.CopyTo(file);
            }

            File.Move(temp, target, true);
            log("Мод Aurora установлен в mods");
        }
        catch (Exception ex)
        {
            log("Не удалось установить мод: " + ex.Message);
        }
    }
}
