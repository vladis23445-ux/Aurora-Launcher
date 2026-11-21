package dev.aurora.mixin;

import dev.aurora.Aurora;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD-слой рисуем в самом конце ванильного HUD.
 * Миксин выбран вместо HudRenderCallback намеренно: в 1.21.6+ HUD-API Fabric
 * переехал на HudLayerRegistrationCallback, а точка TAIL стабильна.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void aurora$renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Aurora.get() == null) {
            return;
        }
        try {
            Aurora.get().onHudRender(context, 1f);
        } catch (Throwable ignored) {
        }
    }
}
