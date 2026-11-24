using System.Drawing.Drawing2D;
using AuroraLauncher.Core;

namespace AuroraLauncher.UI;

/// <summary>Магазин косметики и список заданий.</summary>
internal sealed class CosmeticsForm : DialogBase
{
    private readonly Label coinsLabel = new();
    private readonly Label hintLabel = new();
    private readonly List<ShopRow> rows = new();

    private ProfileData data;

    public CosmeticsForm(ProfileData profile) : base("Косметика", 820, 620)
    {
        data = profile;

        coinsLabel.Bounds = new Rectangle(486, 14, 262, 22);
        coinsLabel.TextAlign = ContentAlignment.MiddleRight;
        coinsLabel.ForeColor = Color.FromArgb(255, 232, 168);
        coinsLabel.BackColor = Color.Transparent;
        coinsLabel.Font = Theme.FontBold;
        Controls.Add(coinsLabel);

        Controls.Add(Section("ПЛАЩИ", 24, 52));
        Controls.Add(Section("КОРОНЫ", 424, 52));

        int index = 0;
        foreach (var cape in Cosmetics.ByType(CosmeticType.Cape))
        {
            AddRow(cape, 24, 74 + index * 48);
            index++;
        }

        index = 0;
        foreach (var crown in Cosmetics.ByType(CosmeticType.Crown))
        {
            AddRow(crown, 424, 74 + index * 48);
            index++;
        }

        hintLabel.Bounds = new Rectangle(24, 316, 772, 18);
        hintLabel.ForeColor = Theme.Subtle;
        hintLabel.BackColor = Color.Transparent;
        hintLabel.Font = Theme.FontSmall;
        Controls.Add(hintLabel);

        Controls.Add(Section("ЗАДАНИЯ В ИГРЕ", 24, 342));

        int questIndex = 0;
        foreach (var quest in Quests.All)
        {
            int column = questIndex < 7 ? 0 : 1;
            int row = questIndex < 7 ? questIndex : questIndex - 7;
            var questRow = new QuestRow(quest)
            {
                Bounds = new Rectangle(24 + column * 400, 362 + row * 34, 372, 32),
            };
            Controls.Add(questRow);
            questIndex++;
        }

        RefreshAll();
    }

    private void AddRow(Cosmetic cosmetic, int x, int y)
    {
        var row = new ShopRow(cosmetic) { Bounds = new Rectangle(x, y, 372, 42) };
        row.Clicked += (_, _) => HandleClick(cosmetic);
        rows.Add(row);
        Controls.Add(row);
    }

    private void HandleClick(Cosmetic cosmetic)
    {
        data = Profile.Load();

        if (!data.Owns(cosmetic.Id))
        {
            if (data.Coins < cosmetic.Price)
            {
                hintLabel.ForeColor = Theme.Danger;
                hintLabel.Text = $"Не хватает {cosmetic.Price - data.Coins} монет — выполните задания в игре";
                RefreshAll();
                return;
            }
            Profile.Buy(data, cosmetic);
            hintLabel.ForeColor = Theme.Good;
            hintLabel.Text = $"Куплено и надето: {cosmetic.Name}";
        }
        else
        {
            Profile.ToggleEquip(data, cosmetic);
            bool on = data.EquippedOf(cosmetic.Key) == cosmetic.Id;
            hintLabel.ForeColor = on ? Theme.Mint : Theme.Subtle;
            hintLabel.Text = on
                ? $"{cosmetic.Name}: надето — выйдите в мир, чтобы увидеть"
                : $"{cosmetic.Name}: снято";
        }

        data = Profile.Load();
        RefreshAll();
    }

    private void RefreshAll()
    {
        coinsLabel.Text = $"◈ {data.Coins} монет";
        foreach (var row in rows)
        {
            row.Apply(data);
        }
        if (string.IsNullOrEmpty(hintLabel.Text))
        {
            hintLabel.ForeColor = Theme.Subtle;
            var next = Quests.NextQuest(data);
            hintLabel.Text = next == null
                ? "Все задания выполнены — отличная работа"
                : $"Подсказка: ближайшее задание «{next.Title}» даст {next.Reward} монет";
        }
    }

    private static Label Section(string text, int x, int y) => new()
    {
        Text = text,
        Location = new Point(x, y),
        AutoSize = true,
        ForeColor = Color.FromArgb(170, Theme.Subtle),
        BackColor = Color.Transparent,
        Font = Theme.FontCap,
    };
}

/// <summary>Строка магазина: образец цвета, название, цена и состояние.</summary>
internal sealed class ShopRow : Control
{
    private readonly Cosmetic cosmetic;
    private bool hover;
    private bool owned;
    private bool equipped;

