package pl.durex.client.gui.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public final class GuiRenderUtils {
    private GuiRenderUtils() {
    }

    public static void drawShadow(DrawContext context, int x, int y, int width, int height, int radius, int color, int layers) {
        for (int i = layers; i >= 1; i--) {
            int alpha = Math.max(1, ColorHelper.getAlpha(color) / (i * 2));
            drawRoundedRect(
                    context,
                    x - i,
                    y - i,
                    width + i * 2,
                    height + i * 2,
                    radius + i,
                    withAlpha(color, alpha)
            );
        }
    }

    public static void drawGlassBlur(DrawContext context, int x, int y, int width, int height, int radius) {
        context.enableScissor(x, y, x + width, y + height);
        for (int i = 0; i < 7; i++) {
            int inset = i * 2;
            int alpha = 30 - i * 3;
            drawRoundedRect(context, x + inset, y + inset, width - inset * 2, height - inset * 2, Math.max(2, radius - i), withAlpha(0xB4D7FF, alpha));
        }
        context.disableScissor();
    }

    public static void drawRoundedOutline(DrawContext context, int x, int y, int width, int height, int radius, int thickness, int color) {
        for (int i = 0; i < thickness; i++) {
            drawRoundedRect(context, x + i, y + i, width - i * 2, height - i * 2, Math.max(1, radius - i), color);
            drawRoundedRect(context, x + i + 1, y + i + 1, width - i * 2 - 2, height - i * 2 - 2, Math.max(1, radius - i - 1), withAlpha(color, 0));
        }
    }

    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int clampedRadius = Math.min(radius, Math.min(width, height) / 2);
        if (clampedRadius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + clampedRadius, y, x + width - clampedRadius, y + height, color);
        context.fill(x, y + clampedRadius, x + width, y + height - clampedRadius, color);

        for (int offset = 0; offset < clampedRadius; offset++) {
            double dy = clampedRadius - offset - 0.5;
            int inset = Math.max(0, clampedRadius - (int) Math.floor(Math.sqrt(clampedRadius * clampedRadius - dy * dy)));

            context.fill(x + inset, y + offset, x + width - inset, y + offset + 1, color);
            context.fill(x + inset, y + height - offset - 1, x + width - inset, y + height - offset, color);
        }
    }

    public static int mixColor(int from, int to, float delta) {
        float clamped = MathHelper.clamp(delta, 0.0F, 1.0F);
        int a = (int) MathHelper.lerp(clamped, ColorHelper.getAlpha(from), ColorHelper.getAlpha(to));
        int r = (int) MathHelper.lerp(clamped, ColorHelper.getRed(from), ColorHelper.getRed(to));
        int g = (int) MathHelper.lerp(clamped, ColorHelper.getGreen(from), ColorHelper.getGreen(to));
        int b = (int) MathHelper.lerp(clamped, ColorHelper.getBlue(from), ColorHelper.getBlue(to));
        return ColorHelper.getArgb(a, r, g, b);
    }

    public static int withAlpha(int color, int alpha) {
        return ColorHelper.getArgb(MathHelper.clamp(alpha, 0, 255), ColorHelper.getRed(color), ColorHelper.getGreen(color), ColorHelper.getBlue(color));
    }

    public static float animate(float current, float target, float speed) {
        return current + (target - current) * MathHelper.clamp(speed, 0.0F, 1.0F);
    }
}
