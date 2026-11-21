using System.IO.Pipes;
using System.Text;
using System.Text.Json;

namespace AuroraLauncher.Core;

/// <summary>
/// Discord Rich Presence по локальному IPC-каналу.
/// Токен не нужен: достаточно Application ID приложения Discord.
/// Чтение и запись идут асинхронно, иначе они блокируют друг друга.
/// </summary>
public sealed class DiscordRpc : IDisposable
{
    private readonly CancellationTokenSource cts = new();
    private readonly Action<string>? log;

    private Thread? worker;
    private NamedPipeClientStream? pipe;      // чтение
    private NamedPipeClientStream? writer;    // запись (отдельное соединение)
    private volatile bool ready;
    private volatile bool playing;

    private string appId = "";
    private long startTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
    private DateTime lastSend = DateTime.MinValue;
    private string lastLogged = "";

    private readonly string logPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "AuroraLauncher", "discord-rpc.log");

    public DiscordRpc(Action<string>? log = null)
    {
        this.log = log;
    }

    public bool Enabled { get; set; }

    public void SetAppId(string id)
    {
        appId = (id ?? "").Trim();
    }

    public void SetPlaying(bool value)
    {
        playing = value;
        if (value)
        {
            startTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        }
        lastSend = DateTime.MinValue;

        if (ready)
        {
            _ = SendActivityAsync();
            lastSend = DateTime.UtcNow;
        }
    }

    public void Start()
    {
        if (worker != null)
        {
            return;
        }
        worker = new Thread(() => RunAsync().GetAwaiter().GetResult())
        {
            IsBackground = true,
            Name = "Aurora Discord RPC",
        };
        worker.Start();
    }

    private void WriteLog(string text)
    {
        log?.Invoke(text);
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(logPath)!);
            File.AppendAllText(logPath, DateTime.Now.ToString("HH:mm:ss") + "  " + text + Environment.NewLine);
        }
        catch
        {
        }
    }

    private async Task RunAsync()
    {
        bool warned = false;
        while (!cts.IsCancellationRequested)
        {
            try
            {
                if (!Enabled || string.IsNullOrWhiteSpace(appId))
                {
                    await Task.Delay(2000, cts.Token);
                    continue;
                }

                if (pipe == null || !pipe.IsConnected)
                {
                    bool ok = await ConnectAsync();
                    if (!ok && !warned)
                    {
                        WriteLog("Discord не найден — статус появится, когда клиент запущен");
                        warned = true;
                    }
                    if (!ok)
                    {
                        await Task.Delay(4000, cts.Token);
                        continue;
                    }
                }

                if (ready && (DateTime.UtcNow - lastSend).TotalSeconds >= 15)
                {
                    await SendActivityAsync();
                    lastSend = DateTime.UtcNow;
                }

                await Task.Delay(500, cts.Token);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                WriteLog("RPC: " + ex.GetType().Name + " " + ex.Message);
                ClosePipe();
                await Task.Delay(2000);
            }
        }
    }

    private async Task<bool> ConnectAsync()
    {
        ClosePipe();

        for (int i = 0; i < 10; i++)
        {
            var candidate = new NamedPipeClientStream(".", "discord-ipc-" + i, PipeDirection.InOut);
            try
            {
                await candidate.ConnectAsync(200, cts.Token);
                pipe = candidate;
                break;
            }
            catch
            {
                candidate.Dispose();
            }
        }

        if (pipe == null)
        {
            return false;
        }

        var local = pipe;
        await SendAsync(local, new
        {
            v = 1,
            client_id = appId,
        }, 0);

        _ = Task.Run(() => ReadLoopAsync(local));
        await OpenWriterAsync();

        DateTime deadline = DateTime.UtcNow.AddSeconds(5);
        while (!ready && DateTime.UtcNow < deadline && pipe == local)
        {
            await Task.Delay(50);
        }

        return pipe == local;
    }

    private async Task OpenWriterAsync()
    {
        try
        {
            writer?.Dispose();
        }
        catch
        {
        }
        writer = null;

        for (int i = 0; i < 10; i++)
        {
            var candidate = new NamedPipeClientStream(".", "discord-ipc-" + i, PipeDirection.Out);
            try
            {
                await candidate.ConnectAsync(200, cts.Token);
                writer = candidate;
                break;
            }
            catch
            {
                candidate.Dispose();
            }
        }

        if (writer == null)
        {
            WriteLog("не удалось открыть канал записи");
            return;
        }

        await SendAsync(writer, new
        {
            v = 1,
            client_id = appId,
        }, 0);
    }

    private async Task ReadLoopAsync(NamedPipeClientStream local)
    {
        byte[] header = new byte[8];
        try
        {
            while (!cts.IsCancellationRequested && local.IsConnected)
            {
                await ReadExactAsync(local, header, 8);
                int op = BitConverter.ToInt32(header, 0);
                int length = BitConverter.ToInt32(header, 4);
                if (length <= 0 || length > 1 << 20)
                {
                    break;
                }

                byte[] body = new byte[length];
                await ReadExactAsync(local, body, length);
                HandleFrame(op, Encoding.UTF8.GetString(body));
            }
        }
        catch
        {
            // соединение закрыто
        }

        ready = false;
        if (pipe == local)
        {
            ClosePipe();
        }
    }

    private void HandleFrame(int op, string json)
    {
        if (op == 2)
        {
            WriteLog("Discord закрыл соединение: " + Shorten(json));
            return;
        }

        try
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;

            if (root.TryGetProperty("evt", out var evt))
            {
                string? name = evt.GetString();
                if (name == "READY")
                {
                    ready = true;
                    WriteLog("Discord RPC подключён (ID " + appId + ")");
                }
                else if (name == "ERROR")
                {
                    WriteLog("Discord RPC ошибка: " + Shorten(json));
                }
            }
        }
        catch
        {
        }
    }

    private static string Shorten(string text) => text.Length > 160 ? text[..160] : text;

    private static async Task ReadExactAsync(Stream stream, byte[] buffer, int count)
    {
        int offset = 0;
        while (offset < count)
        {
            int read = await stream.ReadAsync(buffer.AsMemory(offset, count - offset));
            if (read <= 0)
            {
                throw new IOException("Discord закрыл канал");
            }
            offset += read;
        }
    }

    private Task SendActivityAsync()
    {
        var activity = new
        {
            details = playing ? "В игре" : "В лаунчере",
            state = playing ? "Minecraft 1.21.11 · Fabric" : "Aurora Visuals",
            timestamps = new { start = startTimestamp },
            assets = new
            {
                large_image = "logo",
                large_text = "Aurora Visuals",
            },
            instance = false,
        };

        string tag = playing ? "в игре" : "в лаунчере";
        if (tag != lastLogged)
        {
            lastLogged = tag;
            WriteLog("Discord: статус — " + tag);
        }

        var target = writer;
        if (target == null || !target.IsConnected)
        {
            return Task.CompletedTask;
        }

        return SendAsync(target, new
        {
            cmd = "SET_ACTIVITY",
            args = new
            {
                pid = Environment.ProcessId,
                activity,
            },
            nonce = Guid.NewGuid().ToString(),
        }, 1);
    }

    private async Task SendAsync(NamedPipeClientStream local, object payload, int op)
    {
        string json = JsonSerializer.Serialize(payload);
        byte[] body = Encoding.UTF8.GetBytes(json);
        byte[] frame = new byte[8 + body.Length];
        BitConverter.GetBytes(op).CopyTo(frame, 0);
        BitConverter.GetBytes(body.Length).CopyTo(frame, 4);
        body.CopyTo(frame, 8);

        WriteLog("send op=" + op + " bytes=" + frame.Length);
        await local.WriteAsync(frame);
        await local.FlushAsync();
        WriteLog("sent op=" + op);
    }

    private void ClosePipe()
    {
        ready = false;
        try
        {
            pipe?.Dispose();
        }
        catch
        {
        }
        try
        {
            writer?.Dispose();
        }
        catch
        {
        }
        pipe = null;
        writer = null;
    }

    public void Dispose()
    {
        try
        {
            cts.Cancel();
        }
        catch
        {
        }
        ClosePipe();
    }
}
