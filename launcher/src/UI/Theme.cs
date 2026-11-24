using System.Drawing.Drawing2D;

namespace AuroraLauncher.UI;

/// <summary>
/// Палитра лаунчера: графитово-индиговый фон, фиолетово-розовый градиент,
/// матовые стеклянные панели и тонкие светлые линии.
/// </summary>
internal static class Theme
{
    public static Color BgDeep { get; private set; } = Color.FromArgb(7, 7, 13);
    public static Color Bg { get; private set; } = Color.FromArgb(13, 14, 24);
    public static Color Card { get; private set; } = Color.FromArgb(19, 21, 34);
    public static Color CardInner { get; private set; } = Color.FromArgb(26, 29, 45);
    public static Color CardHover { get; private set; } = Color.FromArgb(35, 39, 62);
    public static Color Stroke { get; private set; } = Color.FromArgb(44, 49, 79);
    public static Color Text { get; private set; } = Color.FromArgb(237, 239, 249);
    public static Color Subtle { get; private set; } = Color.FromArgb(142, 150, 180);

    public static Color Accent { get; private set; } = Color.FromArgb(124, 92, 255);
    public static Color Accent2 { get; private set; } = Color.FromArgb(255, 92, 157);
    public static Color Mint { get; private set; } = Color.FromArgb(195, 180, 255);
    public static Color Deep { get; private set; } = Color.FromArgb(59, 47, 134);

    public static string Current { get; private set; } = "violet";

    public sealed record Preset(string Id, string Name, Color Accent, Color Accent2, Color Mint,
        Color Deep, Color BgDeep, Color Bg, Color Card, Color CardInner, Color Stroke);

    public static readonly Preset[] Presets =
    {
        new("violet", "Фиолетовая", Color.FromArgb(124, 92, 255), Color.FromArgb(255, 92, 157),
            Color.FromArgb(195, 180, 255), Color.FromArgb(59, 47, 134), Color.FromArgb(7, 7, 13),
            Color.FromArgb(13, 14, 24), Color.FromArgb(19, 21, 34), Color.FromArgb(26, 29, 45),
            Color.FromArgb(44, 49, 79)),
        new("emerald", "Изумрудная", Color.FromArgb(59, 232, 160), Color.FromArgb(64, 214, 214),
            Color.FromArgb(140, 255, 200), Color.FromArgb(30, 138, 96), Color.FromArgb(4, 14, 10),
            Color.FromArgb(8, 24, 18), Color.FromArgb(14, 32, 24), Color.FromArgb(20, 43, 33),
            Color.FromArgb(38, 62, 52)),
        new("crimson", "Алая", Color.FromArgb(255, 77, 109), Color.FromArgb(255, 162, 77),
            Color.FromArgb(255, 194, 201), Color.FromArgb(138, 30, 50), Color.FromArgb(15, 5, 8),
            Color.FromArgb(26, 10, 14), Color.FromArgb(36, 15, 21), Color.FromArgb(48, 20, 28),
            Color.FromArgb(74, 34, 44)),
        new("ocean", "Океан", Color.FromArgb(76, 141, 255), Color.FromArgb(65, 224, 224),
            Color.FromArgb(191, 227, 255), Color.FromArgb(30, 76, 138), Color.FromArgb(4, 7, 15),
            Color.FromArgb(8, 16, 28), Color.FromArgb(15, 24, 38), Color.FromArgb(21, 33, 50),
            Color.FromArgb(35, 53, 80)),
        new("amber", "Янтарная", Color.FromArgb(255, 176, 58), Color.FromArgb(255, 107, 61),
            Color.FromArgb(255, 224, 178), Color.FromArgb(138, 90, 30), Color.FromArgb(15, 10, 4),
            Color.FromArgb(26, 18, 6), Color.FromArgb(38, 27, 11), Color.FromArgb(50, 36, 15),
            Color.FromArgb(76, 55, 26)),
    };

    /// <summary>Применяет тему по идентификатору; неизвестный id → фиолетовая.</summary>
    public static void Apply(string? id)
    {
        var preset = Presets.FirstOrDefault(p => p.Id == id) ?? Presets[0];
        Current = preset.Id;
        Accent = preset.Accent;
        Accent2 = preset.Accent2;
        Mint = preset.Mint;
        Deep = preset.Deep;
        BgDeep = preset.BgDeep;
        Bg = preset.Bg;
        Card = preset.Card;
        CardInner = preset.CardInner;
        Stroke = preset.Stroke;
        Text = Color.FromArgb(
            Math.Min(255, preset.Card.R + 218),
            Math.Min(255, preset.Card.G + 218),
            Math.Min(255, preset.Card.B + 215));
        Subtle = Color.FromArgb(
            (preset.Accent.R + preset.Accent2.R + 255) / 3,
            (preset.Accent.G + preset.Accent2.G + 255) / 3,
            (preset.Accent.B + preset.Accent2.B + 255) / 3);
        CardHover = Color.FromArgb(
            Math.Min(255, preset.CardInner.R + 10),
            Math.Min(255, preset.CardInner.G + 10),
            Math.Min(255, preset.CardInner.B + 17));
    }
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
