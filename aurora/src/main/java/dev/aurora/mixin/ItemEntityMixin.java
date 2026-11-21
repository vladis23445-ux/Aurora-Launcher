package dev.aurora.mixin;

import dev.aurora.util.FlatItemsState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Убирает вращение дропнутых предметов, когда включён модуль FlatItems. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "getRotation", at = @At("RETURN"), cancellable = true, require = 0)
    private static void aurora$noSpin(float age, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (FlatItemsState.active) {
            cir.setReturnValue(0f);
        }
    }
}
