using System.Text.Json.Nodes;

namespace AuroraLauncher.Core;

public static class Installer
{
    /// <summary>Скачивает ванильную 1.21.11: json, client.jar, библиотеки, ассеты.</summary>
    public static async Task InstallVanillaAsync(AppConfig config,
        IProgress<string> log, IProgress<double> progress, CancellationToken ct)
    {
        Directory.CreateDirectory(Meta.VersionsDir(config));
        Directory.CreateDirectory(Meta.LibrariesDir(config));
        Directory.CreateDirectory(Meta.AssetsDir(config));

        log.Report("[1/5] Манифест версий Mojang...");
        progress.Report(2);
        var manifest = JsonNode.Parse(await Http.GetStringAsync(Meta.VanillaManifest, ct))!;
        JsonObject? entry = null;
        foreach (var node in manifest["versions"]!.AsArray())
        {
            if (node?["id"]?.GetValue<string>() == Meta.GameVersion)
            {
                entry = node!.AsObject();
                break;
            }
        }
        if (entry == null)
        {
            throw new InvalidOperationException($"Версия {Meta.GameVersion} отсутствует в манифесте Mojang.");
        }

        string versionUrl = entry["url"]!.GetValue<string>();
        var version = JsonNode.Parse(await Http.GetStringAsync(versionUrl, ct))!.AsObject();
        Directory.CreateDirectory(Meta.VersionDir(config, Meta.GameVersion));
        await File.WriteAllTextAsync(Meta.VersionJson(config, Meta.GameVersion),
            version.ToJsonString(new System.Text.Json.JsonSerializerOptions { WriteIndented = false }), ct);
        progress.Report(6);

        string clientUrl = version["downloads"]?["client"]?["url"]?.GetValue<string>()
                           ?? throw new InvalidOperationException("Нет ссылки на client.jar.");
        string clientJar = Meta.ClientJar(config, Meta.GameVersion);
        if (!File.Exists(clientJar))
        {
            log.Report("[2/5] Клиент Minecraft " + Meta.GameVersion + "...");
            await Http.DownloadAsync(clientUrl, clientJar, null, ct);
        }
        else
        {
            log.Report("[2/5] client.jar уже на месте");
        }
        progress.Report(20);

        log.Report("[3/5] Библиотеки...");
        await EnsureLibrariesAsync(config, version, log, ct, 20, 55, progress);
        progress.Report(55);

        log.Report("[4/5] Ассеты (звук, языки, модели)...");
        await InstallAssetsAsync(config, version, log, progress, ct);
        progress.Report(98);

        log.Report("[5/5] Ванильный клиент готов.");
    }

    /// <summary>Ставит Fabric-профиль и его библиотеки, возвращает id версии.</summary>
    public static async Task<string> InstallFabricAsync(AppConfig config,
        IProgress<string> log, IProgress<double> progress, CancellationToken ct)
    {
        log.Report("Fabric loader " + Meta.FabricLoader + "...");
        string url = $"{Meta.FabricMeta}/versions/loader/{Meta.GameVersion}/{Meta.FabricLoader}/profile/json";
        var profile = JsonNode.Parse(await Http.GetStringAsync(url, ct))!.AsObject();

        string id = profile["id"]?.GetValue<string>() ?? $"fabric-loader-{Meta.FabricLoader}-{Meta.GameVersion}";
        Directory.CreateDirectory(Meta.VersionDir(config, id));
        await File.WriteAllTextAsync(Meta.VersionJson(config, id),
            profile.ToJsonString(new System.Text.Json.JsonSerializerOptions { WriteIndented = false }), ct);

        config.VersionId = id;
        await EnsureLibrariesAsync(config, profile, log, ct, 0, 0, null);
        log.Report("Fabric профиль: " + id);
        return id;
    }

