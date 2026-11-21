package dev.aurora.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aurora.Aurora;
import dev.aurora.module.Module;
import dev.aurora.module.ModuleManager;
import dev.aurora.module.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Система конфигов: именованные профили в config/aurora/&lt;name&gt;.json.
 * Старый одиночный config/aurora.json автоматически переносится в default.json.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String EXTENSION = ".json";

    private final ModuleManager modules;
    private final Path dir;
    private String current = "default";

    public ConfigManager(ModuleManager modules) {
        this.modules = modules;
        this.dir = FabricLoader.getInstance().getConfigDir().resolve(Aurora.MOD_ID);
    }

    public void load() {
        try {
            Files.createDirectories(dir);
            migrateLegacy();
        } catch (IOException e) {
            Aurora.LOGGER.error("Не удалось создать папку конфигов", e);
        }

        List<String> names = list();
        if (!names.contains(current)) {
            current = names.isEmpty() ? "default" : names.get(0);
        }
        load(current);
    }

    private void migrateLegacy() throws IOException {
        Path legacy = FabricLoader.getInstance().getConfigDir().resolve(Aurora.MOD_ID + EXTENSION);
        Path target = dir.resolve("default" + EXTENSION);
        if (Files.exists(legacy) && !Files.exists(target)) {
            Files.copy(legacy, target);
        }
    }

    public List<String> list() {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<String> names = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(EXTENSION))
                    .forEach(name -> names.add(name.substring(0, name.length() - EXTENSION.length())));
            names.sort(String::compareToIgnoreCase);
            return names;
        } catch (IOException e) {
            return List.of();
        }
    }

    public String current() {
        return current;
    }

    public Path getDir() {
        return dir;
    }

    public void load(String name) {
        Path file = dir.resolve(name + EXTENSION);
        if (!Files.exists(file)) {
            save(name);
            return;
        }

        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(raw, JsonObject.class);
            if (root == null || !root.has("modules")) {
                return;
            }

            JsonObject mods = root.getAsJsonObject("modules");
            for (Module module : modules.getModules()) {
                if (!mods.has(module.getName())) {
                    continue;
                }
                JsonObject entry = mods.getAsJsonObject(module.getName());
                if (entry.has("enabled")) {
                    module.setEnabled(entry.get("enabled").getAsBoolean());
                }
                if (entry.has("key")) {
                    try {
                        module.setKey(entry.get("key").getAsInt());
                    } catch (Throwable ignored) {
                    }
                }
                if (entry.has("settings")) {
                    JsonObject values = entry.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        JsonElement element = values.get(setting.getName());
                        if (element != null) {
                            try {
                                setting.fromJson(element);
                            } catch (Throwable t) {
                                Aurora.LOGGER.warn("Некорректная настройка {} в модуле {}",
                                        setting.getName(), module.getName());
                            }
                        }
                    }
                }
            }
            current = name;
        } catch (IOException | RuntimeException e) {
            Aurora.LOGGER.error("Не удалось загрузить конфиг " + name, e);
        }
    }

    public void save() {
        save(current);
    }

    public void save(String name) {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();

        for (Module module : modules.getModules()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.isEnabled());
            entry.addProperty("key", module.getKey());

            JsonObject values = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                try {
                    values.add(setting.getName(), setting.toJson());
                } catch (Throwable ignored) {
                }
            }
            entry.add("settings", values);
            mods.add(module.getName(), entry);
        }

        root.add("modules", mods);
        root.addProperty("configName", name);

        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + EXTENSION), GSON.toJson(root), StandardCharsets.UTF_8);
            current = name;
        } catch (IOException e) {
            Aurora.LOGGER.error("Не удалось сохранить конфиг " + name, e);
        }
    }

    /** Создаёт новый профиль на основе текущего состояния. */
    public String createNew() {
        int index = 1;
        while (Files.exists(dir.resolve("config-" + index + EXTENSION))) {
            index++;
        }
        String name = "config-" + index;
        save(name);
        return name;
    }

    public void delete(String name) {
        try {
            Files.deleteIfExists(dir.resolve(name + EXTENSION));
        } catch (IOException e) {
            Aurora.LOGGER.error("Не удалось удалить конфиг " + name, e);
        }

        if (name.equals(current)) {
            List<String> remaining = list();
            if (!remaining.isEmpty()) {
                load(remaining.get(0));
            } else {
                current = "default";
                save(current);
            }
        }
    }
}
