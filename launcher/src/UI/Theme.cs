using System.Drawing.Drawing2D;

namespace AuroraLauncher.UI;

/// <summary>
/// Палитра «aurora»: глубокий зелёно-чёрный фон, изумрудное свечение,
/// бирюзовые и мятные акценты — как настоящее северное сияние.
/// </summary>
internal static class Theme
{
    public static readonly Color BgDeep = Color.FromArgb(4, 10, 8);
    public static readonly Color Bg = Color.FromArgb(8, 17, 14);
    public static readonly Color Card = Color.FromArgb(14, 26, 21);
    public static readonly Color CardInner = Color.FromArgb(20, 35, 28);
    public static readonly Color CardHover = Color.FromArgb(28, 47, 38);
    public static readonly Color Stroke = Color.FromArgb(38, 62, 52);
    public static readonly Color Text = Color.FromArgb(232, 247, 238);
    public static readonly Color Subtle = Color.FromArgb(138, 170, 156);

    public static readonly Color Accent = Color.FromArgb(62, 232, 160);
    public static readonly Color Accent2 = Color.FromArgb(64, 214, 214);
    public static readonly Color Mint = Color.FromArgb(150, 255, 205);
    public static readonly Color Deep = Color.FromArgb(24, 138, 96);
    public static readonly Color Danger = Color.FromArgb(255, 110, 118);
    public static readonly Color Good = Color.FromArgb(96, 232, 160);

    public static readonly Font FontSmall = new("Segoe UI", 8.25f);
    public static readonly Font FontBody = new("Segoe UI", 9f);
    public static readonly Font FontBold = new("Segoe UI", 9f, FontStyle.Bold);
    public static readonly Font FontTitle = new("Segoe UI", 16f, FontStyle.Bold);
    public static readonly Font FontHero = new("Segoe UI", 22f, FontStyle.Bold);

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
    public static void Glow(Graphics g, RectangleF rect, Color color)
    {
        using var path = new GraphicsPath();
        path.AddEllipse(rect);
        using var brush = new PathGradientBrush(path)
        {
            CenterColor = color,
            SurroundColors = new[] { Color.FromArgb(0, color.R, color.G, color.B) },
        };
        g.FillEllipse(brush, rect);
    }

    /// <summary>
    /// Полотно северного сияния: волнистая лента, залитая вертикальным
    /// градиентом «прозрачный → цвет → прозрачный». Живёт за счёт фазы времени.
    /// </summary>
    public static void Curtain(Graphics g, int width, int height, double time,
        double phase, double speed, float baseY, float amplitude, double frequency,
        float thickness, Color color)
    {
        if (width <= 1 || height <= 1)
        {
            return;
        }

        int step = 8;
        var top = new List<PointF>(width / step + 3);
        for (int x = -16; x <= width + 16; x += step)
        {
            double wx = x * frequency + phase + time * speed;
            double wave = Math.Sin(wx) * amplitude
                          + Math.Sin(wx * 0.47 - time * speed * 0.6) * amplitude * 0.45;
            top.Add(new PointF(x, (float)(baseY + wave)));
        }

        var points = new List<PointF>(top.Count * 2);
        points.AddRange(top);
        for (int i = top.Count - 1; i >= 0; i--)
        {
            double wx = top[i].X * frequency + phase + time * speed + 1.7;
            float local = (float)(0.7 + 0.3 * Math.Sin(wx * 0.8));
            points.Add(new PointF(top[i].X, top[i].Y + thickness * local));
        }

        using var path = new GraphicsPath();
        path.AddPolygon(points.ToArray());

        var bounds = path.GetBounds();
        if (bounds.Height < 1f)
        {
            return;
        }

        using var brush = new LinearGradientBrush(bounds,
            Color.FromArgb(0, color), color, LinearGradientMode.Vertical)
        {
            InterpolationColors = new ColorBlend
            {
                Colors = new[]
                {
                    Color.FromArgb(0, color.R, color.G, color.B),
                    Color.FromArgb(color.A, color.R, color.G, color.B),
                    Color.FromArgb((int)(color.A * 0.35f), color.R, color.G, color.B),
                    Color.FromArgb(0, color.R, color.G, color.B),
                },
                Positions = new[] { 0f, 0.35f, 0.75f, 1f },
            },
        };
        g.FillPath(brush, path);
    }

    public static string ShortUuid(string uuid) =>
        string.IsNullOrEmpty(uuid) ? "" : (uuid.Length > 8 ? uuid[..8] : uuid);
}
