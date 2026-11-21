package dev.aurora.ui;

import dev.aurora.render.ColorUtil;
import dev.aurora.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Загрузочный экран Aurora вместо логотипа Mojang: живое сияние,
 * градиентный логотип и аккуратная полоса прогресса.
 */
public final class AuroraSplash {

    private AuroraSplash() {
    }

    public static void render(DrawContext context, int width, int height, float progress) {
        if (width <= 0 || height <= 0) {
            return;
        }

        AuroraBackdrop.render(context, width, height, 1f, 1f);
        context.fill(0, 0, width, height, 0x33040A08);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        String title = "AURORA";
        float scale = Math.max(2f, Math.min(4f, width / 220f));
        float titleWidth = 0f;
        for (int i = 0; i < title.length(); i++) {
            titleWidth += font.getWidth(String.valueOf(title.charAt(i))) + 1f;
        }
        titleWidth *= scale;

        float titleX = width / 2f - titleWidth / 2f;
        float titleY = height * 0.36f;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(titleX, titleY);
        matrices.scale(scale, scale);

        float cursor = 0f;
        int length = Math.max(1, title.length() - 1);
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int color = ColorUtil.lerp(0xFFE9F9F0, AuroraBackdrop.ACCENT2, i / (float) length);
            context.drawText(font, ch, Math.round(cursor) + 1, 1, 0x55000000, false);
            context.drawText(font, ch, Math.round(cursor), 0, color, false);
            cursor += font.getWidth(ch) + 1f;
        }
        matrices.popMatrix();

        String subtitle = "visual edition  ·  loading";
        Render2D.textCentered(context, subtitle, width / 2f, titleY + scale * 10f + 8f,
                ColorUtil.withAlpha(AuroraBackdrop.MINT, 0.65f));

        // прогресс
        float clamped = Math.max(0f, Math.min(1f, progress));
        float barWidth = Math.min(340f, width * 0.46f);
        float barHeight = 3.5f;
        float barX = width / 2f - barWidth / 2f;
        float barY = titleY + scale * 10f + 34f;

        Render2D.roundedRect(context, barX, barY, barWidth, barHeight, barHeight / 2f, 0x33FFFFFF);
        if (clamped > 0.002f) {
            Render2D.roundedGradient(context, barX, barY, barWidth * clamped, barHeight, barHeight / 2f,
                    ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.95f),
                    ColorUtil.withAlpha(AuroraBackdrop.ACCENT2, 0.95f));
        }

        String percent = Math.round(clamped * 100f) + "%";
        Render2D.textCentered(context, percent, width / 2f, barY + 10f,
                ColorUtil.withAlpha(AuroraBackdrop.MINT, 0.75f));

        // пульсирующая точка-индикатор
        float pulse = (float) ((Math.sin(System.currentTimeMillis() / 240.0) + 1.0) * 0.5);
        Render2D.circle(context, width / 2f, barY + 30f, 2.2f + pulse * 1.2f,
                ColorUtil.withAlpha(AuroraBackdrop.ACCENT, 0.25f + pulse * 0.45f));
    }
}
