using System.Text.Json.Nodes;

namespace AuroraLauncher.Core;

/// <summary>Работа с version json Mojang/Fabric: правила, maven-координаты, аргументы.</summary>
public static class Meta
{
    public const string GameVersion = "1.21.11";
    public const string FabricLoader = "0.20.1";
    public const string VanillaManifest = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public const string FabricMeta = "https://meta.fabricmc.net/v2";

    public static string VersionsDir(AppConfig config) => Path.Combine(config.GameDir, "versions");
    public static string LibrariesDir(AppConfig config) => Path.Combine(config.GameDir, "libraries");
    public static string AssetsDir(AppConfig config) => Path.Combine(config.GameDir, "assets");
    public static string ModsDir(AppConfig config) => Path.Combine(config.GameDir, "mods");

    public static string VersionDir(AppConfig config, string id) => Path.Combine(VersionsDir(config), id);
    public static string VersionJson(AppConfig config, string id) => Path.Combine(VersionDir(config, id), id + ".json");
    public static string ClientJar(AppConfig config, string id) => Path.Combine(VersionDir(config, id), id + ".jar");

    public static JsonObject? TryLoadVersion(AppConfig config, string id)
    {
        string path = VersionJson(config, id);
        if (!File.Exists(path))
        {
            return null;
        }
        try
        {
            return JsonNode.Parse(File.ReadAllText(path)) as JsonObject;
        }
        catch
        {
            return null;
        }
    }

    /// <summary>Правила Mojang: allow/deny по ОС и features. Неизвестные features = запрет.</summary>
    public static bool Allowed(JsonNode? rules)
    {
        if (rules is not JsonArray array || array.Count == 0)
        {
            return true;
        }

        bool allow = false;
        foreach (var ruleNode in array)
        {
            if (ruleNode is not JsonObject rule)
            {
                continue;
            }

            bool applies = true;
            if (rule["os"] is JsonObject os)
            {
                string? name = os["name"]?.GetValue<string>();
                if (name != null && name != "windows")
                {
                    applies = false;
                }
                string? arch = os["arch"]?.GetValue<string>();
                if (arch != null)
                {
                    bool is64 = Environment.Is64BitOperatingSystem;
                    if ((arch == "x86" && is64) || (arch == "x86_64" && !is64))
                    {
                        applies = false;
                    }
                }
            }

            if (rule["features"] is JsonObject)
            {
                applies = false; // демо-режим, кастомное разрешение, quickPlay не поддерживаем
            }

            if (applies)
            {
                allow = rule["action"]?.GetValue<string>() == "allow";
            }
        }
        return allow;
    }

    /// <summary>group:artifact:version[:classifier] → путь в maven-репозитории.</summary>
    public static string MavenPath(string coordinates)
    {
        string[] parts = coordinates.Split(':');
        string group = parts[0];
        string artifact = parts.Length > 1 ? parts[1] : "";
        string version = parts.Length > 2 ? parts[2] : "";
        string? classifier = parts.Length > 3 ? parts[3] : null;

        string file = classifier == null
            ? $"{artifact}-{version}.jar"
            : $"{artifact}-{version}-{classifier}.jar";

        return Path.Combine(group.Replace('.', Path.DirectorySeparatorChar), artifact, version, file);
    }

    public static string MavenUrl(string coordinates, string? baseUrl)
    {
        string root = string.IsNullOrWhiteSpace(baseUrl) ? "https://repo1.maven.org/maven2/" : baseUrl!;
        if (!root.EndsWith('/'))
        {
            root += "/";
        }
        return root + MavenPath(coordinates).Replace('\\', '/');
    }

    public sealed record Library(string Path, string Url, bool IsNative, string? NativeClassifier);

    /// <summary>Все разрешённые библиотеки версии (без natives-классификаторов).</summary>
    public static List<Library> Libraries(JsonObject version)
    {
        var result = new List<Library>();
        if (version["libraries"] is not JsonArray libraries)
        {
            return result;
        }

        foreach (var node in libraries)
        {
            if (node is not JsonObject library || !Allowed(library["rules"]))
            {
                continue;
            }

            string? mavenName = library["name"]?.GetValue<string>();
            var artifact = library["downloads"]?["artifact"];
            if (artifact != null)
            {
                string? path = artifact["path"]?.GetValue<string>();
                string? url = artifact["url"]?.GetValue<string>();
                if (!string.IsNullOrEmpty(path) && !string.IsNullOrEmpty(url))
                {
                    result.Add(new Library(path!, url!, false, null));
                }
            }
            else if (!string.IsNullOrEmpty(mavenName))
            {
                string url = MavenUrl(mavenName!, library["url"]?.GetValue<string>());
                result.Add(new Library(MavenPath(mavenName!), url, false, null));
            }

            // нативные библиотеки (LWJGL natives-windows)
            string? classifier = library["natives"]?["windows"]?.GetValue<string>();
            if (classifier != null)
            {
                classifier = classifier.Replace("${arch}", Environment.Is64BitOperatingSystem ? "64" : "32");
                var classifiers = library["downloads"]?["classifiers"];
                if (classifiers?[classifier] is JsonObject nativeArtifact)
                {
                    string? path = nativeArtifact["path"]?.GetValue<string>();
                    string? url = nativeArtifact["url"]?.GetValue<string>();
                    if (!string.IsNullOrEmpty(path) && !string.IsNullOrEmpty(url))
                    {
                        result.Add(new Library(path!, url!, true, classifier));
                    }
                }
                else if (!string.IsNullOrEmpty(mavenName))
                {
                    string withClassifier = mavenName + ":" + classifier;
                    string url = MavenUrl(withClassifier, library["url"]?.GetValue<string>());
                    result.Add(new Library(MavenPath(withClassifier), url, true, classifier));
                }
            }
        }

        return result;
    }

    /// <summary>Разворачивает arguments.jvm / arguments.game с подстановкой переменных.</summary>
    public static List<string> Arguments(JsonObject version, string section, Dictionary<string, string> variables)
    {
        var result = new List<string>();
        if (version["arguments"]?[section] is not JsonArray array)
        {
            return result;
        }

        foreach (var node in array)
        {
            if (node is JsonValue value && node.GetValueKind() == System.Text.Json.JsonValueKind.String)
            {
                result.Add(Substitute(value.GetValue<string>(), variables));
                continue;
            }

            if (node is JsonObject entry)
            {
                if (!Allowed(entry["rules"]))
                {
                    continue;
                }
                var values = entry["value"];
                if (values is JsonArray valueArray)
                {
                    foreach (var item in valueArray)
                    {
                        result.Add(Substitute(item?.GetValue<string>() ?? "", variables));
                    }
                }
                else if (values is JsonValue single)
                {
                    result.Add(Substitute(single.GetValue<string>(), variables));
                }
            }
        }

        return result;
    }

    public static string Substitute(string input, Dictionary<string, string> variables)
    {
        string result = input;
        foreach (var (key, value) in variables)
        {
            result = result.Replace("${" + key + "}", value);
        }
        return result;
    }
}
