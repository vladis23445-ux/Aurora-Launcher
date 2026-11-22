using System.Diagnostics;
using System.Reflection;
using AuroraLauncher.Core;

namespace AuroraLauncher.UI;

public sealed class MainForm : Form
{
    private const int WindowWidth = 1080;
    private const int WindowHeight = 640;

    private readonly AppConfig config;
    private readonly DiscordRpc rpc;

    private readonly AuroraBackground background = new();
    private readonly HeaderBar header = new();
    private readonly GhostButton accountChip = new();
    private readonly GlyphButton minimizeButton = new("–");
    private readonly GlyphButton closeButton = new("✕");

    private readonly Label heroCaption = new();
    private readonly Label heroVersion = new();
    private readonly Label heroSubtitle = new();
    private readonly Label heroState = new();
    private readonly Label statusLabel = new();
    private readonly CardPanel cardStatus = new();
    private readonly CardPanel cardPromo = new();
    private readonly CardPanel cardBuild = new();
    private readonly CardPanel cardStart = new();
    private readonly AccentProgress progressBar = new();
    private readonly Label footerLabel = new();

    private readonly GhostButton accountsButton = new();
    private readonly GhostButton settingsButton = new();
    private readonly GhostButton consoleButton = new();
    private readonly GhostButton networkButton = new();
    private readonly GhostButton installButton = new();
    private readonly AccentButton playButton = new();

    private ConsoleForm? consoleForm;
    private CancellationTokenSource? cts;
    private bool busy;
    private bool dragging;
    private Point dragOrigin;
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
        background.Dock = DockStyle.Fill;
        Controls.Add(background);

        header.Bounds = new Rectangle(0, 0, WindowWidth, 84);
        background.Controls.Add(header);

        accountChip.Text = "аккаунт";
        accountChip.Bounds = new Rectangle(WindowWidth / 2 - 105, 24, 210, 34);
        accountChip.Font = Theme.FontBold;
        accountChip.Click += (_, _) => OpenAccounts();
        header.Controls.Add(accountChip);

        minimizeButton.Bounds = new Rectangle(WindowWidth - 88, 26, 30, 30);
        minimizeButton.Clicked += (_, _) => WindowState = FormWindowState.Minimized;
        header.Controls.Add(minimizeButton);

        closeButton.Bounds = new Rectangle(WindowWidth - 52, 26, 30, 30);
        closeButton.Clicked += (_, _) => Close();
        header.Controls.Add(closeButton);

        header.MouseDown += (_, e) =>
        {
            if (e.Button == MouseButtons.Left)
            {
                dragging = true;
                dragOrigin = e.Location;
            }
        };
        header.MouseMove += (_, e) =>
        {
            if (dragging)
            {
                Location = new Point(Location.X + e.X - dragOrigin.X, Location.Y + e.Y - dragOrigin.Y);
            }
        };
        header.MouseUp += (_, _) => dragging = false;

        // левая колонка
        heroCaption.Text = $"MINECRAFT {Meta.GameVersion}";
        heroCaption.Bounds = new Rectangle(58, 112, 420, 18);
        heroCaption.ForeColor = Theme.Subtle;
        heroCaption.BackColor = Color.Transparent;
        heroCaption.Font = Theme.FontSmall;
        background.Controls.Add(heroCaption);

        heroVersion.Text = "Aurora Visuals";
        heroVersion.Bounds = new Rectangle(52, 130, 640, 58);
        heroVersion.ForeColor = Theme.Text;
        heroVersion.BackColor = Color.Transparent;
        heroVersion.Font = new Font("Segoe UI", 30f, FontStyle.Bold);
        background.Controls.Add(heroVersion);

        heroSubtitle.Text = $"Fabric {Meta.FabricLoader}   ·   Java 21   ·   визуальный клиент";
        heroSubtitle.Bounds = new Rectangle(58, 192, 700, 20);
        heroSubtitle.ForeColor = Theme.Subtle;
        heroSubtitle.BackColor = Color.Transparent;
        heroSubtitle.Font = Theme.FontBody;
        background.Controls.Add(heroSubtitle);

