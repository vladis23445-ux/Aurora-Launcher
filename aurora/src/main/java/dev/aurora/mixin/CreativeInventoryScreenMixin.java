package dev.aurora.mixin;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.ui.AuroraBackdrop;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Вкладки категорий творческого инвентаря в стиле Aurora. */
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {

    @Inject(method = "renderTabIcon", at = @At("HEAD"), require = 0)
    private void aurora$tabPlate(DrawContext context, int x, int y, ItemGroup group, CallbackInfo ci) {
        Render2D.roundedRect(context, x - 6f, y - 7f, 28f, 30f, 5f, 0xE00A1511);
        Render2D.roundedBorder(context, x - 6f, y - 7f, 28f, 30f, 5f, 1f,
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.30f));
    }
}
