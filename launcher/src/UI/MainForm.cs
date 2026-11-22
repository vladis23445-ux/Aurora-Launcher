using System.Reflection;
using AuroraLauncher.Core;

namespace AuroraLauncher.UI;

public sealed class MainForm : Form
{
    private const int WindowWidth = 1080;
    private const int WindowHeight = 640;

    private readonly AppConfig config;
    private readonly DiscordRpc rpc;
    private readonly AuroraSurface surface = new();

    private ConsoleForm? consoleForm;
    private CancellationTokenSource? cts;
    private bool busy;
    private Account? current;

    public MainForm()
    {
        config = AppConfig.Load();
        rpc = new DiscordRpc(text => AppendLog(text));
        BundledMod.Install(config, text => AppendLog(text));
        if (string.IsNullOrWhiteSpace(config.ModProjectDir))
        {
            AppendLog("Мод Aurora установлен из лаунчера (исходники не требуются)");
        }

        Text = "Aurora Launcher";
        FormBorderStyle = FormBorderStyle.None;
        ClientSize = new Size(WindowWidth, WindowHeight);
        StartPosition = FormStartPosition.CenterScreen;
        BackColor = Theme.Bg;
        ForeColor = Theme.Text;
        Font = Theme.FontBody;
        try
        {
            Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath);
        }
        catch
        {
            // иконка не критична
        }
        AutoScaleMode = AutoScaleMode.None;
        DoubleBuffered = true;
        Region = new Region(Theme.Rounded(new RectangleF(0, 0, Width, Height), 16f));

        BuildUi();
        RefreshAccount();
        RefreshHeroState();

        rpc.SetAppId(config.DiscordAppId);
        rpc.Enabled = config.DiscordRpc;
        rpc.SetPlaying(false);
        rpc.Start();

