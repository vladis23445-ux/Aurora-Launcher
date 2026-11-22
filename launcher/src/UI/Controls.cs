using System.Drawing.Drawing2D;
using AuroraLauncher.Core;

namespace AuroraLauncher.UI;

/// <summary>Контейнер поля ввода: скруглённый фон, внутри borderless-контрол.</summary>
internal sealed class FieldPanel : Panel
{
    public Control Input { get; }

    public FieldPanel(Control input, int width, int height = 30)
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
        BackColor = Theme.CardInner;
        Size = new Size(width, height);

        Input = input;
        if (input is TextBox textBox)
        {
            textBox.BorderStyle = BorderStyle.None;
            textBox.BackColor = Theme.CardInner;
            textBox.ForeColor = Theme.Text;
        }
        else if (input is NumericUpDown numeric)
        {
            numeric.BackColor = Theme.CardInner;
            numeric.ForeColor = Theme.Text;
            numeric.BorderStyle = BorderStyle.None;
        }
        input.Font = Theme.FontBody;
        input.TabStop = false;

        int preferredHeight = input switch
        {
            TextBox tb => tb.PreferredHeight,
            UpDownBase upDown => upDown.PreferredHeight,
            _ => input.Height,
        };
        input.Location = new Point(10, Math.Max(2, (height - preferredHeight) / 2));
        input.Width = width - 20;
        Controls.Add(input);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);
        Theme.Fill(g, rect, 9f, Theme.CardInner);
        Theme.Border(g, rect, 9f, Theme.Stroke);
    }
}

/// <summary>Основная кнопка: градиент фиолетовый → розовый, подсветка и мягкая тень.</summary>
internal sealed class AccentButton : Button
{
    private bool hover;
    private float hoverAmount;
    private readonly System.Windows.Forms.Timer hoverTimer = new() { Interval = 15 };

    public AccentButton()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        FlatStyle = FlatStyle.Flat;
        FlatAppearance.BorderSize = 0;
        BackColor = Color.Transparent;
        ForeColor = Color.White;
        Font = new Font("Segoe UI", 10.5f, FontStyle.Bold);
        Cursor = Cursors.Hand;
        TabStop = false;
        hoverTimer.Tick += (_, _) => StepHover();
        MouseEnter += (_, _) => { hover = true; hoverTimer.Start(); };
        MouseLeave += (_, _) => { hover = false; hoverTimer.Start(); };
        EnabledChanged += (_, _) => Invalidate();
    }

    private void StepHover()
    {
        float target = hover ? 1f : 0f;
        hoverAmount += (target - hoverAmount) * 0.30f;
        if (Math.Abs(target - hoverAmount) < 0.01f)
        {
            hoverAmount = target;
            hoverTimer.Stop();
        }
        Invalidate();
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        var rect = new RectangleF(2f, 2f, Width - 4f, Height - 6f);

        if (Enabled)
        {
            Theme.Glow(g, new RectangleF(0, Height * 0.35f, Width, Height * 0.9f),
                Color.FromArgb((int)(46 + 44 * hoverAmount), Theme.Accent));
            Theme.FillGradient(g, rect, 14f,
                Theme.Blend(Theme.Accent, Color.White, 0.05f * hoverAmount),
                Theme.Blend(Theme.Accent2, Color.White, 0.08f * hoverAmount));

            using var gloss = new LinearGradientBrush(
                new RectangleF(0, rect.Y, Width, rect.Height * 0.62f),
                Color.FromArgb(48, 255, 255, 255), Color.FromArgb(0, 255, 255, 255),
                LinearGradientMode.Vertical);
            using (var path = Theme.Rounded(rect, 14f))
            {
                g.FillPath(gloss, path);
            }
            Theme.Border(g, rect, 14f, Color.FromArgb(70, 255, 255, 255));
        }
        else
        {
            Theme.Fill(g, rect, 14f, Color.FromArgb(38, 41, 62));
            Theme.Border(g, rect, 14f, Theme.Stroke);
        }

        TextRenderer.DrawText(g, Text, Font, new Rectangle(0, 0, Width, Height - 4),
            Enabled ? Color.White : Theme.Subtle,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }
}

/// <summary>Второстепенная кнопка: матовое стекло с тонкой рамкой.</summary>
internal sealed class GhostButton : Button
{
    private bool hover;
    private float hoverAmount;
    private readonly System.Windows.Forms.Timer hoverTimer = new() { Interval = 15 };