        playButton.Text = "ИГРАТЬ";
        playButton.Bounds = new Rectangle(58, 236, 420, 62);
        playButton.Click += async (_, _) => await RunLaunchAsync();
        background.Controls.Add(playButton);

        Configure(installButton, "УСТАНОВИТЬ", 494, 236, 132, 62);
        installButton.Click += async (_, _) => await RunInstallAsync();
        background.Controls.Add(installButton);

        heroState.Text = "Проверяю установку...";
        heroState.Bounds = new Rectangle(58, 312, 700, 20);
        heroState.ForeColor = Color.FromArgb(205, Theme.Accent2);
        heroState.BackColor = Color.Transparent;
        heroState.Font = Theme.FontSmall;
        background.Controls.Add(heroState);

        progressBar.Bounds = new Rectangle(58, 342, 700, 6);
        background.Controls.Add(progressBar);

        statusLabel.Bounds = new Rectangle(58, 358, 700, 18);
        statusLabel.Text = "Готово";
        statusLabel.ForeColor = Theme.Subtle;
        statusLabel.BackColor = Color.Transparent;
        statusLabel.Font = Theme.FontSmall;
        background.Controls.Add(statusLabel);

        Configure(accountsButton, "Аккаунты", 58, 394, 124, 38);
        accountsButton.Click += (_, _) => OpenAccounts();
        background.Controls.Add(accountsButton);

        Configure(settingsButton, "Настройки", 190, 394, 124, 38);
        settingsButton.Click += (_, _) => OpenSettings();
        background.Controls.Add(settingsButton);

        Configure(consoleButton, "Консоль", 322, 394, 110, 38);
        consoleButton.Click += (_, _) => OpenConsole();
        background.Controls.Add(consoleButton);

        Configure(networkButton, "СЕТЬ", 440, 394, 96, 38);
        networkButton.Click += (_, _) => OpenNetwork();
        background.Controls.Add(networkButton);

        // правая колонка — карточки
        cardStatus.Title = "Состояние";
        cardStatus.Body = "Проверяю...";
        cardStatus.Bounds = new Rectangle(786, 112, 236, 132);
        background.Controls.Add(cardStatus);

        cardPromo.Title = "Проект";
        cardPromo.Body = "Мод и лаунчер в одном setup.\r\n\r\nВышедшая версия — 1.1.0.\r\n\r\ngithub.com/vladis23445-ux/\r\nAurora-Launcher";
        cardPromo.Accent = Theme.Accent2;
        cardPromo.Bounds = new Rectangle(786, 258, 236, 166);
        background.Controls.Add(cardPromo);

        cardBuild.Title = "Сборка";
        cardBuild.Body = $"Лаунчер {LauncherVersion}\r\nMinecraft {Meta.GameVersion}\r\nFabric {Meta.FabricLoader} · Java 21\r\nRAM: {config.RamMb} МБ";
        cardBuild.Accent = Theme.Mint;
        cardBuild.Bounds = new Rectangle(786, 438, 236, 126);
        background.Controls.Add(cardBuild);

        // нижняя полоса — как начать
        cardStart.Title = "Как начать";
        cardStart.Body = "1 · Нажмите «УСТАНОВИТЬ» — скачаются Minecraft, Fabric и мод Aurora\r\n" +
                         "2 · Создайте аккаунт: вход не нужен, профиль хранится локально\r\n" +
                         "3 · «ИГРАТЬ» — мод запустится вместе с игрой, RSHIFT открывает меню";
        cardStart.Accent = Theme.Accent2;
        cardStart.Bounds = new Rectangle(58, 452, 700, 112);
        background.Controls.Add(cardStart);