        AppendLog("Aurora Launcher готов");
        AppendLog($"Minecraft {Meta.GameVersion} · Fabric {Meta.FabricLoader} · Java 21");
    }

    // ---------------------------------------------------------------- layout

    private void BuildUi()
    {
        surface.Dock = DockStyle.Fill;
        surface.Caption = $"MINECRAFT {Meta.GameVersion}";
        surface.Title = "Aurora Visuals";
        surface.Subtitle = $"Fabric {Meta.FabricLoader}   ·   Java 21   ·   визуальный клиент";
        surface.Footer = "Aurora Visuals  ·  мод и лаунчер в одном setup  ·  github.com/vladis23445-ux/Aurora-Launcher";
        surface.CardPromoBody = "Мод и лаунчер в одном setup.\r\nВерсия 1.1.0\r\n\r\ngithub.com/vladis23445-ux/Aurora-Launcher";
        surface.CardStartBody = "1 · «УСТАНОВИТЬ» — Minecraft, Fabric и мод\r\n2 · Аккаунт локальный, вход не нужен\r\n3 · «ИГРАТЬ», меню мода — RSHIFT";
        surface.BuildId = config.Build;
        surface.BuildSummary = Builds.Get(config.Build).Summary;
        surface.Action += OnSurfaceAction;
        Controls.Add(surface);
    }

    private async void OnSurfaceAction(object? sender, string id)
    {
        switch (id)
        {
            case "play":
                await RunLaunchAsync();
                break;
            case "install":
                await RunInstallAsync();
                break;
            case "accounts":
                OpenAccounts();
                break;
            case "settings":
                OpenSettings();
                break;
            case "console":
                OpenConsole();
                break;
            case "network":
                OpenNetwork();
                break;
            case "build-standard":
                SetBuild(Builds.StandardId);
                break;
            case "build-optimized":
                SetBuild(Builds.OptimizedId);
                break;
            case "chip":
                OpenAccounts();
                break;
            case "min":
                WindowState = FormWindowState.Minimized;
                break;
            case "close":
                Close();
                break;
        }
    }

    private static string LauncherVersion
    {
        get
        {
            var version = Assembly.GetExecutingAssembly().GetName().Version;
            return version == null ? "1.1.0" : $"{version.Major}.{version.Minor}.{Math.Max(version.Build, 0)}";
        }
    }

    // ---------------------------------------------------------------- state

    private void RefreshAccount()
    {
        current = config.Accounts.FirstOrDefault(a => a.Name == config.SelectedAccount)
                  ?? config.Accounts.FirstOrDefault();
        config.SelectedAccount = current?.Name;
        surface.AccountName = current == null ? "создать аккаунт" : current.Name;
        surface.Invalidate();
    }

    private void RefreshHeroState()
    {
        bool installed = Meta.TryLoadVersion(config, config.VersionId) != null;
        int mods = 0;
        string modsDir = Meta.ModsDir(config);
        if (Directory.Exists(modsDir))
        {
            mods = Directory.GetFiles(modsDir, "*.jar").Length;
        }

        surface.StateOk = installed;
        surface.StateText = installed
            ? $"Профиль {Meta.GameVersion} · Fabric {Meta.FabricLoader} · модов: {mods}"
            : "Не установлено — нажмите «УСТАНОВИТЬ»";

        surface.CardStatusBody = installed
            ? $"Профиль: Fabric {Meta.FabricLoader}\r\nВерсия: {Meta.GameVersion}\r\nМодов: {mods} · RAM: {config.RamMb} МБ\r\nОкно: {config.WindowWidth}×{config.WindowHeight}"
            : "Игра не установлена.\r\nНажмите «УСТАНОВИТЬ» — лаунчер скачает\r\nMinecraft, Fabric и мод.";

        surface.CardBuildBody = $"Сборка: {Builds.Get(config.Build).Name}\r\nMinecraft {Meta.GameVersion} · Fabric {Meta.FabricLoader}\r\nЛаунчер {LauncherVersion} · Java 21\r\nRAM: {config.RamMb} МБ";
        surface.BuildId = config.Build;
        surface.BuildSummary = Builds.Get(config.Build).Summary;
        surface.Invalidate();
    }

    private void SetBuild(string id)
    {
        if (string.Equals(config.Build, id, StringComparison.OrdinalIgnoreCase))
        {
            return;
        }

        config.Build = id;
        config.Save();
        var build = Builds.Get(id);
        surface.BuildId = build.Id;
        surface.BuildSummary = build.Summary;
        RefreshHeroState();
        SetStatus("Сборка: " + build.Name, Theme.Mint);
        AppendLog("Выбрана сборка: " + build.Name, LogKind.Ok);
        if (build.TuneGameOptions)
        {
            AppendLog("Перед запуском игры применится пресет графики (резервная копия: options.txt.before-optimization)");
        }
    }

    private void OpenSettings()
    {
        using var form = new SettingsForm(config);
        form.ShowDialog(this);
        RefreshHeroState();
    }

    private void OpenAccounts()
    {
        using var form = new AccountsForm(config);
        form.ShowDialog(this);
        RefreshAccount();
    }

    private void OpenNetwork()
    {
        using var dialog = new NetworkForm(config);
        dialog.ShowDialog(this);
    }

    private void OpenConsole()
    {
        if (consoleForm == null || consoleForm.IsDisposed)
        {
            consoleForm = new ConsoleForm();
        }
        consoleForm.Show(this);
        consoleForm.BringToFront();
    }

    // ---------------------------------------------------------------- actions

    private void SetProgress(int value)
    {
        surface.Progress = Math.Clamp(value, 0, 100);
        surface.Invalidate();
    }

    private void SetStatus(string text, Color color)
    {
        surface.StatusText = text;
        surface.StatusColor = color;
        surface.Invalidate();
    }

    private void SetBusy(bool value, string status)
    {
        busy = value;
        surface.Busy = value;
        surface.RefreshState();
        SetProgress(value ? surface.Progress : 0);
        SetStatus(status, Theme.Subtle);
        UseWaitCursor = value;
    }

    private async Task RunInstallAsync()
    {
        if (busy)
        {
            return;
        }

        OpenConsole();
        cts = new CancellationTokenSource();
        SetBusy(true, "Установка...");

        var log = new Progress<string>(text => AppendLog(text));
        var progress = new Progress<double>(p => SetProgress((int)p));

        try
        {
            string java = await JavaManager.EnsureAsync(config, log, progress, cts.Token);
            config.JavaPath = java;
            config.Save();

            await Installer.InstallVanillaAsync(config, log, progress, cts.Token);
            await Installer.InstallFabricAsync(config, log, progress, cts.Token);
            await Installer.InstallModsAsync(config, java, log, progress, cts.Token);

            SetProgress(100);
            SetBusy(false, "Установлено — можно играть");
            RefreshHeroState();
            AppendLog("=== УСТАНОВКА ЗАВЕРШЕНА ===", LogKind.Ok);
            AppendLog("Моды: " + Meta.ModsDir(config));
        }
        catch (OperationCanceledException)
        {
            SetBusy(false, "Отменено");
            AppendLog("Установка отменена", LogKind.Warn);
        }
        catch (Exception e)
        {
            SetBusy(false, "Ошибка установки");
            AppendLog("ОШИБКА: " + e.Message, LogKind.Error);
            MessageBox.Show(this, e.Message, "Ошибка установки", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    private async Task RunLaunchAsync()
    {
        if (busy)
        {
            return;
        }

        RefreshAccount();
        if (current == null)
        {
            OpenAccounts();
            RefreshAccount();
            if (current == null)
            {
                return;
            }
        }

        if (Meta.TryLoadVersion(config, config.VersionId) == null)
        {
            var answer = MessageBox.Show(this,
                "Профиль не установлен. Выполнить установку сейчас?",
                "Aurora Launcher", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
            if (answer == DialogResult.Yes)
            {
                await RunInstallAsync();
            }
            if (Meta.TryLoadVersion(config, config.VersionId) == null)
            {
                return;
            }
        }

        cts = new CancellationTokenSource();
        SetBusy(true, "Подготовка Java...");
        var log = new Progress<string>(text => AppendLog(text));
        var progress = new Progress<double>(p => SetProgress((int)p));

        try
        {
            string java = await JavaManager.EnsureAsync(config, log, progress, cts.Token);
            config.JavaPath = java;
            SetBusy(true, "Запуск Minecraft...");

            var process = await GameLauncher.LaunchAsync(config, current, java, log, cts.Token);
            AppendLog("Процесс запущен, PID " + process.Id, LogKind.Ok);
            rpc.SetPlaying(true);
            SetBusy(false, "Игра запущена");
            SetProgress(100);

            if (config.CloseOnLaunch)
            {
                Hide();
            }

            _ = Task.Run(async () =>
            {
                try
                {
                    await process.WaitForExitAsync();
                    BeginInvoke(() =>
                    {
                        AppendLog("Игра закрыта (код " + process.ExitCode + ")");
                        rpc.SetPlaying(false);
                        Show();
                        WindowState = FormWindowState.Normal;
                        Activate();
                        SetBusy(false, "Готово");
                    });
                }
                catch
                {
                    // процесс уже завершён
                }
            });
        }
        catch (Exception e)
        {
            SetBusy(false, "Ошибка запуска");
            AppendLog("ОШИБКА ЗАПУСКА: " + e.Message, LogKind.Error);
            MessageBox.Show(this, e.Message, "Ошибка запуска", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    // ---------------------------------------------------------------- logging

    private enum LogKind
    {
        Info,
        Ok,
        Warn,
        Error,
    }

    private void AppendLog(string text, LogKind kind = LogKind.Info)
    {
        if (InvokeRequired)
        {
            BeginInvoke(() => AppendLog(text, kind));
            return;
        }

        Color color = kind switch
        {
            LogKind.Ok => Theme.Good,
            LogKind.Warn => Color.FromArgb(255, 198, 96),
            LogKind.Error => Theme.Danger,
            _ => Color.FromArgb(196, 206, 200),
        };

        if (consoleForm != null && !consoleForm.IsDisposed)
        {
            consoleForm.Append(text, color);
        }

        if (kind is LogKind.Warn or LogKind.Error)
        {
            SetStatus(text, color);
        }
    }

    protected override void OnFormClosing(FormClosingEventArgs e)
    {
        config.Save();
        rpc.Dispose();
        base.OnFormClosing(e);
    }
}
