package dev.aurora.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Общий профиль игрока: монеты, счётчики заданий, купленная и надетая косметика.
 * Файл лежит рядом с лаунчером, поэтому лаунчер и игра видят одни и те же данные.
 */
public final class Profile {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = resolveFile();

    private static JsonObject root = new JsonObject();
    private static long lastSyncMs;
    private static long knownModified;
    private static boolean dirty;

    private Profile() {
    }

    private static Path resolveFile() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "AuroraLauncher", "profile.json");
        }
        return Paths.get(System.getProperty("user.home"), ".aurora-launcher", "profile.json");
    }

    public static Path file() {
        return FILE;
    }

    // ------------------------------------------------------------ загрузка

    /** Читает файл, если он изменился (лаунчер мог купить косметику). */
    public static synchronized void sync() {
        long now = System.currentTimeMillis();
        if (now - lastSyncMs < 500L) {
            return;
        }
        lastSyncMs = now;

        if (!Files.exists(FILE)) {
            return;
        }
        long modified;
        try {
            modified = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            return;
        }
        if (modified == knownModified) {
            return;
        }
        try {
            String text = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonElement parsed = GSON.fromJson(text, JsonElement.class);
            if (parsed != null && parsed.isJsonObject()) {
                root = parsed.getAsJsonObject();
                knownModified = modified;
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void save() {
        dirty = false;
        try {
            Path parent = FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
            knownModified = Files.getLastModifiedTime(FILE).toMillis();
        } catch (Exception ignored) {
        }
    }

    /** Сохраняет, только если что-то меняли. */
    public static synchronized void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    private static JsonObject counters() {
        JsonObject counters = root.getAsJsonObject("counters");
        if (counters == null) {
            counters = new JsonObject();
            root.add("counters", counters);
        }
        return counters;
    }

    private static JsonArray array(String name) {
        JsonArray array = root.getAsJsonArray(name);
        if (array == null) {
            array = new JsonArray();
            root.add(name, array);
        }
        return array;
    }

    private static JsonObject equipped() {
        JsonObject equipped = root.getAsJsonObject("equipped");
        if (equipped == null) {
            equipped = new JsonObject();
            root.add("equipped", equipped);
        }
        return equipped;
    }

    // ------------------------------------------------------------ монеты

    public static synchronized int coins() {
        sync();
        JsonElement element = root.get("coins");
        return element == null ? 0 : element.getAsInt();
    }

    public static synchronized void addCoins(int amount) {
        sync();
        root.addProperty("coins", Math.max(0, coins() + amount));
        dirty = true;
        save();
    }

    public static synchronized boolean spendCoins(int amount) {
        sync();
        if (coins() < amount) {
            return false;
        }
        root.addProperty("coins", coins() - amount);
        dirty = true;
        save();
        return true;
    }

    // ------------------------------------------------------------ счётчики

    public static synchronized double counter(String key) {
        JsonElement element = counters().get(key);
        return element == null ? 0.0 : element.getAsDouble();
    }

    public static synchronized void addCounter(String key, double amount) {
        sync();
        counters().addProperty(key, counter(key) + amount);
        dirty = true;
    }

    public static synchronized void setCounter(String key, double value) {
        counters().addProperty(key, value);
        dirty = true;
    }

    // ------------------------------------------------------------ задания

    public static synchronized boolean isClaimed(String questId) {
        sync();
        for (JsonElement element : array("claimed")) {
            if (element.isJsonPrimitive() && questId.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    /** Отмечает задание выполненным и начисляет награду. */
    public static synchronized boolean claim(String questId, int reward) {
        sync();
        if (isClaimed(questId)) {
            return false;
        }
        array("claimed").add(questId);
        root.addProperty("coins", Math.max(0, coins() + reward));
        dirty = true;
        save();
        return true;
    }

    public static synchronized List<String> claimedIds() {
        sync();
        List<String> ids = new ArrayList<>();
        for (JsonElement element : array("claimed")) {
            if (element.isJsonPrimitive()) {
                ids.add(element.getAsString());
            }
        }
        return ids;
    }

    // ------------------------------------------------------------ косметика

    public static synchronized boolean owns(String cosmeticId) {
        sync();
        for (JsonElement element : array("owned")) {
            if (element.isJsonPrimitive() && cosmeticId.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void own(String cosmeticId) {
        sync();
        if (owns(cosmeticId)) {
            return;
        }
        array("owned").add(cosmeticId);
        dirty = true;
        save();
    }

    /** Надетая косметика типа cape/crown, пустая строка — ничего не надето. */
    public static synchronized String equipped(String type) {
        sync();
        JsonElement element = equipped().get(type);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    public static synchronized void equip(String type, String cosmeticId) {
        sync();
        equipped().addProperty(type, cosmeticId == null ? "" : cosmeticId);
        dirty = true;
        save();
    }
}
