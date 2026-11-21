package dev.aurora.mixin;

import dev.aurora.util.FlatItemsState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FlatItems: дропнутый предмет укладывается плашмя на землю —
 * карточка поворачивается на 90° вокруг X и чуть приподнимается над блоком.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void aurora$flatten(net.minecraft.entity.ItemEntity entity, ItemEntityRenderState state,
                                float tickDelta, CallbackInfo ci) {
        if (FlatItemsState.active) {
            state.age = 0f;
            state.uniqueOffset = 0f;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"), require = 0)
    private void aurora$pushFlat(ItemEntityRenderState state, MatrixStack matrices,
                                 OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (!FlatItemsState.active) {
            return;
        }
        matrices.push();
        matrices.translate(0f, 0.06f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("RETURN"), require = 0)
    private void aurora$popFlat(ItemEntityRenderState state, MatrixStack matrices,
                                OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (FlatItemsState.active) {
            matrices.pop();
        }
    }
}
