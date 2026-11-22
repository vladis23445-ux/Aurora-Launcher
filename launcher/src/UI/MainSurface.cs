using System.Drawing.Drawing2D;

namespace AuroraLauncher.UI;

/// <summary>
/// Единая поверхность главного окна: фон, шапка, карточки, кнопки и Стив
/// рисуются в одном контексте, поэтому прозрачных дочерних контролов нет.
/// </summary>
internal sealed class AuroraSurface : Control
{
    private sealed class Zone
    {
        public string Id = "";
        public Rectangle Bounds;
        public ButtonKind Kind;
        public bool Enabled = true;
    }

    internal enum ButtonKind
    {
        Accent,
        Ghost,
        Chip,
        Glyph,
    }

    private readonly List<Zone> zones = new();
    private readonly SteveSprite steve = new();
    private readonly System.Windows.Forms.Timer timer = new() { Interval = 33 };

    private string? hoverId;
    private string? pressId;
    private bool dragging;
    private Point dragOrigin;
    private Point dragStart;

    public event EventHandler<string>? Action;

    // состояние, которое выставляет окно
    public string Caption { get; set; } = "";
    public string Title { get; set; } = "Aurora Visuals";
    public string Subtitle { get; set; } = "";
    public string StateText { get; set; } = "";
    public bool StateOk { get; set; } = true;
    public string StatusText { get; set; } = "Готово";
    public Color StatusColor { get; set; } = Theme.Subtle;
    public string AccountName { get; set; } = "аккаунт";
    public string BuildId { get; set; } = "standard";
    public string BuildSummary { get; set; } = "";
    public string Footer { get; set; } = "";
    public int Progress { get; set; }
    public bool Busy { get; set; }

    public string CardStatusTitle { get; set; } = "Состояние";
    public string CardStatusBody { get; set; } = "";
    public string CardBuildTitle { get; set; } = "Сборка";
    public string CardBuildBody { get; set; } = "";
    public string CardPromoTitle { get; set; } = "Проект";
    public string CardPromoBody { get; set; } = "";
    public string CardStartTitle { get; set; } = "Как начать";
    public string CardStartBody { get; set; } = "";

