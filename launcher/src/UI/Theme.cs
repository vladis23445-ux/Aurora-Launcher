using System.Drawing.Drawing2D;

namespace AuroraLauncher.UI;

/// <summary>
/// Палитра лаунчера: графитово-индиговый фон, фиолетово-розовый градиент,
/// матовые стеклянные панели и тонкие светлые линии.
/// </summary>
internal static class Theme
{
    public static readonly Color BgDeep = Color.FromArgb(7, 7, 13);
    public static readonly Color Bg = Color.FromArgb(13, 14, 24);
    public static readonly Color Card = Color.FromArgb(19, 21, 34);
    public static readonly Color CardInner = Color.FromArgb(26, 29, 45);
    public static readonly Color CardHover = Color.FromArgb(35, 39, 62);
    public static readonly Color Stroke = Color.FromArgb(44, 49, 79);
    public static readonly Color Text = Color.FromArgb(237, 239, 249);
    public static readonly Color Subtle = Color.FromArgb(142, 150, 180);

    public static readonly Color Accent = Color.FromArgb(124, 92, 255);
    public static readonly Color Accent2 = Color.FromArgb(255, 92, 157);
    public static readonly Color Mint = Color.FromArgb(195, 180, 255);
    public static readonly Color Deep = Color.FromArgb(59, 47, 134);
    public static readonly Color Danger = Color.FromArgb(255, 95, 109);
    public static readonly Color Good = Color.FromArgb(74, 222, 128);

    public static readonly Font FontSmall = new("Segoe UI", 8.25f);
    public static readonly Font FontBody = new("Segoe UI", 9f);
    public static readonly Font FontBold = new("Segoe UI", 9f, FontStyle.Bold);
    public static readonly Font FontCap = new("Segoe UI", 7.9f, FontStyle.Bold);
    public static readonly Font FontTitle = new("Segoe UI", 16f, FontStyle.Bold);
    public static readonly Font FontHero = new("Segoe UI", 25f, FontStyle.Bold);

    public static GraphicsPath Rounded(RectangleF rect, float radius)
    {
        var path = new GraphicsPath();
        float d = Math.Max(0f, radius * 2f);
        if (d <= 0.2f)
        {
            path.AddRectangle(rect);
            return path;
        }
        d = Math.Min(d, Math.Min(rect.Width, rect.Height));
        path.AddArc(rect.X, rect.Y, d, d, 180, 90);
        path.AddArc(rect.Right - d, rect.Y, d, d, 270, 90);
        path.AddArc(rect.Right - d, rect.Bottom - d, d, d, 0, 90);
        path.AddArc(rect.X, rect.Bottom - d, d, d, 90, 90);
        path.CloseFigure();
        return path;
    }

    public static void Fill(Graphics g, RectangleF rect, float radius, Color color)
    {
        using var path = Rounded(rect, radius);
        using var brush = new SolidBrush(color);
        g.FillPath(brush, path);
    }

    public static void FillGradient(Graphics g, RectangleF rect, float radius, Color from, Color to,
        LinearGradientMode mode = LinearGradientMode.Horizontal)
    {
        using var path = Rounded(rect, radius);
        using var brush = new LinearGradientBrush(rect, from, to, mode);
        g.FillPath(brush, path);
    }

    public static void Border(Graphics g, RectangleF rect, float radius, Color color, float width = 1f)
    {
        using var path = Rounded(rect, radius);
        using var pen = new Pen(color, width);
        g.DrawPath(pen, path);
    }

    /// <summary>Мягкое радиальное свечение.</summary>
    /// <summary>
    /// Мягкое радиальное свечение. Рисуется серией вложенных эллипсов:
    /// PathGradientBrush ломается, когда GDI+ отдаёт обрезанный контекст
    /// прозрачного дочернего контрола.
    /// </summary>
    public static void Glow(Graphics g, RectangleF rect, Color color)
    {
        if (rect.Width <= 2f || rect.Height <= 2f)
        {
            return;
        }

        const int steps = 20;
        float cx = rect.X + rect.Width / 2f;
        float cy = rect.Y + rect.Height / 2f;
        int alpha = Math.Max(4, color.A / 9);

        for (int i = 0; i < steps; i++)
        {
            float scale = 1f - i * 0.045f;
            var r = new RectangleF(cx - rect.Width * scale / 2f, cy - rect.Height * scale / 2f,
                rect.Width * scale, rect.Height * scale);
            using var brush = new SolidBrush(Color.FromArgb(Math.Min(255, alpha), color));
            g.FillEllipse(brush, r);
        }
    }

    /// <summary>Цвет с заданной прозрачностью.</summary>
    public static Color Alpha(Color color, int alpha) =>
        Color.FromArgb(Math.Clamp(alpha, 0, 255), color.R, color.G, color.B);

    public static Color Blend(Color a, Color b, float t)
    {
        float f = Math.Clamp(t, 0f, 1f);
        return Color.FromArgb(
            (int)(a.A + (b.A - a.A) * f),
            (int)(a.R + (b.R - a.R) * f),
            (int)(a.G + (b.G - a.G) * f),
            (int)(a.B + (b.B - a.B) * f));
    }

    public static string ShortUuid(string uuid) =>
        string.IsNullOrEmpty(uuid) ? "" : (uuid.Length > 8 ? uuid[..8] : uuid);
}
