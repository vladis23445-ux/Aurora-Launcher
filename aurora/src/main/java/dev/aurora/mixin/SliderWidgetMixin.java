package dev.aurora.mixin;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.ui.AuroraBackdrop;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Слайдеры (включая настройки графики/звука) в стиле Aurora. */
@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin {

    @Shadow
    protected double value;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$renderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ClickableWidget self = (ClickableWidget) (Object) this;
        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        boolean hovered = self.isHovered();

        Render2D.roundedRect(context, x, y, w, h, 5f,
                ColorUtil.withAlpha(hovered ? 0xC016362A : 0xC00E1A15, 0.92f));
        float fill = Math.max(1.5f, (float) (w * this.value));
        Render2D.roundedGradient(context, x, y, fill, h, 5f,
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.85f),
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT2, 0.45f));
        Render2D.roundedBorder(context, x, y, w, h, 5f, 1f,
                ColorUtil.withAlpha(hovered ? AuroraBackdrop.ACCENT : 0xFFFFFF, hovered ? 0.45f : 0.12f));

        String label = self.getMessage().getString();
        Render2D.textCentered(context, label, x + w / 2f, y + (h - 8) / 2f, 0xFFF2FBF6);

        ci.cancel();
    }
}
