using System.Drawing.Drawing2D;

namespace AuroraLauncher.UI;

/// <summary>
/// Спрайт Стива: пиксельная фигура, голова поворачивается и зрачки следят
/// за курсором. Рисуется в общую поверхность окна, поэтому никаких
/// прозрачных дочерних контролов не используется.
/// </summary>
internal sealed class SteveSprite
{
    private const float BodyBottom = 29f;

    private float lookX;
    private float lookY;
    private float targetX;
    private float targetY;
    private double bobPhase;
    private long ticks;

    private static readonly Color Hair = Color.FromArgb(58, 41, 28);
    private static readonly Color HairShade = Color.FromArgb(40, 28, 19);
    private static readonly Color Skin = Color.FromArgb(201, 153, 115);
    private static readonly Color SkinShade = Color.FromArgb(174, 128, 93);
    private static readonly Color Shirt = Color.FromArgb(40, 173, 178);
    private static readonly Color ShirtShade = Color.FromArgb(29, 138, 146);
    private static readonly Color Pants = Color.FromArgb(74, 79, 176);
    private static readonly Color PantsShade = Color.FromArgb(56, 60, 143);
    private static readonly Color Shoes = Color.FromArgb(88, 90, 106);
    private static readonly Color EyeWhite = Color.FromArgb(247, 249, 255);
    private static readonly Color PupilColor = Color.FromArgb(44, 58, 132);
    private static readonly Color Mouth = Color.FromArgb(116, 74, 58);

    /// <summary>Пересчёт взгляда и дыхания, вызывается таймером окна.</summary>
    public void Tick(Rectangle bounds, Point cursor)
    {
        ticks++;
        bobPhase += 0.055;

        float unit = Math.Min(bounds.Width / 18f, bounds.Height / (BodyBottom + 4f));
        if (unit <= 0f || !float.IsFinite(unit))
        {
            unit = 1f;
        }
        float left = bounds.X + (bounds.Width - 16f * unit) / 2f;
        float top = bounds.Y + (bounds.Height - BodyBottom * unit) / 2f;
        float cx = left + 8f * unit;
        float cy = top + 5f * unit;
        float radius = Math.Max(70f, 8f * unit);

        targetX = Math.Clamp((cursor.X - cx) / radius, -1f, 1f);
        targetY = Math.Clamp((cursor.Y - cy) / radius, -1f, 1f);
        lookX += (targetX - lookX) * 0.16f;
        lookY += (targetY - lookY) * 0.16f;
    }

    private bool Blinking => ticks % 240 is > 228 and < 236;