        footerLabel.Text = "Aurora Visuals · мод и лаунчер в одном setup · github.com/vladis23445-ux/Aurora-Launcher";
        footerLabel.Bounds = new Rectangle(58, 578, 764, 16);
        footerLabel.ForeColor = Color.FromArgb(110, Theme.Text);
        footerLabel.BackColor = Color.Transparent;
        footerLabel.Font = Theme.FontSmall;
        background.Controls.Add(footerLabel);
    }

    private static string LauncherVersion
    {
        get
        {
            var version = Assembly.GetExecutingAssembly().GetName().Version;
            return version == null ? "1.1.0" : $"{version.Major}.{version.Minor}.{Math.Max(version.Build, 0)}";
        }
    }
    private static void Configure(Control control, string text, int x, int y, int w, int h)
    {
        control.Text = text;
        control.Bounds = new Rectangle(x, y, w, h);
    }

    // ---------------------------------------------------------------- state

    private void RefreshAccount()
    {
        current = config.Accounts.FirstOrDefault(a => a.Name == config.SelectedAccount)
                  ?? config.Accounts.FirstOrDefault();
        config.SelectedAccount = current?.Name;
        accountChip.Text = current == null ? "создать аккаунт" : current.Name;
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

        heroState.Text = installed
            ? $"Установлен профиль {config.VersionId}   ·   модов в папке: {mods}"
            : "Не установлено — нажмите «УСТАНОВИТЬ»";
        heroState.ForeColor = installed
            ? Color.FromArgb(205, Theme.Accent2)
            : Color.FromArgb(255, 255, 196, 92);

        cardStatus.Body = installed
            ? $"Профиль: Fabric {Meta.FabricLoader}\r\nВерсия: {Meta.GameVersion}\r\nМодов: {mods} · RAM: {config.RamMb} МБ\r\nОкно: {config.WindowWidth}×{config.WindowHeight}"
            : "Игра не установлена.\r\nНажмите «УСТАНОВИТЬ» — лаунчер скачает\r\nMinecraft, Fabric и мод.";

        cardBuild.Body = $"Лаунчер {LauncherVersion}\r\nMinecraft {Meta.GameVersion}\r\nFabric {Meta.FabricLoader} · Java 21\r\nRAM: {config.RamMb} МБ";
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

    private void SetBusy(bool value, string status)
    {
        busy = value;
        playButton.Enabled = !value;
        installButton.Enabled = !value;
        settingsButton.Enabled = !value;
        accountsButton.Enabled = !value;
        statusLabel.Text = status;
        if (!value)
        {
            progressBar.Value = 0;
        }
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
        var progress = new Progress<double>(p => progressBar.Value = (int)p);

        try
        {
            string java = await JavaManager.EnsureAsync(config, log, progress, cts.Token);
            config.JavaPath = java;
            config.Save();

            await Installer.InstallVanillaAsync(config, log, progress, cts.Token);
            await Installer.InstallFabricAsync(config, log, progress, cts.Token);
            await Installer.InstallModsAsync(config, java, log, progress, cts.Token);

            progressBar.Value = 100;
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
        var progress = new Progress<double>(p => progressBar.Value = (int)p);

        try
        {
            string java = await JavaManager.EnsureAsync(config, log, progress, cts.Token);
            config.JavaPath = java;
            SetBusy(true, "Запуск Minecraft...");

            var process = await GameLauncher.LaunchAsync(config, current, java, log, cts.Token);
            AppendLog("Процесс запущен, PID " + process.Id, LogKind.Ok);
            rpc.SetPlaying(true);
            SetBusy(false, "Игра запущена");
            progressBar.Value = 100;

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
            statusLabel.Text = text;
            statusLabel.ForeColor = color;
        }
    }

    protected override void OnFormClosing(FormClosingEventArgs e)
    {
        config.Save();
        rpc.Dispose();
        base.OnFormClosing(e);
    }
}
