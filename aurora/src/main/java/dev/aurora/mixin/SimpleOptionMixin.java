package dev.aurora.mixin;

import dev.aurora.util.FullbrightState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Подменяет значение гаммы на чтении: ваниль ограничивает ползунок единицей,
 * поэтому записать большое значение в настройки нельзя — а вернуть можно.
 */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin {

    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true, require = 0)
    private void aurora$brightGamma(CallbackInfoReturnable<Object> cir) {
        if (!FullbrightState.active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        if ((Object) this == client.options.getGamma()) {
            cir.setReturnValue(FullbrightState.gamma);
        }
    }
}
