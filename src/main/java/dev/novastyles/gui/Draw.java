package dev.novastyles.gui;

import net.minecraft.client.gui.DrawContext;

/** Tiny immediate-mode helpers: rounded rects and color math. No textures, no shaders, no allocations. */
final class Draw {
    private Draw() { }

    static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x1, y1, x2, y2, color);
            return;
        }
        c.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            c.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    static int mulA(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0xFFFFFF);
    }

    static int withA(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = ch(c1, 24, c2, t), r = ch(c1, 16, c2, t), g = ch(c1, 8, c2, t), b = ch(c1, 0, c2, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int ch(int c1, int shift, int c2, float t) {
        int a = (c1 >>> shift) & 0xFF, b = (c2 >>> shift) & 0xFF;
        return Math.round(a + (b - a) * t);
    }
}
