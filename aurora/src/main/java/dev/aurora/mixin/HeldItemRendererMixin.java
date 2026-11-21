package dev.aurora.mixin;

import dev.aurora.Aurora;
import dev.aurora.modules.CustomHand;
import dev.aurora.modules.ItemSwitch;
import dev.aurora.util.HeldShaderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom Hand.
 *
 * В 1.21.11 у HeldItemRenderer два перегруженных renderItem, поэтому имя метода
 * указано вместе с полным дескриптором — иначе миксин цепляется за overload
 * с LivingEntity/ItemStack и падает с InvalidInjectionException.
 * require = 0: если сигнатура в маппингах снова изменится, игра не упадёт,
 * просто пропадёт эффект руки.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    private static final String RENDER_ITEM =
            "renderItem(FLnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
                    + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V";

    private static final String RENDER_ITEMS_IN_HAND =
            "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;"
                    + "Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";

    @Inject(method = RENDER_ITEMS_IN_HAND, at = @At("HEAD"), require = 0)
    private void aurora$handPassOn(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                   MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                   CallbackInfo ci) {
        HeldShaderState.inHandPass = true;
    }

    @Inject(method = RENDER_ITEMS_IN_HAND, at = @At("RETURN"), require = 0)
    private void aurora$handPassOff(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                    MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                    CallbackInfo ci) {
        HeldShaderState.inHandPass = false;
    }

    @Inject(method = RENDER_ITEM, at = @At("HEAD"), require = 0)
    private void aurora$transformHand(float tickDelta, MatrixStack matrices,
                                      OrderedRenderCommandQueue queue,
                                      ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (Aurora.get() == null) {
            return;
        }
        CustomHand module = Aurora.get().getModules().get(CustomHand.class);
        if (module != null && module.isEnabled()) {
            module.applyTransform(matrices);
        }

        ItemSwitch itemSwitch = Aurora.get().getModules().get(ItemSwitch.class);
        if (itemSwitch != null && itemSwitch.isEnabled()) {
            itemSwitch.apply(matrices);
        }
    }
}
