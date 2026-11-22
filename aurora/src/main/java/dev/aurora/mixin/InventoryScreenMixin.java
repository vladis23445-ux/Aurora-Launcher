package dev.aurora.mixin;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.ui.AuroraBackdrop;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Инвентарь в стиле Aurora: своя панель и рамки слотов вместо ванильной текстуры. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {


    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$panel(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        net.minecraft.client.gui.screen.Screen self = (net.minecraft.client.gui.screen.Screen) (Object) this;
        int x = (self.width - 176) / 2;
        int y = (self.height - 166) / 2;

        Render2D.roundedRect(context, x, y, 176f, 166f, 7f, 0x8A0A1511);
        Render2D.roundedBorder(context, x, y, 176f, 166f, 7f, 1.2f,
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.55f));
        Render2D.gradientH(context, x, y, 176f, 1.4f,
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.55f),
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT2, 0.15f));

        int slotColor = 0x33E8FFF4;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(context, x + 8 + col * 18, y + 84 + row * 18, slotColor);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(context, x + 8 + col * 18, y + 142, slotColor);
        }
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 2; row++) {
                slot(context, x + 98 + col * 18, y + 18 + row * 18, slotColor);
            }
        }
        slot(context, x + 154, y + 28, slotColor);
        for (int row = 0; row < 4; row++) {
            slot(context, x + 8, y + 8 + row * 18, slotColor);
        }
        slot(context, x + 77, y + 62, slotColor);

        ci.cancel();
    }

    private static void slot(DrawContext context, int sx, int sy, int color) {
        Render2D.roundedRect(context, sx, sy, 18f, 18f, 3f, 0x2A000000);
        Render2D.roundedBorder(context, sx, sy, 18f, 18f, 3f, 1f, color);
    }
}
