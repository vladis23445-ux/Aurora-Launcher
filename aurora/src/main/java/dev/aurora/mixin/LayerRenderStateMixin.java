package dev.aurora.mixin;

import dev.aurora.util.HeldShaderState;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HeldShader: перекрашивает квады модели предмета через массив tints.
 * Геометрия не меняется, поэтому заливка не может «отъехать» от предмета.
 */
@Mixin(ItemRenderState.LayerRenderState.class)
public abstract class LayerRenderStateMixin {

    @Shadow
    private int[] tints;

    private int[] aurora$savedTints;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void aurora$tintOn(CallbackInfo ci) {
        if (!HeldShaderState.active || !HeldShaderState.inHandPass) {
            return;
        }
        aurora$savedTints = tints;
        int color = HeldShaderState.argb;
        tints = new int[] { color, color, color, color, color, color };
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void aurora$tintOff(CallbackInfo ci) {
        if (aurora$savedTints != null) {
            tints = aurora$savedTints;
            aurora$savedTints = null;
        }
    }
}