    public event EventHandler? Clicked;

    public ShopRow(Cosmetic cosmetic)
    {
        this.cosmetic = cosmetic;
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        BackColor = Color.Transparent;
        Cursor = Cursors.Hand;
        MouseEnter += (_, _) => { hover = true; Invalidate(); };
        MouseLeave += (_, _) => { hover = false; Invalidate(); };
    }

    public void Apply(ProfileData data)
    {
        owned = data.Owns(cosmetic.Id);
        equipped = data.EquippedOf(cosmetic.Key) == cosmetic.Id;
        Invalidate();
    }

    protected override void OnClick(EventArgs e)
    {
        Clicked?.Invoke(this, EventArgs.Empty);
        base.OnClick(e);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;

        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);
        Theme.Fill(g, rect, 12f, Color.FromArgb(hover ? 40 : 20, 255, 255, 255));
        Theme.Border(g, rect, 12f,
            equipped ? Color.FromArgb(190, Theme.Accent2) : Color.FromArgb(hover ? 90 : 34, Theme.Accent2));

        var swatch = new RectangleF(9, 8, 26, 26);
        Theme.FillGradient(g, swatch, 8f, cosmetic.Top, cosmetic.Bottom, LinearGradientMode.ForwardDiagonal);
        Theme.Border(g, swatch, 8f, Color.FromArgb(90, Color.White));

        TextRenderer.DrawText(g, cosmetic.Name, Theme.FontBold,
            new Rectangle(46, 6, Width - 210, 18), Theme.Text,
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        string subtitle = cosmetic.IsFree
            ? $"{cosmetic.TypeName} · бесплатно"
            : $"{cosmetic.TypeName} · {cosmetic.Price} монет";
        TextRenderer.DrawText(g, subtitle, Theme.FontSmall,
            new Rectangle(46, 22, Width - 210, 16), Theme.Subtle,
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        string state;
        Color stateColor;
        if (equipped)
        {
            state = "НАДЕТО · снять";
            stateColor = Theme.Mint;
        }
        else if (owned)
        {
            state = "Надеть";
            stateColor = Theme.Text;
        }
        else
        {
            state = cosmetic.IsFree ? "Забрать" : "Купить";
            stateColor = Color.FromArgb(255, 232, 168);
        }

        TextRenderer.DrawText(g, state, Theme.FontBold,
            new Rectangle(Width - 160, 0, 150, Height), stateColor,
            TextFormatFlags.Right | TextFormatFlags.VerticalCenter | TextFormatFlags.NoPadding);
    }
}

/// <summary>Строка задания: название, прогресс и награда.</summary>
internal sealed class QuestRow : Control
{
    private readonly Quest quest;
    private readonly ProfileData data;
    private readonly double fraction;

    public QuestRow(Quest quest, ProfileData data)
    {
        this.quest = quest;
        this.data = data;
        fraction = Quests.Fraction(data, quest);
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        BackColor = Color.Transparent;
    }

    public QuestRow(Quest quest) : this(quest, Profile.Load())
    {
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;

        bool done = data.IsClaimed(quest.Id);
        Color accent = done ? Theme.Good : Theme.Accent;

        TextRenderer.DrawText(g, quest.Title, Theme.FontBold,
            new Rectangle(0, 0, 240, 14), done ? Theme.Mint : Theme.Text,
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        TextRenderer.DrawText(g, "+" + quest.Reward, Theme.FontSmall,
            new Rectangle(302, 0, 68, 14), Color.FromArgb(255, 224, 150),
            TextFormatFlags.Right | TextFormatFlags.NoPadding);

        TextRenderer.DrawText(g, quest.Desc, Theme.FontSmall,
            new Rectangle(0, 14, 240, 14), Color.FromArgb(155, Theme.Subtle),
            TextFormatFlags.Left | TextFormatFlags.NoPadding | TextFormatFlags.EndEllipsis);

        string progress = done ? "выполнено" : Quests.FormatProgress(quest, Quests.Progress(data, quest));
        TextRenderer.DrawText(g, progress, Theme.FontSmall,
            new Rectangle(246, 14, 124, 14), done ? Theme.Mint : Theme.Subtle,
            TextFormatFlags.Right | TextFormatFlags.NoPadding);

        var bar = new RectangleF(0, 29, Width - 2, 3);
        Theme.Fill(g, bar, 1.5f, Color.FromArgb(22, 255, 255, 255));
        if (fraction > 0.01)
        {
            Theme.Fill(g, new RectangleF(bar.X, bar.Y, (float)(bar.Width * fraction), bar.Height), 1.5f, accent);
        }
    }
}
