using System.Drawing.Drawing2D;
using AuroraLauncher.Core;

namespace AuroraLauncher.UI;

/// <summary>Базовое окно лаунчера: без рамки, со скруглением, своей шапкой и перетаскиванием.</summary>
internal abstract class DialogBase : Form
{
    private bool dragging;
    private Point dragOrigin;

    protected DialogBase(string title, int width, int height)
    {
        Text = title;
        FormBorderStyle = FormBorderStyle.None;
        ClientSize = new Size(width, height);
        StartPosition = FormStartPosition.CenterParent;
        BackColor = Theme.Card;
        ForeColor = Theme.Text;
        Font = Theme.FontBody;
        ShowInTaskbar = false;
        DoubleBuffered = true;
        AutoScaleMode = AutoScaleMode.None;
        KeyPreview = true;
        Region = new Region(Theme.Rounded(new RectangleF(0, 0, width, height), 14f));

        var caption = new Label
        {
            Text = title.ToUpperInvariant(),
            Bounds = new Rectangle(22, 18, width - 80, 20),
            ForeColor = Theme.Text,
            BackColor = Color.Transparent,
            Font = Theme.FontBold,
        };
        Controls.Add(caption);

        var close = new GlyphButton("✕") { Bounds = new Rectangle(width - 46, 12, 30, 30) };
        close.Clicked += (_, _) => OnCloseRequested();
        Controls.Add(close);

        AttachDrag(this);
        AttachDrag(caption);
    }

    private void AttachDrag(Control control)
    {
        control.MouseDown += (_, e) =>
        {
            if (e.Button == MouseButtons.Left)
            {
                dragging = true;
                dragOrigin = e.Location;
            }
        };
        control.MouseMove += (_, e) =>
        {
            if (dragging)
            {
                Location = new Point(Location.X + e.X - dragOrigin.X, Location.Y + e.Y - dragOrigin.Y);
            }
        };
        control.MouseUp += (_, _) => dragging = false;
    }

    protected override void OnKeyDown(KeyEventArgs e)
    {
        if (e.KeyCode == Keys.Escape)
        {
            OnCloseRequested();
            e.Handled = true;
        }
        base.OnKeyDown(e);
    }

    protected virtual void OnCloseRequested() => Close();

    protected static Label Caption(string text, int x, int y) => new()
    {
        Text = text,
        Location = new Point(x, y),
        AutoSize = true,
        ForeColor = Theme.Subtle,
        BackColor = Color.Transparent,
        Font = Theme.FontSmall,
    };
}

/// <summary>Настройки в отдельном окне.</summary>
internal sealed class SettingsForm : DialogBase
{
    private readonly AppConfig config;

    private readonly NumericUpDown ramBox = new();
    private readonly NumericUpDown widthBox = new();
    private readonly NumericUpDown heightBox = new();
    private readonly TextBox javaBox = new();
    private readonly TextBox gameDirBox = new();
    private readonly TextBox jvmBox = new();
    private readonly CheckBox fullscreenBox = new();
    private readonly CheckBox closeOnLaunchBox = new();
    private readonly ComboBox versionBox = new();
    private readonly CheckBox discordRpcBox = new();
    private readonly TextBox discordIdBox = new();

