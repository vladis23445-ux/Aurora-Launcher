package dev.aurora.mixin;

import dev.aurora.util.FullbrightState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Убирает «темноту» (эффект Warden) при включённом Fullbright. */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Inject(method = "getDarkness", at = @At("RETURN"), cancellable = true, require = 0)
    private void aurora$noDarkness(LivingEntity entity, float factor, float tickDelta,
                                   CallbackInfoReturnable<Float> cir) {
        if (FullbrightState.active) {
            cir.setReturnValue(0f);
        }
    }
}
