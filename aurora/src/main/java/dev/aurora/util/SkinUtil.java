package dev.aurora.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Скин игрока. В 1.21.11 SkinTextures — это набор AssetInfo.TextureAsset,
 * поэтому путь к текстуре берётся через body().texturePath().
 */
public final class SkinUtil {

    private SkinUtil() {
    }

    public static Identifier headTexture(PlayerEntity player) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() == null) {
                return null;
            }
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry == null) {
                return null;
            }
            return entry.getSkinTextures().body().texturePath();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String initials(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        return name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }
}