    public AuroraSurface()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.Selectable, true);
        BackColor = Theme.Bg;
        TabStop = false;
        timer.Tick += (_, _) => StepAnimation();
        timer.Start();
    }

    public void RefreshState()
    {
        LayoutZones();
        Invalidate();
    }

    private void StepAnimation()
    {
        if (!IsHandleCreated)
        {
            return;
        }
        steve.Tick(SteveBounds, PointToClient(Cursor.Position));
        Invalidate(SteveBounds);
    }

    // ------------------------------------------------------------- геометрия

    private const int HeaderHeight = 78;

    private static Rectangle PlayBounds => new(56, 196, 320, 56);
    private static Rectangle InstallBounds => new(56, 262, 320, 40);
    private static Rectangle ProgressBounds => new(56, 338, 320, 5);
    private static Rectangle SteveBounds => new(392, 84, 300, 500);

    private void LayoutZones()
    {
        zones.Clear();
        zones.Add(new Zone { Id = "play", Bounds = PlayBounds, Kind = ButtonKind.Accent, Enabled = !Busy });
        zones.Add(new Zone { Id = "install", Bounds = InstallBounds, Kind = ButtonKind.Accent, Enabled = !Busy });
        zones.Add(new Zone { Id = "accounts", Bounds = new Rectangle(56, 382, 152, 36), Kind = ButtonKind.Ghost, Enabled = !Busy });
        zones.Add(new Zone { Id = "settings", Bounds = new Rectangle(224, 382, 152, 36), Kind = ButtonKind.Ghost, Enabled = !Busy });
        zones.Add(new Zone { Id = "console", Bounds = new Rectangle(56, 426, 152, 36), Kind = ButtonKind.Ghost });
        zones.Add(new Zone { Id = "network", Bounds = new Rectangle(224, 426, 152, 36), Kind = ButtonKind.Ghost });
        zones.Add(new Zone { Id = "build-standard", Bounds = new Rectangle(56, 496, 150, 34), Kind = ButtonKind.Ghost, Enabled = !Busy });
        zones.Add(new Zone { Id = "build-optimized", Bounds = new Rectangle(214, 496, 162, 34), Kind = ButtonKind.Ghost, Enabled = !Busy });

        int center = Width / 2;
        zones.Add(new Zone { Id = "chip", Bounds = new Rectangle(center - 105, 21, 210, 32), Kind = ButtonKind.Chip });
        zones.Add(new Zone { Id = "min", Bounds = new Rectangle(Width - 88, 23, 30, 30), Kind = ButtonKind.Glyph });
        zones.Add(new Zone { Id = "close", Bounds = new Rectangle(Width - 52, 23, 30, 30), Kind = ButtonKind.Glyph });
    }

    private Zone? HitTest(Point point)
    {
        for (int i = zones.Count - 1; i >= 0; i--)
        {
            if (zones[i].Bounds.Contains(point))
            {
                return zones[i];
            }
        }
        return null;
    }

    // ------------------------------------------------------------- события

    protected override void OnResize(EventArgs e)
    {
        LayoutZones();
        base.OnResize(e);
    }

    protected override void OnMouseMove(MouseEventArgs e)
    {
        if (dragging)
        {
            var screen = PointToScreen(e.Location);
            var target = Parent ?? FindForm();
            if (target != null)
            {
                target.Location = new Point(dragStart.X + screen.X - dragOrigin.X, dragStart.Y + screen.Y - dragOrigin.Y);
            }
            return;
        }

        var hit = HitTest(e.Location);
        string? id = hit != null && hit.Enabled ? hit.Id : null;
        if (id != hoverId)
        {
            hoverId = id;
            Invalidate();
        }
        Cursor = id != null ? Cursors.Hand
            : e.Y <= HeaderHeight ? Cursors.SizeAll
            : Cursors.Default;
        base.OnMouseMove(e);
    }

    protected override void OnMouseLeave(EventArgs e)
    {
        if (!dragging && hoverId != null)
        {
            hoverId = null;
            Invalidate();
        }
        base.OnMouseLeave(e);
    }

    protected override void OnMouseDown(MouseEventArgs e)
    {
        if (e.Button == MouseButtons.Left)
        {
            var hit = HitTest(e.Location);
            if (hit?.Enabled == true)
            {
                pressId = hit.Id;
                Invalidate();
            }
            else if (e.Y <= HeaderHeight && FindForm() is { } form)
            {
                dragging = true;
                dragOrigin = PointToScreen(e.Location);
                dragStart = form.Location;
            }
        }
        base.OnMouseDown(e);
    }

    protected override void OnMouseUp(MouseEventArgs e)
    {
        if (dragging)
        {
            dragging = false;
            return;
        }

        var hit = HitTest(e.Location);
        string? pressed = pressId;
        pressId = null;
        Invalidate();
        if (pressed != null && hit?.Id == pressed && hit.Enabled)
        {
            Action?.Invoke(this, pressed);
        }
        base.OnMouseUp(e);
    }

    // ------------------------------------------------------------- отрисовка

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;

        if (zones.Count == 0)
        {
            LayoutZones();
        }

        DrawBackdrop(g);
        DrawHeader(g);
        DrawHero(g);
        steve.Draw(g, SteveBounds);
        DrawCards(g);
        DrawFooter(g);
    }

    private void DrawBackdrop(Graphics g)
    {
        var rect = new Rectangle(0, 0, Math.Max(1, Width), Math.Max(1, Height));
        using (var brush = new LinearGradientBrush(rect, Theme.BgDeep, Theme.Bg, LinearGradientMode.ForwardDiagonal))
        {
            g.FillRectangle(brush, rect);
        }

        Theme.Glow(g, new RectangleF(-160, -260, 760, 720), Color.FromArgb(70, Theme.Accent));
        Theme.Glow(g, new RectangleF(Width - 520, -120, 700, 640), Color.FromArgb(52, Theme.Accent2));
        Theme.Glow(g, new RectangleF(Width * 0.25f, Height * 0.45f, 700, 520), Color.FromArgb(36, Theme.Deep));

        using (var dot = new SolidBrush(Color.FromArgb(18, 255, 255, 255)))
        {
            for (int y = 24; y < Height; y += 26)
            {
                for (int x = 24; x < Width; x += 26)
                {
                    g.FillRectangle(dot, x, y, 1f, 1f);
                }
            }
        }

        using var vignette = new LinearGradientBrush(rect,
            Color.FromArgb(0, 0, 0, 0), Color.FromArgb(110, 0, 0, 0), LinearGradientMode.Vertical);
        g.FillRectangle(vignette, rect);
    }

    private void DrawHeader(Graphics g)
    {
        var mark = new RectangleF(28, 20, 36, 36);
        Theme.Glow(g, new RectangleF(mark.X - 12, mark.Y - 12, mark.Width + 24, mark.Height + 24),
            Color.FromArgb(85, Theme.Accent));
        Theme.FillGradient(g, mark, 12f, Theme.Accent, Theme.Accent2, LinearGradientMode.BackwardDiagonal);

        float cx = mark.X + mark.Width / 2f;
        float cy = mark.Y + mark.Height / 2f;
        using (var spark = new GraphicsPath())
        {
            spark.AddPolygon(new[]
            {
                new PointF(cx, cy - 11), new PointF(cx + 3f, cy - 3f),
                new PointF(cx + 11, cy), new PointF(cx + 3f, cy + 3f),
                new PointF(cx, cy + 11), new PointF(cx - 3f, cy + 3f),
                new PointF(cx - 11, cy), new PointF(cx - 3f, cy - 3f),
            });
            using var brush = new SolidBrush(Color.FromArgb(240, 255, 255, 255));
            g.FillPath(brush, spark);
        }

        TextRenderer.DrawText(g, "AURORA", Theme.FontTitle, new Rectangle(78, 16, 240, 28), Color.White,
            TextFormatFlags.Left | TextFormatFlags.NoPadding);
        TextRenderer.DrawText(g, "L A U N C H E R   ·   V I S U A L   E D I T I O N", Theme.FontCap,
            new Rectangle(80, 44, 460, 16), Color.FromArgb(150, Theme.Subtle),
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        using var line = new Pen(Color.FromArgb(24, 255, 255, 255), 1f);
        g.DrawLine(line, 0, HeaderHeight - 1, Width, HeaderHeight - 1);

        foreach (var zone in zones)
        {
            switch (zone.Id)
            {
                case "chip":
                    DrawChip(g, zone);
                    break;
                case "min":
                    DrawGlyph(g, zone, "–");
                    break;
                case "close":
                    DrawGlyph(g, zone, "✕");
                    break;
            }
        }
    }

    private void DrawChip(Graphics g, Zone zone)
    {
        bool hover = hoverId == zone.Id;
        var rect = zone.Bounds;
        var rounded = new RectangleF(rect.X + 0.5f, rect.Y + 0.5f, rect.Width - 1f, rect.Height - 1f);
        Theme.Fill(g, rounded, 16f, Color.FromArgb(hover ? 46 : 28, 255, 255, 255));
        Theme.Border(g, rounded, 16f, Color.FromArgb(hover ? 150 : 70, Theme.Accent2));
        TextRenderer.DrawText(g, AccountName, Theme.FontBold, rect,
            hover ? Color.White : Theme.Text,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private void DrawGlyph(Graphics g, Zone zone, string glyph)
    {
        bool hover = hoverId == zone.Id;
        if (hover)
        {
            Theme.Fill(g, new RectangleF(zone.Bounds.X, zone.Bounds.Y, zone.Bounds.Width, zone.Bounds.Height),
                9f, Color.FromArgb(48, 255, 255, 255));
        }
        TextRenderer.DrawText(g, glyph, Theme.FontBold, zone.Bounds,
            hover ? Color.White : Theme.Subtle,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private void DrawHero(Graphics g)
    {
        TextRenderer.DrawText(g, Caption, Theme.FontCap, new Rectangle(56, 100, 340, 16),
            Color.FromArgb(170, Theme.Subtle), TextFormatFlags.Left | TextFormatFlags.NoPadding);

        TextRenderer.DrawText(g, Title, Theme.FontHero, new Rectangle(52, 112, 360, 46), Theme.Text,
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        TextRenderer.DrawText(g, Subtitle, Theme.FontBody, new Rectangle(56, 172, 340, 18), Theme.Subtle,
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        foreach (var zone in zones)
        {
            switch (zone.Id)
            {
                case "play":
                    DrawAccent(g, zone, "ИГРАТЬ", 14f, 12f);
                    break;
                case "install":
                    DrawAccent(g, zone, "УСТАНОВИТЬ", 12f, 10.5f);
                    break;
                case "accounts":
                    DrawGhost(g, zone, "Аккаунты");
                    break;
                case "settings":
                    DrawGhost(g, zone, "Настройки");
                    break;
                case "console":
                    DrawGhost(g, zone, "Консоль");
                    break;
                case "network":
                    DrawGhost(g, zone, "СЕТЬ");
                    break;
                case "build-standard":
                    DrawPill(g, zone, "Стандарт", BuildId != "optimized");
                    break;
                case "build-optimized":
                    DrawPill(g, zone, "Оптимизация", BuildId == "optimized");
                    break;
            }
        }

        TextRenderer.DrawText(g, StateText, Theme.FontSmall, new Rectangle(56, 314, 340, 18),
            StateOk ? Color.FromArgb(215, Theme.Mint) : Color.FromArgb(255, 246, 190, 108),
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        DrawBuildSection(g);

        var bar = ProgressBounds;
        Theme.Fill(g, bar, bar.Height / 2f, Color.FromArgb(24, 255, 255, 255));
        int filled = (int)(bar.Width * Math.Clamp(Progress, 0, 100) / 100f);
        if (filled > 2)
        {
            Theme.FillGradient(g, new RectangleF(bar.X, bar.Y, filled, bar.Height), bar.Height / 2f,
                Theme.Accent, Theme.Accent2);
        }

        TextRenderer.DrawText(g, StatusText, Theme.FontSmall, new Rectangle(56, 350, 340, 16),
            StatusColor, TextFormatFlags.Left | TextFormatFlags.NoPadding);
    }

    private void DrawAccent(Graphics g, Zone zone, string label, float radius, float fontSize)
    {
        bool hover = hoverId == zone.Id && zone.Enabled;
        bool pressed = pressId == zone.Id;
        var rect = new RectangleF(zone.Bounds.X, zone.Bounds.Y + (pressed ? 1 : 0),
            zone.Bounds.Width, zone.Bounds.Height - 2);

        if (!zone.Enabled)
        {
            Theme.Fill(g, rect, radius, Color.FromArgb(40, 43, 64));
            Theme.Border(g, rect, radius, Theme.Stroke);
            TextRenderer.DrawText(g, label, Theme.FontBold, Rectangle.Round(rect),
                Color.FromArgb(120, Theme.Subtle),
                TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
            return;
        }

        Theme.Glow(g, new RectangleF(rect.X, rect.Y + rect.Height * 0.2f, rect.Width, rect.Height * 1.5f),
            Color.FromArgb(hover ? 92 : 58, Theme.Accent));
        Theme.FillGradient(g, rect, radius,
            hover ? Color.FromArgb(140, 108, 255) : Theme.Accent,
            hover ? Color.FromArgb(255, 116, 176) : Theme.Accent2);

        using (var gloss = new LinearGradientBrush(
                   new RectangleF(rect.X, rect.Y, rect.Width, rect.Height * 0.62f),
                   Color.FromArgb(46, 255, 255, 255), Color.FromArgb(0, 255, 255, 255),
                   LinearGradientMode.Vertical))
        using (var path = Theme.Rounded(rect, radius))
        {
            g.FillPath(gloss, path);
        }
        Theme.Border(g, rect, radius, Color.FromArgb(70, 255, 255, 255));

        using var font = new Font("Segoe UI", fontSize, FontStyle.Bold);
        TextRenderer.DrawText(g, label, font, Rectangle.Round(rect), Color.White,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private void DrawGhost(Graphics g, Zone zone, string label)
    {
        bool hover = hoverId == zone.Id && zone.Enabled;
        var rect = new RectangleF(zone.Bounds.X + 0.5f, zone.Bounds.Y + 0.5f,
            zone.Bounds.Width - 1f, zone.Bounds.Height - 1f);

        Theme.Fill(g, rect, 11f, Color.FromArgb(zone.Enabled ? (hover ? 42 : 20) : 12, 255, 255, 255));
        Theme.Border(g, rect, 11f, Color.FromArgb(hover ? 120 : 40, Theme.Accent2));
        TextRenderer.DrawText(g, label, Theme.FontBold, zone.Bounds,
            zone.Enabled ? (hover ? Color.White : Theme.Text) : Color.FromArgb(110, Theme.Subtle),
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private void DrawBuildSection(Graphics g)
    {
        TextRenderer.DrawText(g, "СБОРКА", Theme.FontCap, new Rectangle(56, 474, 200, 16),
            Color.FromArgb(170, Theme.Subtle), TextFormatFlags.Left | TextFormatFlags.NoPadding);

        if (!string.IsNullOrEmpty(BuildSummary))
        {
            TextRenderer.DrawText(g, BuildSummary, Theme.FontSmall, new Rectangle(56, 540, 344, 44),
                Color.FromArgb(190, Theme.Subtle),
                TextFormatFlags.Left | TextFormatFlags.WordBreak | TextFormatFlags.NoPadding);
        }
    }

    private void DrawPill(Graphics g, Zone zone, string label, bool active)
    {
        bool hover = hoverId == zone.Id && zone.Enabled;
        var rect = new RectangleF(zone.Bounds.X + 0.5f, zone.Bounds.Y + 0.5f,
            zone.Bounds.Width - 1f, zone.Bounds.Height - 1f);

        if (active)
        {
            Theme.FillGradient(g, rect, 12f, Theme.Alpha(Theme.Accent, 78), Theme.Alpha(Theme.Accent2, 62));
            Theme.Border(g, rect, 12f, Color.FromArgb(180, Theme.Accent2));
        }
        else
        {
            Theme.Fill(g, rect, 12f, Color.FromArgb(zone.Enabled ? (hover ? 40 : 18) : 10, 255, 255, 255));
            Theme.Border(g, rect, 12f, Color.FromArgb(hover ? 110 : 34, Theme.Accent2));
        }

        Color text = active ? Color.White : zone.Enabled ? Theme.Subtle : Color.FromArgb(110, Theme.Subtle);
        TextRenderer.DrawText(g, label, Theme.FontBold, zone.Bounds, text,
            TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }

    private void DrawCards(Graphics g)
    {
        DrawCard(g, new Rectangle(720, 100, 304, 118), CardStatusTitle, CardStatusBody, Theme.Accent2);
        DrawCard(g, new Rectangle(720, 228, 304, 108), CardBuildTitle, CardBuildBody, Theme.Accent);
        DrawCard(g, new Rectangle(720, 348, 304, 132), CardPromoTitle, CardPromoBody, Theme.Mint);
        DrawCard(g, new Rectangle(720, 492, 304, 100), CardStartTitle, CardStartBody, Theme.Good);
    }

    private static void DrawCard(Graphics g, Rectangle bounds, string title, string body, Color accent)
    {
        var rect = new RectangleF(bounds.X + 0.5f, bounds.Y + 0.5f, bounds.Width - 1f, bounds.Height - 1f);
        Theme.Fill(g, rect, 15f, Color.FromArgb(228, 20, 22, 36));
        Theme.Border(g, rect, 15f, Color.FromArgb(30, 255, 255, 255));

        using (var top = new LinearGradientBrush(
                   new RectangleF(rect.X, rect.Y, rect.Width, 26f),
                   Color.FromArgb(24, 255, 255, 255), Color.FromArgb(0, 255, 255, 255),
                   LinearGradientMode.Vertical))
        using (var path = Theme.Rounded(rect, 15f))
        {
            g.FillPath(top, path);
        }

        using (var dot = new SolidBrush(accent))
        {
            g.FillEllipse(dot, bounds.X + 16, bounds.Y + 20, 5, 5);
        }

        TextRenderer.DrawText(g, Spaced(title.ToUpperInvariant()), Theme.FontCap,
            new Rectangle(bounds.X + 28, bounds.Y + 15, bounds.Width - 44, 16),
            Color.FromArgb(205, Theme.Subtle),
            TextFormatFlags.Left | TextFormatFlags.NoPadding);

        TextRenderer.DrawText(g, body, Theme.FontBody,
            new Rectangle(bounds.X + 16, bounds.Y + 38, bounds.Width - 32, bounds.Height - 50),
            Theme.Subtle,
            TextFormatFlags.Left | TextFormatFlags.WordBreak | TextFormatFlags.NoPadding);
    }

    private void DrawFooter(Graphics g)
    {
        TextRenderer.DrawText(g, Footer, Theme.FontSmall, new Rectangle(56, 600, Width - 120, 16),
            Color.FromArgb(112, Theme.Subtle), TextFormatFlags.Left | TextFormatFlags.NoPadding);
    }

    private static string Spaced(string text) => string.Join(" ", text.ToCharArray());

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
