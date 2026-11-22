package dev.aurora.mixin;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import dev.aurora.ui.AuroraBackdrop;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Все ванильные кнопки (настройки, выбор мира, сеть, пауза) рисуются
 * в нашем стиле: скруглённые плашки, акцентная рамка, свой текст.
 */
@Mixin(PressableWidget.class)
public class PressableWidgetMixin {

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$renderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PressableWidget self = (PressableWidget) (Object) this;

        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // кнопки-иконки (книга рецептов, стрелки, режимы) не перекрываем:
        // у них нет текста, а иконку рисует ваниль
        if (self.getMessage().getString().isEmpty()) {
            return;
        }

        boolean hovered = self.isHovered();
        boolean active = self.active;

        int fill;
        if (!active) {
            fill = 0x600A0F0C;
        } else if (hovered) {
            fill = 0xB8153F2E;
        } else {
            fill = 0xA00E1A15;
        }

        int border = !active
                ? ColorUtil.withAlpha(0xFFFFFF, 0.06f)
                : ColorUtil.withAlpha(hovered ? AuroraBackdrop.ACCENT : 0xFFFFFF, hovered ? 0.45f : 0.14f);

        Render2D.roundedRect(context, x, y, w, h, 5f, fill);
        Render2D.roundedBorder(context, x, y, w, h, 5f, 1f, border);

        String label = self.getMessage().getString();
        int textColor = active
                ? (hovered ? 0xFFFFFFFF : 0xFFE9F9F0)
                : 0xFF6B7C74;
        Render2D.textCentered(context, label, x + w / 2f, y + (h - 8) / 2f, textColor);

        ci.cancel();
    }
}