    public GhostButton()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        FlatStyle = FlatStyle.Flat;
        FlatAppearance.BorderSize = 0;
        BackColor = Color.Transparent;
        ForeColor = Theme.Text;
        Font = new Font("Segoe UI", 9f, FontStyle.Bold);
        Cursor = Cursors.Hand;
        TabStop = false;
        hoverTimer.Tick += (_, _) =>
        {
            float target = hover ? 1f : 0f;
            hoverAmount += (target - hoverAmount) * 0.30f;
            if (Math.Abs(target - hoverAmount) < 0.01f)
            {
                hoverAmount = target;
                hoverTimer.Stop();
            }
            Invalidate();
        };
        MouseEnter += (_, _) => { hover = true; hoverTimer.Start(); };
        MouseLeave += (_, _) => { hover = false; hoverTimer.Start(); };
        EnabledChanged += (_, _) => Invalidate();
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);

        int fillAlpha = (int)(16 + 26 * hoverAmount);
        Theme.Fill(g, rect, 11f, Color.FromArgb(Enabled ? fillAlpha : 10, 255, 255, 255));
        Theme.Border(g, rect, 11f,
            Color.FromArgb((int)(26 + 70 * hoverAmount), Theme.Accent2));

        TextRenderer.DrawText(g, Text, Font, new Rectangle(0, 0, Width, Height),
            Enabled ? Theme.Blend(Theme.Subtle, Color.White, hoverAmount) : Color.FromArgb(110, Theme.Subtle),
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }
}

/// <summary>Кнопка-глиф без фокуса (свернуть / закрыть).</summary>
internal sealed class GlyphButton : Control
{
    private bool hover;
    private float hoverAmount;
    private readonly System.Windows.Forms.Timer hoverTimer = new() { Interval = 15 };

    public event EventHandler? Clicked;

    public GlyphButton(string glyph)
    {
        Text = glyph;
        Size = new Size(30, 30);
        Cursor = Cursors.Hand;
        Font = new Font("Segoe UI", 9f, FontStyle.Bold);
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        BackColor = Color.Transparent;
        hoverTimer.Tick += (_, _) =>
        {
            float target = hover ? 1f : 0f;
            hoverAmount += (target - hoverAmount) * 0.35f;
            if (Math.Abs(target - hoverAmount) < 0.01f)
            {
                hoverAmount = target;
                hoverTimer.Stop();
            }
            Invalidate();
        };
    }

    protected override void OnMouseEnter(EventArgs e)
    {
        hover = true;
        hoverTimer.Start();
        base.OnMouseEnter(e);
    }

    protected override void OnMouseLeave(EventArgs e)
    {
        hover = false;
        hoverTimer.Start();
        base.OnMouseLeave(e);
    }

    protected override void OnMouseClick(MouseEventArgs e)
    {
        if (e.Button == MouseButtons.Left)
        {
            Clicked?.Invoke(this, EventArgs.Empty);
        }
        base.OnMouseClick(e);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);

        if (hoverAmount > 0.01f)
        {
            Theme.Fill(g, rect, 9f, Color.FromArgb((int)(46 * hoverAmount), 255, 255, 255));
        }

        Color color = hoverAmount > 0.4f ? Color.White : Theme.Subtle;
        TextRenderer.DrawText(g, Text, Font, new Rectangle(0, 0, Width, Height), color,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }
}

/// <summary>Тонкий индикатор прогресса с градиентом.</summary>
internal sealed class AccentProgress : Control
{
    private int value;

    public int Value
    {
        get => value;
        set
        {
            int clamped = Math.Clamp(value, 0, 100);
            if (clamped != this.value)
            {
                this.value = clamped;
                Invalidate();
            }
        }
    }

    public AccentProgress()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        BackColor = Color.Transparent;
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        var rect = new RectangleF(0, 0, Math.Max(1, Width), Math.Max(1, Height));
        float radius = rect.Height / 2f;

        Theme.Fill(g, rect, radius, Color.FromArgb(24, 255, 255, 255));
        float width = rect.Width * Math.Clamp(value, 0, 100) / 100f;
        if (width > 2f)
        {
            Theme.FillGradient(g, new RectangleF(0, 0, width, rect.Height), radius,
                Theme.Accent, Theme.Accent2);
        }
    }
}

