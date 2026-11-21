package dev.aurora.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.ui.AuroraBackdrop;
import dev.aurora.util.HotbarSmooth;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Хотбар в стиле Aurora: своя панель вместо ванильной текстуры
 * и плавно едущая рамка выделения слота.
 */
@Mixin(InGameHud.class)
public abstract class HotbarSmoothMixin {

    @Redirect(method = "renderHotbar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"),
            require = 0)
    private void aurora$hotbar(DrawContext context, RenderPipeline pipeline, Identifier texture,
                               int x, int y, int width, int height) {
        if (!HotbarSmooth.active || texture == null || pipeline == null) {
            context.drawGuiTexture(pipeline, texture, x, y, width, height);
            return;
        }

        String path = texture.getPath();

        if (path.endsWith("hud/hotbar")) {
            float w = width;
            float h = height;
            Render2D.roundedRect(context, x, y, w, h, 5f, 0xE00A1511);
            Render2D.roundedBorder(context, x, y, w, h, 5f, 1f,
                    ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.35f));
            for (int i = 1; i < 9; i++) {
                float lineX = x + i * 20f + 1f;
                context.fill(Math.round(lineX), y + 2, Math.round(lineX) + 1, y + height - 2, 0x1AFFFFFF);
            }
            return;
        }

        if (path.endsWith("hotbar_selection")) {
            int sx = HotbarSmooth.apply(x);
            Render2D.roundedRect(context, sx, y, width, height, 4f, 0xB016362A);
            Render2D.roundedBorder(context, sx, y, width, height, 4f, 1.4f,
                    ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.85f));
            Render2D.gradientH(context, sx + 3f, y + 2f, width - 6f, 1.6f,
                    ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.9f),
                    ColorUtil.withAlpha(AuroraBackdrop.ACCENT2, 0.25f));
            return;
        }

        context.drawGuiTexture(pipeline, texture, x, y, width, height);
    }
}
