namespace AuroraLauncher.Core;

public enum CosmeticType
{
    Cape,
    Crown,
}

/// <summary>Предмет косметики из магазина.</summary>
public sealed class Cosmetic
{
    public required string Id { get; init; }
    public required string Name { get; init; }
    public required CosmeticType Type { get; init; }
    public int Price { get; init; }
    public Color Top { get; init; }
    public Color Bottom { get; init; }
    public Color Accent { get; init; }

    public bool IsFree => Price <= 0;

    public string Key => Type == CosmeticType.Cape ? "cape" : "crown";

    public string TypeName => Type == CosmeticType.Cape ? "Плащ" : "Корона";
}

/// <summary>Каталог косметики — совпадает с тем, что рисует мод.</summary>
public static class Cosmetics
{
    public static readonly List<Cosmetic> All = new()
    {
        new Cosmetic
        {
            Id = "cape-aurora", Name = "Aurora", Type = CosmeticType.Cape, Price = 0,
            Top = Color.FromArgb(0x7C, 0x5C, 0xFF), Bottom = Color.FromArgb(0xFF, 0x5C, 0x9D),
            Accent = Color.White,
        },
        new Cosmetic
        {
            Id = "cape-ember", Name = "Ember", Type = CosmeticType.Cape, Price = 120,
            Top = Color.FromArgb(0xFF, 0x8A, 0x3D), Bottom = Color.FromArgb(0xC2, 0x2B, 0x3A),
            Accent = Color.FromArgb(0xFF, 0xD8, 0xA8),
        },
        new Cosmetic
        {
            Id = "cape-void", Name = "Void", Type = CosmeticType.Cape, Price = 200,
            Top = Color.FromArgb(0x3A, 0x2E, 0x6E), Bottom = Color.FromArgb(0x0B, 0x0A, 0x16),
            Accent = Color.FromArgb(0x9C, 0x8C, 0xFF),
        },
        new Cosmetic
        {
            Id = "cape-mint", Name = "Mint Wave", Type = CosmeticType.Cape, Price = 260,
            Top = Color.FromArgb(0x4B, 0xE3, 0xC8), Bottom = Color.FromArgb(0x2A, 0x7B, 0xB8),
            Accent = Color.FromArgb(0xEA, 0xFF, 0xF8),
        },
        new Cosmetic
        {
            Id = "cape-gilded", Name = "Gilded", Type = CosmeticType.Cape, Price = 420,
            Top = Color.FromArgb(0xFF, 0xD9, 0x8A), Bottom = Color.FromArgb(0xB8, 0x7A, 0x21),
            Accent = Color.FromArgb(0xFF, 0xF4, 0xCE),
        },
        new Cosmetic
        {
            Id = "crown-bronze", Name = "Бронзовая", Type = CosmeticType.Crown, Price = 150,
            Top = Color.FromArgb(0xC9, 0x8A, 0x4B), Bottom = Color.FromArgb(0x7A, 0x4A, 0x22),
            Accent = Color.FromArgb(0xFF, 0xD7, 0xA1),
        },
        new Cosmetic
        {
            Id = "crown-emerald", Name = "Изумрудная", Type = CosmeticType.Crown, Price = 320,
            Top = Color.FromArgb(0x4B, 0xD9, 0xA0), Bottom = Color.FromArgb(0x1B, 0x7A, 0x55),
            Accent = Color.FromArgb(0xCF, 0xFF, 0xE9),
        },
        new Cosmetic
        {
            Id = "crown-obsidian", Name = "Обсидиановая", Type = CosmeticType.Crown, Price = 480,
            Top = Color.FromArgb(0x4A, 0x3E, 0x74), Bottom = Color.FromArgb(0x14, 0x10, 0x1F),
            Accent = Color.FromArgb(0xB1, 0x8C, 0xFF),
        },
    };

    public static IEnumerable<Cosmetic> ByType(CosmeticType type) =>
        All.Where(c => c.Type == type);

    public static Cosmetic? Get(string? id) =>
        string.IsNullOrEmpty(id) ? null : All.FirstOrDefault(c => c.Id == id);

    public static Cosmetic? Equipped(ProfileData data, CosmeticType type) =>
        Get(data.EquippedOf(type == CosmeticType.Cape ? "cape" : "crown"));
}
