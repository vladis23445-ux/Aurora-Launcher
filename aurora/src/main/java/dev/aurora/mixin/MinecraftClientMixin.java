package dev.aurora.mixin;

import dev.aurora.ui.AuroraMainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Подменяет ванильный TitleScreen на наше меню в стиле Aurora.
 * Работает и при старте игры, и при выходе из мира.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen aurora$replaceTitleScreen(Screen screen) {
        if (screen instanceof TitleScreen) {
            return new AuroraMainMenu();
        }
        return screen;
    }
}
