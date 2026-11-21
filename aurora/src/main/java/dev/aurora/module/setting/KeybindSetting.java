package dev.aurora.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public boolean isPressed() {
        int key = getValue();
        MinecraftClient client = MinecraftClient.getInstance();
        if (key == GLFW.GLFW_KEY_UNKNOWN || client.getWindow() == null) {
            return false;
        }
        return InputUtil.isKeyPressed(client.getWindow(), key);
    }

    public String getDisplay() {
        int key = getValue();
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "NONE";
        }
        try {
            String name = GLFW.glfwGetKeyName(key, 0);
            if (name != null) {
                return name.toUpperCase(java.util.Locale.ROOT);
            }
        } catch (Throwable ignored) {
        }
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "KEY " + key;
        };
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsInt());
        }
    }
}