    public SettingsForm(AppConfig config) : base("Настройки", 640, 600)
    {
        this.config = config;

        const int left = 24;

        Controls.Add(Caption("ПАМЯТЬ, МБ", left, 56));
        ramBox.Minimum = 1024;
        ramBox.Maximum = 32768;
        ramBox.Increment = 512;
        Controls.Add(new FieldPanel(ramBox, 180) { Location = new Point(left, 74) });

        Controls.Add(Caption("ОКНО", left, 116));
        widthBox.Minimum = 640;
        widthBox.Maximum = 3840;
        widthBox.Increment = 10;
        heightBox.Minimum = 480;
        heightBox.Maximum = 2160;
        heightBox.Increment = 10;
        Controls.Add(new FieldPanel(widthBox, 86) { Location = new Point(left, 134) });
        Controls.Add(new FieldPanel(heightBox, 86) { Location = new Point(left + 94, 134) });

        fullscreenBox.Text = "Полный экран";
        fullscreenBox.AutoSize = true;
        fullscreenBox.ForeColor = Theme.Subtle;
        fullscreenBox.FlatStyle = FlatStyle.Flat;
        fullscreenBox.BackColor = Color.Transparent;
        fullscreenBox.TabStop = false;
        fullscreenBox.Location = new Point(left, 174);
        Controls.Add(fullscreenBox);

        Controls.Add(Caption("ПАПКА ИГРЫ", left, 206));
        Controls.Add(new FieldPanel(gameDirBox, 540) { Location = new Point(left, 224) });
        var browseDir = new GhostButton { Text = "Обзор", Bounds = new Rectangle(left + 548, 224, 44, 30) };
        browseDir.Click += (_, _) => BrowseDir();
        Controls.Add(browseDir);

        Controls.Add(Caption("JAVA (AUTO — СКАЧАЕТ JDK 21)", left, 266));
        Controls.Add(new FieldPanel(javaBox, 540) { Location = new Point(left, 284) });
        var browseJava = new GhostButton { Text = "Обзор", Bounds = new Rectangle(left + 548, 284, 44, 30) };
        browseJava.Click += (_, _) => BrowseJava();
        Controls.Add(browseJava);

        Controls.Add(Caption("ВЕРСИЯ", left, 326));
        versionBox.DropDownStyle = ComboBoxStyle.DropDownList;
        versionBox.FlatStyle = FlatStyle.Flat;
        versionBox.BackColor = Theme.CardInner;
        versionBox.ForeColor = Theme.Text;
        Controls.Add(new FieldPanel(versionBox, 300) { Location = new Point(left, 344) });

        closeOnLaunchBox.Text = "Скрывать лаунчер при запуске";
        closeOnLaunchBox.AutoSize = true;
        closeOnLaunchBox.ForeColor = Theme.Subtle;
        closeOnLaunchBox.FlatStyle = FlatStyle.Flat;
        closeOnLaunchBox.BackColor = Color.Transparent;
        closeOnLaunchBox.TabStop = false;
        closeOnLaunchBox.Location = new Point(left, 388);
        Controls.Add(closeOnLaunchBox);

        Controls.Add(Caption("DISCORD RICH PRESENCE", left, 470));
        discordRpcBox.Text = "Показывать статус в Discord";
        discordRpcBox.AutoSize = true;
        discordRpcBox.ForeColor = Theme.Subtle;
        discordRpcBox.FlatStyle = FlatStyle.Flat;
        discordRpcBox.BackColor = Color.Transparent;
        discordRpcBox.TabStop = false;
        discordRpcBox.Location = new Point(left, 490);
        Controls.Add(discordRpcBox);

        discordIdBox.PlaceholderText = "Application ID приложения Discord";
        Controls.Add(new FieldPanel(discordIdBox, 300) { Location = new Point(left + 214, 486) });

        Controls.Add(Caption("ДОП. JVM АРГУМЕНТЫ", left, 414));
        Controls.Add(new FieldPanel(jvmBox, 592) { Location = new Point(left, 432) });

        var cancel = new GhostButton { Text = "Отмена", Bounds = new Rectangle(24, 546, 140, 34) };
        cancel.Click += (_, _) => Close();
        Controls.Add(cancel);

        var save = new AccentButton { Text = "Сохранить", Bounds = new Rectangle(412, 546, 204, 34) };
        save.Click += (_, _) =>
        {
            ApplyTo(config);
            config.Save();
            DialogResult = DialogResult.OK;
            Close();
        };
        Controls.Add(save);

        LoadFrom(config);
    }

