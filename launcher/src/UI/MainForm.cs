using System.Diagnostics;
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
    private readonly AccentProgress progressBar = new();

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
        if (string.IsNullOrWhiteSpace(config.ModProjectDir))
        {
            AppendLog("Папка мода не найдена — укажите её перед сборкой", LogKind.Warn);
        }
    }

    // ---------------------------------------------------------------- layout

    private void BuildUi()
    {
        background.Dock = DockStyle.Fill;
        Controls.Add(background);

        header.Bounds = new Rectangle(0, 0, WindowWidth, 84);
        background.Controls.Add(header);

        accountChip.Text = "аккаунт";
        accountChip.Bounds = new Rectangle(WindowWidth - 118 - 200, 25, 200, 32);
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

        // hero
        heroCaption.Text = "MINECRAFT";
        heroCaption.Bounds = new Rectangle(58, 146, 400, 18);
        heroCaption.ForeColor = Theme.Subtle;
        heroCaption.BackColor = Color.Transparent;
        heroCaption.Font = Theme.FontSmall;
        background.Controls.Add(heroCaption);

        heroVersion.Text = Meta.GameVersion;
        heroVersion.Bounds = new Rectangle(52, 164, 600, 52);
        heroVersion.ForeColor = Theme.Text;
        heroVersion.BackColor = Color.Transparent;
        heroVersion.Font = new Font("Segoe UI", 32f, FontStyle.Bold);
        background.Controls.Add(heroVersion);

        heroSubtitle.Text = $"Fabric {Meta.FabricLoader}   ·   Aurora Visuals   ·   Java 21";
        heroSubtitle.Bounds = new Rectangle(58, 222, 760, 20);
        heroSubtitle.ForeColor = Theme.Subtle;
        heroSubtitle.BackColor = Color.Transparent;
        heroSubtitle.Font = Theme.FontBody;
        background.Controls.Add(heroSubtitle);

        heroState.Text = "Проверяю установку...";
        heroState.Bounds = new Rectangle(58, 250, 940, 20);
        heroState.ForeColor = Color.FromArgb(205, Theme.Accent2);
        heroState.BackColor = Color.Transparent;
        heroState.Font = Theme.FontSmall;
        background.Controls.Add(heroState);

        // нижняя панель
        statusLabel.Bounds = new Rectangle(56, 516, 968, 18);
        statusLabel.Text = "Готово";
        statusLabel.ForeColor = Theme.Subtle;
        statusLabel.BackColor = Color.Transparent;
        statusLabel.Font = Theme.FontSmall;
        background.Controls.Add(statusLabel);

        progressBar.Bounds = new Rectangle(56, 538, 968, 6);
        background.Controls.Add(progressBar);

        Configure(accountsButton, "Аккаунты", 56, 556, 160, 44);
        accountsButton.Click += (_, _) => OpenAccounts();

        Configure(settingsButton, "Настройки", 228, 556, 160, 44);
        settingsButton.Click += (_, _) => OpenSettings();

        Configure(consoleButton, "Консоль", 400, 556, 132, 44);
        consoleButton.Click += (_, _) => OpenConsole();

        Configure(networkButton, "СЕТЬ", 544, 556, 96, 44);
        networkButton.Click += (_, _) => OpenNetwork();

        Configure(installButton, "УСТАНОВИТЬ", 850, 556, 174, 44);
        installButton.Click += async (_, _) => await RunInstallAsync();

        playButton.Text = "ИГРАТЬ";
        playButton.Bounds = new Rectangle(652, 550, 186, 54);
        playButton.Click += async (_, _) => await RunLaunchAsync();

        background.Controls.Add(accountsButton);
        background.Controls.Add(settingsButton);
        background.Controls.Add(consoleButton);
        background.Controls.Add(installButton);
        background.Controls.Add(playButton);
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
