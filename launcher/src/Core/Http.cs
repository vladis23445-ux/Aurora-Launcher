using System.IO.Compression;
using System.Net.Http;

namespace AuroraLauncher.Core;

public static class Http
{
    public static readonly HttpClient Client = CreateClient();

    private static HttpClient CreateClient()
    {
        var handler = new SocketsHttpHandler
        {
            AutomaticDecompression = System.Net.DecompressionMethods.GZip | System.Net.DecompressionMethods.Deflate,
            MaxConnectionsPerServer = 32,
            PooledConnectionLifetime = TimeSpan.FromMinutes(5),
        };
        var client = new HttpClient(handler)
        {
            Timeout = TimeSpan.FromMinutes(30),
        };
        client.DefaultRequestHeaders.UserAgent.ParseAdd("AuroraLauncher/1.0");
        return client;
    }

    public static async Task<string> GetStringAsync(string url, CancellationToken ct = default)
    {
        using var response = await Client.GetAsync(url, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStringAsync(ct);
    }

    /// <summary>Скачивание через .part с проверкой длины: битые файлы не остаются.</summary>
    public static async Task DownloadAsync(string url, string path,
        IProgress<long>? progress = null, CancellationToken ct = default)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);

        string part = path + ".part";
        try
        {
            using var response = await Client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct);
            response.EnsureSuccessStatusCode();

            long total = response.Content.Headers.ContentLength ?? -1;
            long received = 0;

            await using (var source = await response.Content.ReadAsStreamAsync(ct))
            await using (var target = new FileStream(part, FileMode.Create, FileAccess.Write, FileShare.None, 65536, true))
            {
                var buffer = new byte[65536];
                int read;
                while ((read = await source.ReadAsync(buffer, ct)) > 0)
                {
                    await target.WriteAsync(buffer.AsMemory(0, read), ct);
                    received += read;
                    if (total > 0)
                    {
                        progress?.Report(received * 100 / total);
                    }
                }
            }

            if (total > 0 && received != total)
            {
                throw new IOException($"неполная загрузка: {received} из {total} байт");
            }

            File.Move(part, path, true);
        }
        catch
        {
            try
            {
                if (File.Exists(part))
                {
                    File.Delete(part);
                }
            }
            catch
            {
                // временный файл мог остаться — не критично
            }
            throw;
        }
    }

    /// <summary>Проверка, что jar читается как zip (ловит обрезанные загрузки).</summary>
    public static bool IsValidJar(string path)
    {
        try
        {
            if (!File.Exists(path) || new FileInfo(path).Length < 256)
            {
                return false;
            }
            using var zip = ZipFile.OpenRead(path);
            return zip.Entries.Count > 0;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>Скачивание библиотеки с ретраями и проверкой целостности.</summary>
    public static async Task<bool> DownloadJarAsync(string url, string path,
        IProgress<string>? log, CancellationToken ct, int attempts = 3)
    {
        for (int attempt = 1; attempt <= attempts; attempt++)
        {
            try
            {
                if (File.Exists(path))
                {
                    File.Delete(path);
                }
                await DownloadAsync(url, path, null, ct);
                if (IsValidJar(path))
                {
                    return true;
                }
                log?.Report($"Повреждён файл {Path.GetFileName(path)}, попытка {attempt}/{attempts}");
            }
            catch (OperationCanceledException)
            {
                throw;
            }
            catch (Exception e)
            {
                log?.Report($"Ошибка загрузки {Path.GetFileName(path)}: {e.Message}");
            }
            await Task.Delay(400, ct);
        }
        return false;
    }

    public static void ExtractZip(string zipPath, string destination, bool stripRootFolder = false)
    {
        Directory.CreateDirectory(destination);
        using var archive = ZipFile.OpenRead(zipPath);

        foreach (var entry in archive.Entries)
        {
            if (string.IsNullOrEmpty(entry.Name))
            {
                continue;
            }

            string relative = entry.FullName.Replace('/', Path.DirectorySeparatorChar);
            if (stripRootFolder)
            {
                int index = relative.IndexOf(Path.DirectorySeparatorChar);
                if (index < 0)
                {
                    continue;
                }
                relative = relative[(index + 1)..];
            }

            string destinationPath = Path.Combine(destination, relative);
            Directory.CreateDirectory(Path.GetDirectoryName(destinationPath)!);
            entry.ExtractToFile(destinationPath, true);
        }
    }
}