    private void LoadFrom(AppConfig cfg)
    {
        ramBox.Value = Math.Clamp(cfg.RamMb, 1024, 32768);
        widthBox.Value = Math.Clamp(cfg.WindowWidth, 640, 3840);
        heightBox.Value = Math.Clamp(cfg.WindowHeight, 480, 2160);
        javaBox.Text = cfg.JavaPath;
        gameDirBox.Text = cfg.GameDir;
        jvmBox.Text = cfg.ExtraJvm;
        fullscreenBox.Checked = cfg.Fullscreen;
        closeOnLaunchBox.Checked = cfg.CloseOnLaunch;
        discordRpcBox.Checked = cfg.DiscordRpc;
        discordIdBox.Text = cfg.DiscordAppId;

        versionBox.Items.Clear();
        string root = Meta.VersionsDir(cfg);
        if (Directory.Exists(root))
        {
            foreach (var dir in Directory.GetDirectories(root))
            {
                string id = Path.GetFileName(dir);
                if (File.Exists(Path.Combine(dir, id + ".json")))
                {
                    versionBox.Items.Add(id);
                }
            }
        }
        string fabricId = $"fabric-loader-{Meta.FabricLoader}-{Meta.GameVersion}";
        if (!versionBox.Items.Contains(fabricId))
        {
            versionBox.Items.Add(fabricId);
        }
        int index = versionBox.Items.IndexOf(cfg.VersionId);
        versionBox.SelectedIndex = index >= 0 ? index : versionBox.Items.Count - 1;
    }

    private void ApplyTo(AppConfig cfg)
    {
        cfg.RamMb = (int)ramBox.Value;
        cfg.WindowWidth = (int)widthBox.Value;
        cfg.WindowHeight = (int)heightBox.Value;
        cfg.JavaPath = string.IsNullOrWhiteSpace(javaBox.Text) ? "auto" : javaBox.Text.Trim();
        if (!string.IsNullOrWhiteSpace(gameDirBox.Text))
        {
            cfg.GameDir = gameDirBox.Text.Trim();
            Directory.CreateDirectory(cfg.GameDir);
        }
        cfg.ExtraJvm = jvmBox.Text;
        cfg.Fullscreen = fullscreenBox.Checked;
        cfg.CloseOnLaunch = closeOnLaunchBox.Checked;
        cfg.DiscordRpc = discordRpcBox.Checked;
        cfg.DiscordAppId = discordIdBox.Text.Trim();
        if (versionBox.SelectedItem is string version)
        {
            cfg.VersionId = version;
        }
    }

    private void BrowseJava()
    {
        using var dialog = new OpenFileDialog { Title = "java.exe", Filter = "java.exe|java.exe|Все файлы|*.*" };
        if (dialog.ShowDialog(this) == DialogResult.OK)
        {
            javaBox.Text = dialog.FileName;
        }
    }

    private void BrowseDir()
    {
        using var dialog = new FolderBrowserDialog { Description = "Папка игры" };
        if (dialog.ShowDialog(this) == DialogResult.OK)
        {
            gameDirBox.Text = dialog.SelectedPath;
        }
    }
}

/// <summary>Аккаунты в отдельном окне.</summary>
internal sealed class AccountsForm : DialogBase
{
    private readonly AppConfig config;
    private readonly FlowLayoutPanel list = new();
    private Account? current;

    public AccountsForm(AppConfig config) : base("Аккаунты", 460, 420)
    {
        this.config = config;

        list.Bounds = new Rectangle(20, 56, 420, 286);
        list.BackColor = Color.Transparent;
        list.AutoScroll = true;
        list.WrapContents = false;
        list.FlowDirection = FlowDirection.TopDown;
        Controls.Add(list);

        var add = new AccentButton { Text = "Создать оффлайн", Bounds = new Rectangle(20, 356, 210, 38) };
        add.Click += (_, _) => Create();
        Controls.Add(add);

        var remove = new GhostButton { Text = "Удалить", Bounds = new Rectangle(242, 356, 130, 38) };
        remove.Click += (_, _) => Remove();
        Controls.Add(remove);

        Rebuild();
    }