    public void Draw(Graphics g, Rectangle bounds)
    {
        float unit = Math.Min(bounds.Width / 18f, bounds.Height / (BodyBottom + 4f));
        if (unit <= 0f || !float.IsFinite(unit))
        {
            return;
        }

        float left = bounds.X + (bounds.Width - 16f * unit) / 2f;
        float top = bounds.Y + (bounds.Height - BodyBottom * unit) / 2f;
        float bob = (float)Math.Sin(bobPhase) * unit * 0.22f;

        float X(float units) => left + units * unit;
        float Y(float units) => top + units * unit + bob;

        void Box(float x, float y, float w, float h, Color color)
        {
            using var brush = new SolidBrush(color);
            g.FillRectangle(brush, X(x), Y(y), w * unit, h * unit);
        }

        // мягкая тень на «полу»
        var shadow = new RectangleF(X(1.4f), Y(28.4f) - bob, unit * 13.2f, unit * 2.6f);
        for (int i = 0; i < 6; i++)
        {
            float k = 1f - i * 0.13f;
            var ellipse = new RectangleF(
                shadow.X + shadow.Width * (1f - k) / 2f,
                shadow.Y + shadow.Height * (1f - k) / 2f,
                shadow.Width * k, shadow.Height * k);
            using var brush = new SolidBrush(Color.FromArgb(14, 6, 4, 16));
            g.FillEllipse(brush, ellipse);
        }

        // ноги и обувь
        Box(4f, 19f, 3f, 7f, Pants);
        Box(9f, 19f, 3f, 7f, Pants);
        Box(6.55f, 19f, 0.45f, 7f, PantsShade);
        Box(11.55f, 19f, 0.45f, 7f, PantsShade);
        Box(4f, 26f, 3f, 2f, Shoes);
        Box(9f, 26f, 3f, 2f, Shoes);

        // туловище
        Box(4f, 9f, 8f, 10f, Shirt);
        Box(11.15f, 9f, 0.85f, 10f, ShirtShade);
        Box(4f, 18.3f, 8f, 0.7f, PantsShade);

        // руки
        Box(2f, 9f, 2f, 3.2f, Shirt);
        Box(2f, 12.2f, 2f, 5.6f, Skin);
        Box(2f, 16.6f, 2f, 1.2f, SkinShade);
        Box(12f, 9f, 2f, 3.2f, ShirtShade);
        Box(12f, 12.2f, 2f, 5.6f, SkinShade);
        Box(12f, 16.6f, 2f, 1.2f, SkinShade);

        // шея
        Box(6.7f, 8.2f, 2.6f, 0.9f, SkinShade);

        // голова в отдельном буфере: поворот не трогает контекст окна
        int side = Math.Max(16, (int)Math.Ceiling(10f * unit));
        using var head = new Bitmap(side, side);
        using (var hg = Graphics.FromImage(head))
        {
            hg.SmoothingMode = SmoothingMode.AntiAlias;
            float hu = side / 10f;

            void Cell(float x, float y, float w, float h, Color color)
            {
                using var brush = new SolidBrush(color);
                hg.FillRectangle(brush, (x - 3f) * hu, (y - 0f) * hu, w * hu, h * hu);
            }

            hg.TranslateTransform(side / 2f, 9f * hu);
            hg.RotateTransform(lookX * 6f);
            hg.TranslateTransform(-side / 2f, -9f * hu);

            Cell(4f, 1f, 8f, 8f, Skin);
            Cell(11.1f, 1f, 0.9f, 8f, SkinShade);
            Cell(4f, 8.2f, 8f, 0.8f, SkinShade);
            Cell(4f, 1f, 8f, 2f, Hair);
            Cell(4f, 3f, 1f, 1.8f, Hair);
            Cell(11f, 3f, 1f, 1.8f, Hair);
            Cell(4f, 1f, 8f, 0.5f, HairShade);
            Cell(4f, 1f, 0.8f, 2f, HairShade);

            var leftEye = new RectangleF(2f * hu, 4.2f * hu, 2f * hu, 1.7f * hu);
            var rightEye = new RectangleF(6f * hu, 4.2f * hu, 2f * hu, 1.7f * hu);
            using (var white = new SolidBrush(EyeWhite))
            {
                hg.FillRectangle(white, leftEye);
                hg.FillRectangle(white, rightEye);
            }

            if (Blinking)
            {
                using var lid = new SolidBrush(SkinShade);
                hg.FillRectangle(lid, leftEye.X, leftEye.Y + leftEye.Height * 0.35f, leftEye.Width, leftEye.Height * 0.4f);
                hg.FillRectangle(lid, rightEye.X, rightEye.Y + rightEye.Height * 0.35f, rightEye.Width, rightEye.Height * 0.4f);
            }
            else
            {
                float r = hu * 0.42f;
                float dx = Math.Clamp(lookX, -1f, 1f) * hu * 0.5f;
                float dy = Math.Clamp(lookY, -1f, 1f) * hu * 0.38f;
                using var pupil = new SolidBrush(PupilColor);
                hg.FillEllipse(pupil, leftEye.X + leftEye.Width / 2f + dx - r, leftEye.Y + leftEye.Height / 2f + dy - r, r * 2f, r * 2f);
                hg.FillEllipse(pupil, rightEye.X + rightEye.Width / 2f + dx - r, rightEye.Y + rightEye.Height / 2f + dy - r, r * 2f, r * 2f);
            }

            Cell(7.5f, 5.8f, 1.1f, 0.9f, SkinShade);
            Cell(6.6f, 7.2f, 2.8f, 0.45f, Mouth);
        }

        g.DrawImage(head, X(3f) + lookX * unit * 0.5f, Y(0f) + lookY * unit * 0.35f, 10f * unit, 10f * unit);
    }
}