    /// <summary>Fabric API + наш мод в папку mods.</summary>
    public static async Task InstallModsAsync(AppConfig config, string javaPath,
        IProgress<string> log, IProgress<double> progress, CancellationToken ct)
    {
        Directory.CreateDirectory(Meta.ModsDir(config));

        log.Report("Fabric API для 1.21.11...");
        string query = "https://api.modrinth.com/v2/project/fabric-api/version"
                       + "?game_versions=" + Uri.EscapeDataString("[\"1.21.11\"]")
                       + "&loaders=" + Uri.EscapeDataString("[\"fabric\"]");
        var versions = JsonNode.Parse(await Http.GetStringAsync(query, ct))!.AsArray();
        if (versions.Count == 0)
        {
            throw new InvalidOperationException("Modrinth не вернул сборку Fabric API под 1.21.11.");
        }

        var files = versions[0]?["files"]?.AsArray();
        string apiUrl = files![0]!["url"]!.GetValue<string>();
        string apiName = files[0]!["filename"]!.GetValue<string>();
        string apiPath = Path.Combine(Meta.ModsDir(config), apiName);
        if (!File.Exists(apiPath))
        {
            await Http.DownloadAsync(apiUrl, apiPath, null, ct);
        }
        log.Report("Fabric API: " + apiName);
        progress.Report(40);

        string? modJar = ModBuilder.FindBuiltJar(config);
        if (modJar == null)
        {
            log.Report("Сборка мода Aurora (gradle build)...");
            modJar = await ModBuilder.BuildAsync(config, javaPath, log, progress, ct);
        }

        string target = Path.Combine(Meta.ModsDir(config), Path.GetFileName(modJar));
        if (!string.Equals(Path.GetFullPath(modJar), Path.GetFullPath(target), StringComparison.OrdinalIgnoreCase))
        {
            File.Copy(modJar, target, true);
        }
        log.Report("Мод Aurora: " + Path.GetFileName(target));
        progress.Report(100);
    }

    public static async Task EnsureLibrariesAsync(AppConfig config, JsonObject version,
        IProgress<string> log, CancellationToken ct,
        double progressFrom = 0, double progressTo = 0, IProgress<double>? progress = null)
    {
        var libraries = Meta.Libraries(version);
        var missing = libraries
            .Where(lib =>
            {
                string path = Path.Combine(Meta.LibrariesDir(config), lib.Path);
                return !File.Exists(path) || !Http.IsValidJar(path);
            })
            .GroupBy(lib => lib.Path)
            .Select(group => group.First())
            .ToList();

        if (missing.Count == 0)
        {
            return;
        }

        log.Report($"Загружаю библиотек: {missing.Count}");
        int done = 0;
        int total = missing.Count;

        await Parallel.ForEachAsync(missing,
            new ParallelOptions { MaxDegreeOfParallelism = 8, CancellationToken = ct },
            async (library, token) =>
            {
                string path = Path.Combine(Meta.LibrariesDir(config), library.Path);
                if (!await Http.DownloadJarAsync(library.Url, path, log, token))
                {
                    log.Report("Не удалось скачать " + library.Path);
                }
                int current = Interlocked.Increment(ref done);
                if (progress != null && progressTo > progressFrom)
                {
                    progress.Report(progressFrom + (progressTo - progressFrom) * current / total);
                }
            });
    }

    private static async Task InstallAssetsAsync(AppConfig config, JsonObject version,
        IProgress<string> log, IProgress<double> progress, CancellationToken ct)
    {
        var assetIndex = version["assetIndex"];
        string indexUrl = assetIndex?["url"]?.GetValue<string>()
                          ?? throw new InvalidOperationException("В версии нет assetIndex.");
        string indexId = assetIndex?["id"]?.GetValue<string>() ?? Meta.GameVersion;

        string indexPath = Path.Combine(Meta.AssetsDir(config), "indexes", indexId + ".json");
        Directory.CreateDirectory(Path.GetDirectoryName(indexPath)!);
        if (!File.Exists(indexPath))
        {
            await Http.DownloadAsync(indexUrl, indexPath, null, ct);
        }

        var index = JsonNode.Parse(await File.ReadAllTextAsync(indexPath, ct))!.AsObject();
        if (index["objects"] is not JsonObject objects)
        {
            return;
        }

        var entries = objects.ToList();
        var missing = new List<(string hash, string path)>();
        foreach (var (_, node) in entries)
        {
            string? hash = node?["hash"]?.GetValue<string>();
            if (string.IsNullOrEmpty(hash))
            {
                continue;
            }
            string target = Path.Combine(Meta.AssetsDir(config), "objects", hash[..2], hash);
            if (!File.Exists(target))
            {
                missing.Add((hash, target));
            }
        }

        if (missing.Count == 0)
        {
            log.Report("Ассеты уже скачаны.");
            return;
        }

        log.Report($"Загружаю ассетов: {missing.Count}");
        int done = 0;
        int total = missing.Count;

        await Parallel.ForEachAsync(missing,
            new ParallelOptions { MaxDegreeOfParallelism = 16, CancellationToken = ct },
            async (asset, token) =>
            {
                string url = $"https://resources.download.minecraft.net/{asset.hash[..2]}/{asset.hash}";
                try
                {
                    await Http.DownloadAsync(url, asset.path, null, token);
                }
                catch
                {
                    // отдельные ассеты могут быть недоступны — не прерываем установку
                }
                int current = Interlocked.Increment(ref done);
                if (current % 50 == 0 || current == total)
                {
                    progress.Report(55 + 40.0 * current / total);
                }
            });

        log.Report("Ассеты готовы.");
    }
}
