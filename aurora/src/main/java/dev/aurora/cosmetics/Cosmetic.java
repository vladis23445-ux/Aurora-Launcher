package dev.aurora.cosmetics;

import dev.aurora.util.Profile;

import java.util.ArrayList;
import java.util.List;

/** Один предмет косметики: плащ или корона. */
public record Cosmetic(String id, String name, Type type, int price, int topColor, int bottomColor, int accent) {

    public enum Type {
        CAPE,
        CROWN,
    }

    /** Ключ в profile.json: cape / crown. */
    public String key() {
        return type.name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isFree() {
        return price <= 0;
    }

    public static final List<Cosmetic> ALL = List.of(
            // плащи
            new Cosmetic("cape-aurora", "Aurora", Type.CAPE, 0, 0xFF7C5CFF, 0xFFFF5C9D, 0xFFFFFFFF),
            new Cosmetic("cape-ember", "Ember", Type.CAPE, 120, 0xFFFF8A3D, 0xFFC22B3A, 0xFFFFD8A8),
            new Cosmetic("cape-void", "Void", Type.CAPE, 200, 0xFF3A2E6E, 0xFF0B0A16, 0xFF9C8CFF),
            new Cosmetic("cape-mint", "Mint Wave", Type.CAPE, 260, 0xFF4BE3C8, 0xFF2A7BB8, 0xFFEAFFF8),
            new Cosmetic("cape-gilded", "Gilded", Type.CAPE, 420, 0xFFFFD98A, 0xFFB87A21, 0xFFFFF4CE),
            // короны
            new Cosmetic("crown-bronze", "Бронзовая корона", Type.CROWN, 150, 0xFFC98A4B, 0xFF7A4A22, 0xFFFFD7A1),
            new Cosmetic("crown-emerald", "Изумрудная корона", Type.CROWN, 320, 0xFF4BD9A0, 0xFF1B7A55, 0xFFCFFFE9),
            new Cosmetic("crown-obsidian", "Обсидиановая корона", Type.CROWN, 480, 0xFF4A3E74, 0xFF14101F, 0xFFB18CFF)
    );

    public static List<Cosmetic> byType(Type type) {
        List<Cosmetic> list = new ArrayList<>();
        for (Cosmetic cosmetic : ALL) {
            if (cosmetic.type() == type) {
                list.add(cosmetic);
            }
        }
        return list;
    }

    public static Cosmetic get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (Cosmetic cosmetic : ALL) {
            if (cosmetic.id().equals(id)) {
                return cosmetic;
            }
        }
        return null;
    }

    /** Надетый предмет данного типа или null. */
    public static Cosmetic equipped(Type type) {
        String id = Profile.equipped(type.name().toLowerCase(java.util.Locale.ROOT));
        return get(id);
    }
}