    private void Rebuild()
    {
        list.SuspendLayout();
        foreach (Control control in list.Controls.Cast<Control>().ToList())
        {
            control.Dispose();
        }
        list.Controls.Clear();

        current = config.Accounts.FirstOrDefault(a => a.Name == config.SelectedAccount)
                  ?? config.Accounts.FirstOrDefault();

        if (config.Accounts.Count == 0)
        {
            list.Controls.Add(new Label
            {
                Text = "Пока нет аккаунтов.\r\nСоздайте оффлайн-ник — он попадёт в игру.",
                AutoSize = false,
                Width = 400,
                Height = 60,
                ForeColor = Theme.Subtle,
                BackColor = Color.Transparent,
                Font = Theme.FontSmall,
            });
        }

        foreach (var account in config.Accounts)
        {
            var row = new AccountRow(account) { Width = 400, IsSelected = current?.Name == account.Name };
            row.Chosen += (_, _) =>
            {
                config.SelectedAccount = account.Name;
                config.Save();
                Rebuild();
            };
            list.Controls.Add(row);
        }
        list.ResumeLayout();
    }

    private void Create()
    {
        string? name = Prompt.Show(this, "Новый оффлайн-аккаунт", "Ник (3–16 символов)");
        if (string.IsNullOrWhiteSpace(name))
        {
            return;
        }
        name = name.Trim();
        if (name.Length is < 3 or > 16 || !name.All(c => char.IsLetterOrDigit(c) || c == '_'))
        {
            MessageBox.Show(this, "Ник: 3–16 символов, буквы, цифры и _.", "Aurora Launcher",
                MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return;
        }
        if (config.Accounts.Any(a => a.Name.Equals(name, StringComparison.OrdinalIgnoreCase)))
        {
            return;
        }

        var account = new Account { Name = name, Uuid = AppConfig.OfflineUuid(name), Type = "offline" };
        config.Accounts.Add(account);
        config.SelectedAccount = account.Name;
        config.Save();
        Rebuild();
    }

    private void Remove()
    {
        if (current == null)
        {
            return;
        }
        config.Accounts.Remove(current);
        config.SelectedAccount = config.Accounts.FirstOrDefault()?.Name;
        config.Save();
        Rebuild();
    }
}

/// <summary>Консоль: отдельное немодальное окно.</summary>
internal sealed class ConsoleForm : DialogBase
{
    private readonly RichTextBox box = new();

    public ConsoleForm() : base("Консоль", 760, 460)
    {
        box.Bounds = new Rectangle(20, 56, 720, 384);
        box.BackColor = Color.FromArgb(10, 18, 15);
        box.ForeColor = Color.FromArgb(190, 205, 198);
        box.BorderStyle = BorderStyle.None;
        box.ReadOnly = true;
        box.Font = new Font("Consolas", 9f);
        box.WordWrap = false;
        box.DetectUrls = false;
        box.ScrollBars = RichTextBoxScrollBars.Vertical;
        Controls.Add(box);

        var clear = new GhostButton { Text = "Очистить", Bounds = new Rectangle(600, 14, 96, 28) };
        clear.Click += (_, _) => box.Clear();
        Controls.Add(clear);
    }

    protected override void OnCloseRequested()
    {
        Hide();
    }

    public void Append(string text, Color color)
    {
        if (InvokeRequired)
        {
            BeginInvoke(() => Append(text, color));
            return;
        }
        box.SelectionStart = box.TextLength;
        box.SelectionLength = 0;
        box.SelectionColor = Color.FromArgb(96, 112, 104);
        box.AppendText(DateTime.Now.ToString("HH:mm:ss") + "   ");
        box.SelectionColor = color;
        box.AppendText(text + Environment.NewLine);
        box.SelectionColor = box.ForeColor;
        box.ScrollToCaret();
    }
}

/// <summary>Диалог ввода в стиле лаунчера.</summary>
internal sealed class Prompt : Form
{
    private readonly TextBox input = new();

