package dev.aurora.mixin;

import dev.aurora.ui.AuroraSplash;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Заменяет ванильный загрузочный экран на анимированный Aurora.
 *
 * Важно: ванильный progress обновляется внутри render(), который мы отменяем,
 * поэтому берём прогресс напрямую из ResourceReload и сглаживаем его сами.
 */
@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Shadow
    private ResourceReload reload;

    private static float auroraProgress;
    private static long completeSince = -1L;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        float target = 0f;
        boolean complete = false;
        try {
            target = Math.max(0f, Math.min(1f, reload.getProgress()));
            complete = reload.isComplete();
        } catch (Throwable ignored) {
        }

        // плавно подтягиваем отображаемое значение, чтобы полоса не дёргалась
        auroraProgress += (target - auroraProgress) * 0.25f;
        if (target >= 1f && auroraProgress > 0.995f) {
            auroraProgress = 1f;
        }

        if (complete) {
            auroraProgress = 1f;
            if (completeSince < 0L) {
                completeSince = System.currentTimeMillis();
            }
        } else {
            completeSince = -1L;
        }

        AuroraSplash.render(context, client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(), auroraProgress);
        ci.cancel();

        // Ванильный render сам снимает оверлей по завершении — мы его отменили,
        // поэтому закрываем сплэш вручную после короткой паузы.
        if (complete && completeSince > 0L && System.currentTimeMillis() - completeSince > 350L) {
            completeSince = -1L;
            auroraProgress = 0f;
            client.setOverlay(null);
        }
    }
}