/// <summary>Строка аккаунта: аватар с инициалом, ник, тип подключения.</summary>
internal sealed class AccountRow : Control
{
    private bool hover;

    public Account Account { get; }
    public bool IsSelected { get; set; }
    public event EventHandler? Chosen;

    public AccountRow(Account account)
    {
        Account = account;
        Height = 54;
        Width = 300;
        Cursor = Cursors.Hand;
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.SupportsTransparentBackColor |
                 ControlStyles.ResizeRedraw, true);
        BackColor = Color.Transparent;
        MouseEnter += (_, _) => { hover = true; Invalidate(); };
        MouseLeave += (_, _) => { hover = false; Invalidate(); };
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);

        if (IsSelected)
        {
            Theme.Fill(g, rect, 12f, Color.FromArgb(38, Theme.Accent));
            Theme.Border(g, rect, 12f, Color.FromArgb(140, Theme.Accent));
        }
        else if (hover)
        {
            Theme.Fill(g, rect, 12f, Theme.CardHover);
            Theme.Border(g, rect, 12f, Theme.Stroke);
        }
        else
        {
            Theme.Fill(g, rect, 12f, Theme.CardInner);
            Theme.Border(g, rect, 12f, Theme.Stroke);
        }

        var avatar = new RectangleF(10, 10, 34, 34);
        Theme.FillGradient(g, avatar, 11f, Theme.Accent, Theme.Accent2, LinearGradientMode.BackwardDiagonal);
        string initial = Account.Name.Length > 0 ? Account.Name[..1].ToUpperInvariant() : "?";
        using (var font = new Font("Segoe UI", 12f, FontStyle.Bold))
        {
            TextRenderer.DrawText(g, initial, font, Rectangle.Round(avatar), Color.White,
                TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
        }

        TextRenderer.DrawText(g, Account.Name, Theme.FontBold,
            new Rectangle(54, 12, Width - 88, 18), Theme.Text,
            TextFormatFlags.Left | TextFormatFlags.VerticalCenter);
        TextRenderer.DrawText(g, "offline · " + Theme.ShortUuid(Account.Uuid), Theme.FontSmall,
            new Rectangle(54, 29, Width - 88, 16), Theme.Subtle,
            TextFormatFlags.Left | TextFormatFlags.VerticalCenter);

        if (IsSelected)
        {
            using var pen = new Pen(Theme.Accent2, 2f);
            g.DrawEllipse(pen, Width - 26, Height / 2f - 4, 8, 8);
        }
    }

    protected override void OnClick(EventArgs e)
    {
        Chosen?.Invoke(this, EventArgs.Empty);
        base.OnClick(e);
    }
}

/// <summary>Стеклянная карточка с заголовком-капсом и текстом.</summary>
internal sealed class CardPanel : Control
{
    public string Title { get; set; } = "";
    public string Body { get; set; } = "";
    public Color Accent { get; set; } = Theme.Accent;

    public CardPanel()
    {
        SetStyle(ControlStyles.SupportsTransparentBackColor | ControlStyles.UserPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
        BackColor = Color.Transparent;
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;

        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);
        Theme.Fill(g, rect, 15f, Color.FromArgb(228, 20, 22, 36));
        Theme.Border(g, rect, 15f, Color.FromArgb(30, 255, 255, 255));

        // светлая кромка сверху
        using (var top = new LinearGradientBrush(
                   new RectangleF(rect.X, rect.Y, rect.Width, 26f),
                   Color.FromArgb(26, 255, 255, 255), Color.FromArgb(0, 255, 255, 255),
                   LinearGradientMode.Vertical))
        using (var path = Theme.Rounded(rect, 15f))
        {
            g.FillPath(top, path);
        }

        using (var dot = new SolidBrush(Accent))
        {
            g.FillEllipse(dot, 16, 21, 5, 5);
        }
        TextRenderer.DrawText(g, Spaced(Title.ToUpperInvariant()), Theme.FontCap,
            new Rectangle(28, 16, Width - 44, 16), Color.FromArgb(205, Theme.Subtle),
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        TextRenderer.DrawText(g, Body, Theme.FontBody,
            new Rectangle(16, 40, Width - 32, Height - 52), Theme.Subtle,
            TextFormatFlags.Left | TextFormatFlags.WordBreak | TextFormatFlags.NoPadding);
    }

    private static string Spaced(string text) => string.Join(" ", text.ToCharArray());
}