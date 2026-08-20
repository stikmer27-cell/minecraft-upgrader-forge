package net.execheinz.upgrader.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class CasinoRenderer {
    private CasinoRenderer() {}

    static void roundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        roundedGradient(graphics, x, y, width, height, radius, color, color);
    }

    static void roundedGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, int top, int bottom) {
        if (width <= 0 || height <= 0) return;
        radius = Mth.clamp(radius, 0, Math.min(width, height) / 2);
        if (radius == 0) {
            graphics.fillGradient(x, y, x + width, y + height, top, bottom);
            return;
        }

        // The old implementation submitted one draw call per pixel row. A large
        // picker panel therefore cost hundreds of draw calls every frame. Keep
        // exact rounded corners, but render the rectangular body as gradients
        // and only submit the small corner bands row by row.
        graphics.fillGradient(x + radius, y, x + width - radius, y + height, top, bottom);
        int middleTop = lerpColor(top, bottom, (float) radius / Math.max(1, height - 1));
        int middleBottom = lerpColor(top, bottom, (float) (height - radius) / Math.max(1, height - 1));
        graphics.fillGradient(x, y + radius, x + width, y + height - radius, middleTop, middleBottom);
        for (int row = 0; row < radius; row++) {
            int inset = cornerInset(row, height, radius);
            int topColor = lerpColor(top, bottom, (float) row / Math.max(1, height - 1));
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, topColor);
            int bottomRow = height - 1 - row;
            int bottomInset = cornerInset(bottomRow, height, radius);
            int bottomColor = lerpColor(top, bottom, (float) bottomRow / Math.max(1, height - 1));
            graphics.fill(x + bottomInset, y + bottomRow, x + width - bottomInset, y + bottomRow + 1, bottomColor);
        }
    }

    static void card(GuiGraphics graphics, int x, int y, int width, int height, int radius, int border, int top, int bottom) {
        roundedRect(graphics, x, y, width, height, radius, border);
        roundedGradient(graphics, x + 1, y + 1, width - 2, height - 2, Math.max(1, radius - 1), top, bottom);
    }

    static void raisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int radius,
                            int border, int top, int bottom, int accent) {
        roundedRect(graphics, x + 1, y + 3, width - 2, height, radius, 0x90000000);
        roundedRect(graphics, x - 1, y - 1, width + 2, height + 2, radius + 1, 0x24273649);
        card(graphics, x, y, width, height, radius, border, top, bottom);
        graphics.fill(x + radius + 2, y + 2, x + width - radius - 2, y + 3, 0x385E7190);
        graphics.fill(x + radius + 3, y + height - 3, x + width - radius - 3, y + height - 2, accent);
    }

    static void accentRail(GuiGraphics graphics, int x, int y, int width, int color) {
        graphics.fill(x, y, x + width, y + 1, 0x40101620);
        graphics.fill(x + 7, y + 1, x + width - 7, y + 2, color);
        graphics.fill(x + 15, y + 2, x + width - 15, y + 3, color & 0x55FFFFFF);
    }

    static void chevrons(GuiGraphics graphics, int x, int y, int count, int color) {
        for (int i = 0; i < count; i++) {
            int left = x + i * 7;
            graphics.fill(left, y, left + 3, y + 2, color);
            graphics.fill(left + 2, y + 2, left + 5, y + 4, color);
            graphics.fill(left, y + 4, left + 3, y + 6, color);
        }
    }

    static void cornerBrackets(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        int length = Math.min(14, Math.min(width, height) / 3);
        graphics.fill(x, y, x + length, y + 1, color);
        graphics.fill(x, y, x + 1, y + length, color);
        graphics.fill(x + width - length, y, x + width, y + 1, color);
        graphics.fill(x + width - 1, y, x + width, y + length, color);
        graphics.fill(x, y + height - 1, x + length, y + height, color);
        graphics.fill(x, y + height - length, x + 1, y + height, color);
        graphics.fill(x + width - length, y + height - 1, x + width, y + height, color);
        graphics.fill(x + width - 1, y + height - length, x + width, y + height, color);
    }

    static void diagonalWatermark(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        int usable = Math.min(width - 10, height - 10);
        int stripes = Math.max(2, Math.min(6, width / 24));
        int spacing = Math.max(17, (width + usable) / stripes);
        for (int stripe = 0; stripe < stripes; stripe++) {
            int offset = -usable + stripe * spacing;
            for (int step = 0; step < usable; step += 7) {
                int px = x + offset + step;
                int py = y + height - 6 - step;
                if (px >= x + 5 && px + 7 < x + width - 5 && py >= y + 5 && py < y + height - 5) {
                    graphics.fill(px, py, px + 7, py + 1, color);
                }
            }
        }
    }

    static void glow(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        for (int spread = 5; spread >= 1; spread--) {
            int alpha = Math.max(4, ((color >>> 24) & 0xFF) / (spread + 2));
            int faded = (alpha << 24) | (color & 0x00FFFFFF);
            roundedRect(graphics, x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread, faded);
        }
    }

    static void disc(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        roundedRect(graphics, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    static void arc(GuiGraphics graphics, int cx, int cy, int innerRadius, int outerRadius, float startDegrees, float sweepDegrees, int color) {
        if (sweepDegrees <= 0.0F) return;
        float end = startDegrees + Math.min(360.0F, sweepDegrees);
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0F);
        for (float angle = startDegrees; angle < end; angle += 2.0F) {
            float segment = Math.min(2.35F, end - angle + 0.35F);
            graphics.pose().pushPose();
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle + segment * 0.5F));
            graphics.fill(innerRadius, -2, outerRadius, 2, color);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    static void tickRing(GuiGraphics graphics, int cx, int cy, int innerRadius, int outerRadius, int color, int majorColor) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0F);
        for (int i = 0; i < 72; i++) {
            graphics.pose().pushPose();
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(i * 5.0F));
            int inner = i % 6 == 0 ? innerRadius - 2 : innerRadius;
            graphics.fill(inner, -1, outerRadius, 1, i % 6 == 0 ? majorColor : color);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    static void needle(GuiGraphics graphics, int cx, int cy, float angleDegrees, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 10.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDegrees));
        // Keep the moving marker on the outer scale. Its stem must not run
        // through the chance and status captions in the wheel centre.
        glow(graphics, 31, -2, 10, 4, 2, 0x70FFD35A);
        roundedGradient(graphics, 30, -1, 12, 3, 1, 0xFFFFFFFF, color);
        graphics.fill(38, -3, 44, 4, color);
        graphics.fill(41, -5, 45, 6, color);
        graphics.pose().popPose();
    }

    static void hexGrid(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        for (int row = 0; row < height; row += 9) {
            int offset = (row / 9) % 2 == 0 ? 0 : 6;
            for (int col = -offset; col < width; col += 12) {
                graphics.fill(x + col, y + row, x + col + 5, y + row + 1, color);
                graphics.fill(x + col + 6, y + row + 4, x + col + 11, y + row + 5, color);
            }
        }
    }

    static int lerpColor(int from, int to, float amount) {
        amount = Mth.clamp(amount, 0.0F, 1.0F);
        int a = Mth.lerpInt(amount, from >>> 24 & 0xFF, to >>> 24 & 0xFF);
        int r = Mth.lerpInt(amount, from >>> 16 & 0xFF, to >>> 16 & 0xFF);
        int g = Mth.lerpInt(amount, from >>> 8 & 0xFF, to >>> 8 & 0xFF);
        int b = Mth.lerpInt(amount, from & 0xFF, to & 0xFF);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int cornerInset(int row, int height, int radius) {
        if (radius <= 0) return 0;
        int distance = Math.min(row, height - 1 - row);
        if (distance >= radius) return 0;
        double dy = radius - distance - 0.5D;
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(Math.max(0.0D, radius * radius - dy * dy))));
    }
}