    private Prompt(string title, string question)
    {
        Text = title;
        ClientSize = new Size(380, 160);
        StartPosition = FormStartPosition.CenterParent;
        FormBorderStyle = FormBorderStyle.None;
        MaximizeBox = false;
        MinimizeBox = false;
        BackColor = Theme.Card;
        ForeColor = Theme.Text;
        Font = Theme.FontBody;
        ShowInTaskbar = false;
        DoubleBuffered = true;
        Region = new Region(Theme.Rounded(new RectangleF(0, 0, Width, Height), 12f));

        var caption = new Label
        {
            Text = title.ToUpperInvariant(),
            Bounds = new Rectangle(20, 16, 340, 18),
            ForeColor = Theme.Text,
            BackColor = Color.Transparent,
            Font = Theme.FontBold,
        };

        var label = new Label
        {
            Text = question,
            Bounds = new Rectangle(20, 40, 340, 18),
            ForeColor = Theme.Subtle,
            BackColor = Color.Transparent,
            Font = Theme.FontSmall,
        };

        var field = new FieldPanel(input, 340) { Location = new Point(20, 62) };

        var ok = new AccentButton
        {
            Text = "Создать",
            Bounds = new Rectangle(196, 112, 164, 34),
            DialogResult = DialogResult.OK,
        };

        var cancel = new GhostButton
        {
            Text = "Отмена",
            Bounds = new Rectangle(20, 112, 164, 34),
            DialogResult = DialogResult.Cancel,
        };

        Controls.AddRange(new Control[] { caption, label, field, ok, cancel });
        AcceptButton = ok;
        CancelButton = cancel;
        ActiveControl = input;
    }

    public static string? Show(IWin32Window owner, string title, string question)
    {
        using var form = new Prompt(title, question);
        return form.ShowDialog(owner) == DialogResult.OK ? form.input.Text.Trim() : null;
    }
}

/// <summary>Сетевая игра: адрес для друга, порт и инструкция.</summary>
internal sealed class NetworkForm : DialogBase
{
    private readonly TextBox portBox = new();
    private readonly Label addressLabel = new();

    public NetworkForm(AppConfig config) : base("Сетевая игра", 640, 400)
    {
        const int left = 24;

        string ip = LocalAddress();
        addressLabel.Text = ip;
        addressLabel.Bounds = new Rectangle(left, 78, 560, 26);
        addressLabel.ForeColor = Theme.Accent2;
        addressLabel.BackColor = Color.Transparent;
        addressLabel.Font = new Font("Consolas", 14f, FontStyle.Bold);

        Controls.Add(Caption("АДРЕС ЭТОГО КОМПЬЮТЕРА", left, 56));
        Controls.Add(addressLabel);

        Controls.Add(Caption("ПОРТ", left, 118));
        portBox.Text = "25565";
        Controls.Add(new FieldPanel(portBox, 120) { Location = new Point(left, 136) });

        var copy = new AccentButton { Text = "Скопировать адрес", Bounds = new Rectangle(left + 136, 136, 220, 32) };
        copy.Click += (_, _) =>
        {
            try
            {
                Clipboard.SetText(addressLabel.Text + ":" + portBox.Text.Trim());
            }
            catch
            {
            }
        };
        Controls.Add(copy);

        var hint = new Label
        {
            Bounds = new Rectangle(left, 186, 592, 190),
            ForeColor = Theme.Subtle,
            BackColor = Color.Transparent,
            Font = Theme.FontSmall,
            Text = "Как играть вдвоём:\r\n" +
                   "1. В игре: Esc → «Открыть для сети» → включите читы по желанию → порт 25565.\r\n" +
                   "2. Друг в лаунчере нажимает «СЕТЬ» и вводит ваш адрес в «Прямое подключение».\r\n" +
                   "3. Компьютеры должны быть в одной сети: дома это один Wi-Fi,\r\n" +
                   "   через интернет — любая виртуальная LAN (Radmin VPN, ZeroTier, Tailscale).\r\n\r\n" +
                   "Свой туннель без такой программы лаунчер поднять не может:\r\n" +
                   "для этого нужен постоянный сервер-посредник (relay) с внешним IP.",
        };
        Controls.Add(hint);
    }

    private static string LocalAddress()
    {
        try
        {
            foreach (var address in System.Net.Dns.GetHostAddresses(System.Net.Dns.GetHostName()))
            {
                if (address.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork &&
                    !System.Net.IPAddress.IsLoopback(address))
                {
                    return address.ToString();
                }
            }
        }
        catch
        {
        }
        return "127.0.0.1";
    }
}