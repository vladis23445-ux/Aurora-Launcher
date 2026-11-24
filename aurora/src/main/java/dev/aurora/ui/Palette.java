package dev.aurora.ui;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Темы интерфейса мода: акцентные цвета меню и подсветок. */
public final class Palette {

    public enum Theme {
        VIOLET("Violet", 0xFF7C5CFF, 0xFFFF5C9D, 0xFFC3B4FF),
        EMERALD("Emerald", 0xFF3BE8A0, 0xFF40D6D6, 0xFF8CFFC8),
        CRIMSON("Crimson", 0xFFFF4D6D, 0xFFFFA24D, 0xFFFFC2C9),
        OCEAN("Ocean", 0xFF4C8DFF, 0xFF41E0E0, 0xFFBFE3FF),
        AMBER("Amber", 0xFFFFB03A, 0xFFFF6B3D, 0xFFFFE0B2);

        private final String title;
        private final int accent;
        private final int accent2;
        private final int mint;

        Theme(String title, int accent, int accent2, int mint) {
            this.title = title;
            this.accent = accent;
            this.accent2 = accent2;
            this.mint = mint;
        }

        public String title() {
            return title;
        }

        public int accent() {
            return accent;
        }

        public int accent2() {
            return accent2;
        }

        public int mint() {
            return mint;
        }
    }

    private static Theme current = Theme.VIOLET;

    private Palette() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aurora").resolve("theme.txt");
    }

    public static Theme current() {
        return current;
    }

    public static int accent() {
        return current.accent;
    }

    public static int accent2() {
        return current.accent2;
    }

    public static int mint() {
        return current.mint;
    }

    public static void next() {
        Theme[] values = Theme.values();
        set(values[(current.ordinal() + 1) % values.length]);
    }

    public static void set(Theme theme) {
        current = theme;
        save();
    }

    public static void load() {
        try {
            Path path = file();
            if (!Files.exists(path)) {
                return;
            }
            String raw = Files.readString(path, StandardCharsets.UTF_8).trim();
            for (Theme theme : Theme.values()) {
                if (theme.name().equalsIgnoreCase(raw)) {
                    current = theme;
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, current.name(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
