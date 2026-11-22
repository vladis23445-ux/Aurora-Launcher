using System.Drawing.Drawing2D;
using AuroraLauncher.Core;

namespace AuroraLauncher.UI;

/// <summary>Фон приложения: глубокий зелёный градиент + живые полотна северного сияния.</summary>
internal sealed class AuroraBackground : Panel
{
    private readonly System.Windows.Forms.Timer timer;

    public AuroraBackground()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
        BackColor = Theme.Bg;
        timer = new System.Windows.Forms.Timer { Interval = 45 };
        timer.Tick += (_, _) => Invalidate();
        timer.Disposed += (_, _) => timer.Stop();
        timer.Start();
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;

        using (var brush = new LinearGradientBrush(
                   new Rectangle(0, 0, Math.Max(1, Width), Math.Max(1, Height)),
                   Theme.BgDeep, Theme.Bg, LinearGradientMode.Vertical))
        {
            g.FillRectangle(brush, ClientRectangle);
        }

        double t = Environment.TickCount64 / 1000.0;
        float h = Math.Max(1, Height);
        float w = Math.Max(1, Width);

        Theme.Glow(g, new RectangleF(w * 0.10f - 260, -h * 0.35f, w * 0.75f, h * 0.95f),
            Color.FromArgb(52, Theme.Accent));
        Theme.Glow(g, new RectangleF(w * 0.70f - 180, -h * 0.22f, w * 0.6f, h * 0.8f),
            Color.FromArgb(34, Theme.Accent2));

        Theme.Curtain(g, Width, Height, t, 0.0, 0.34, h * 0.20f, h * 0.035f, 0.0042,
            h * 0.30f, Color.FromArgb(96, Theme.Accent));
        Theme.Curtain(g, Width, Height, t, 1.9, 0.27, h * 0.31f, h * 0.045f, 0.0031,
            h * 0.24f, Color.FromArgb(74, Theme.Accent2));
        Theme.Curtain(g, Width, Height, t, 3.4, 0.41, h * 0.13f, h * 0.028f, 0.0055,
            h * 0.20f, Color.FromArgb(58, Theme.Mint));
        Theme.Curtain(g, Width, Height, t, 5.1, 0.22, h * 0.44f, h * 0.05f, 0.0024,
            h * 0.18f, Color.FromArgb(40, Theme.Deep));

        using var vignette = new LinearGradientBrush(
            new Rectangle(0, 0, Math.Max(1, Width), Math.Max(1, Height)),
            Color.FromArgb(0, 0, 0, 0), Color.FromArgb(150, 0, 0, 0), LinearGradientMode.Vertical);
        g.FillRectangle(vignette, ClientRectangle);
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            timer.Stop();
            timer.Dispose();
        }
        base.Dispose(disposing);
    }
}

/// <summary>Шапка: логотип-марка и градиентный заголовок.</summary>
internal sealed class HeaderBar : Panel
{
    public HeaderBar()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer, true);
        BackColor = Theme.Bg;
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;

        var mark = new RectangleF(28, 24, 34, 34);
        Theme.FillGradient(g, mark, 10f, Theme.Accent, Theme.Accent2, LinearGradientMode.BackwardDiagonal);
        Theme.Fill(g, new RectangleF(mark.X + 9, mark.Y + 9, 16, 16), 8f, Color.FromArgb(70, 255, 255, 255));
        using (var pen = new Pen(Color.FromArgb(60, 255, 255, 255), 1f))
        {
            g.DrawEllipse(pen, mark.X + 9, mark.Y + 9, 16, 16);
        }

        var titleRect = new RectangleF(76, 22, 240, 26);
        using (var titleBrush = new LinearGradientBrush(titleRect, Theme.Text, Theme.Accent2,
                   LinearGradientMode.Horizontal))
        {
            g.DrawString("AURORA", Theme.FontTitle, titleBrush, titleRect.X, titleRect.Y);
        }
        using (var small = new SolidBrush(Theme.Subtle))
        {
            g.DrawString("visual edition · launcher", Theme.FontSmall, small, 78, 50);
        }

        using var line = new Pen(Color.FromArgb(16, 255, 255, 255), 1f);
        g.DrawLine(line, 0, Height - 1, Width, Height - 1);
    }
}

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
        Theme.Fill(g, rect, 8f, Theme.CardInner);
        Theme.Border(g, rect, 8f, Theme.Stroke);
    }
}

/// <summary>Основная кнопка: градиент emerald → teal, блик и плавная подсветка.</summary>
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
        Font = new Font("Segoe UI", 10f, FontStyle.Bold);
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
        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);

        if (Enabled)
        {
            int lift = (int)(16 * hoverAmount);
            Theme.FillGradient(g, rect, 11f, Lighten(Theme.Accent, lift),
                Lighten(Theme.Accent2, (int)(12 * hoverAmount)));
            using var gloss = new LinearGradientBrush(
                new RectangleF(0, 0, Width, Height * 0.6f),
                Color.FromArgb(54, 255, 255, 255), Color.FromArgb(0, 255, 255, 255),
                LinearGradientMode.Vertical);
            using var path = Theme.Rounded(rect, 11f);
            g.FillPath(gloss, path);
        }
        else
        {
            Theme.Fill(g, rect, 11f, Color.FromArgb(70, 84, 78));
        }

        TextRenderer.DrawText(g, Text, Font, new Rectangle(0, 0, Width, Height),
            Color.White, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private static Color Lighten(Color color, int amount) => Color.FromArgb(
        color.A,
        Math.Min(255, color.R + amount),
        Math.Min(255, color.G + amount),
        Math.Min(255, color.B + amount));
}

/// <summary>Второстепенная кнопка: тёмная плашка с рамкой и мягким свечением.</summary>
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

        Color baseFill = Enabled ? Theme.CardInner : Color.FromArgb(22, 32, 27);
        Theme.Fill(g, rect, 9f, Blend(baseFill, Theme.CardHover, hoverAmount));
        Theme.Border(g, rect, 9f, Blend(Theme.Stroke, Color.FromArgb(150, Theme.Accent), hoverAmount));

        TextRenderer.DrawText(g, Text, Font, new Rectangle(0, 0, Width, Height),
            Enabled ? Blend(Theme.Text, Color.White, hoverAmount) : Theme.Subtle,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private static Color Blend(Color a, Color b, float t)
    {
        float f = Math.Clamp(t, 0f, 1f);
        return Color.FromArgb(
            (int)(a.A + (b.A - a.A) * f),
            (int)(a.R + (b.R - a.R) * f),
            (int)(a.G + (b.G - a.G) * f),
            (int)(a.B + (b.B - a.B) * f));
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
            Theme.Fill(g, rect, 8f, Color.FromArgb((int)(36 * hoverAmount), 255, 255, 255));
        }

        Color color = hoverAmount > 0.4f ? Theme.Text : Theme.Subtle;
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

        Theme.Fill(g, rect, Height / 2f, Color.FromArgb(26, 255, 255, 255));
        float width = rect.Width * Math.Clamp(value, 0, 100) / 100f;
        if (width > 2f)
        {
            Theme.FillGradient(g, new RectangleF(0, 0, width, rect.Height), Height / 2f,
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
            Theme.Fill(g, rect, 11f, Color.FromArgb(40, Theme.Accent));
            Theme.Border(g, rect, 11f, Color.FromArgb(130, Theme.Accent));
        }
        else if (hover)
        {
            Theme.Fill(g, rect, 11f, Theme.CardHover);
            Theme.Border(g, rect, 11f, Theme.Stroke);
        }
        else
        {
            Theme.Fill(g, rect, 11f, Theme.CardInner);
            Theme.Border(g, rect, 11f, Theme.Stroke);
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

/// <summary>Карточка-панель с заголовком и текстом в стиле Aurora.</summary>
internal sealed class CardPanel : Control
{
    public string Title { get; set; } = "";
    public string Body { get; set; } = "";
    public Color Accent { get; set; } = Theme.Accent;

    public CardPanel()
    {
        SetStyle(ControlStyles.SupportsTransparentBackColor | ControlStyles.UserPaint, true);
        DoubleBuffered = true;
        BackColor = Color.Transparent;
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = System.Drawing.Drawing2D.SmoothingMode.AntiAlias;

        var rect = new RectangleF(0.5f, 0.5f, Width - 1f, Height - 1f);
        using (var path = Theme.Rounded(rect, 14f))
        using (var fill = new SolidBrush(Color.FromArgb(28, 14, 30, 24)))
        {
            g.FillPath(fill, path);
        }
        using (var path = Theme.Rounded(rect, 14f))
        using (var pen = new Pen(Color.FromArgb(46, 62, 52), 1f))
        {
            g.DrawPath(pen, path);
        }

        using (var bar = new SolidBrush(Accent))
        {
            g.FillRectangle(bar, 16, 20, 3, 14);
        }
        using (var font = new Font("Segoe UI", 8.5f, FontStyle.Bold))
        using (var brush = new SolidBrush(Color.FromArgb(150, 255, 205)))
        {
            g.DrawString(Title.ToUpperInvariant(), font, brush, 26, 18);
        }

        using (var font = new Font("Segoe UI", 9f))
        using (var brush = new SolidBrush(Theme.Subtle))
        using (var format = new StringFormat { Trimming = StringTrimming.EllipsisCharacter })
        {
            var area = new RectangleF(16, 44, Width - 32, Height - 56);
            g.DrawString(Body, font, brush, area, format);
        }
    }
}