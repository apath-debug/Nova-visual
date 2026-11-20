package dev.novastyles.gui;

import dev.novastyles.config.Category;
import dev.novastyles.config.Setting;
import dev.novastyles.config.Settings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.novastyles.gui.Draw.lerp;
import static dev.novastyles.gui.Draw.mulA;
import static dev.novastyles.gui.Draw.rrect;
import static dev.novastyles.gui.Draw.withA;

/**
 * Right Shift menu. Everything is drawn with plain fills (no blur, no textures),
 * mouse is polled from GLFW so the screen does not depend on the 1.21.9+ input event classes.
 */
public class StylesScreen extends Screen {
    private static final int W = 380, H = 240, SIDE = 96, ROW_H = 24, GAP = 4, PAD = 10;

    private static final int BG = 0xF0121216;
    private static final int SIDE_BG = 0xFF0E0E12;
    private static final int ROW_BG = 0xFF1A1A20;
    private static final int ROW_HOVER = 0xFF22222A;
    private static final int TRACK = 0xFF3A3A44;
    private static final int TEXT = 0xFFE6E6EA;
    private static final int TEXT_DIM = 0xFF8A8A96;
    private static final int WHITE = 0xFFFFFFFF;

    private int category = 0;
    private float open = 0f;
    private float scroll = 0f, scrollTarget = 0f;
    private long lastNanos = System.nanoTime();
    private boolean prevLeft, prevRight;
    private Setting dragging;
    private final Map<Setting, Float> anims = new IdentityHashMap<>();
    private final float[] tabHover = new float[Settings.CATEGORIES.size()];

    public StylesScreen() {
        super(Text.literal("Nova Styles"));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty: skips the vanilla background blur, which is expensive.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float k = Settings.ANIMATIONS.value ? 1f - (float) Math.exp(-dt * 18f) : 1f;

        open += (1f - open) * k;
        if (open > 0.995f) open = 1f;
        float alpha = open;

        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean ready = open > 0.9f;
        boolean clickL = ready && left && !prevLeft;
        boolean clickR = ready && right && !prevRight;
        prevLeft = left;
        prevRight = right;
        if (!left) dragging = null;

        List<Category> cats = Settings.CATEGORIES;
        int accent = Settings.accent();
        int x0 = (width - W) / 2;
        int y0 = (height - H) / 2 + Math.round((1f - alpha) * 12f);
        int x1 = x0 + W, y1 = y0 + H;

        // Backdrop, glow border, panel, sidebar
        ctx.fill(0, 0, width, height, mulA(0x70000000, alpha));
        rrect(ctx, x0 - 1, y0 - 1, x1 + 1, y1 + 1, 5, mulA(withA(accent, 0x44), alpha));
        rrect(ctx, x0, y0, x1, y1, 4, mulA(BG, alpha));
        rrect(ctx, x0, y0, x0 + SIDE, y1, 4, mulA(SIDE_BG, alpha));
        ctx.fill(x0 + SIDE - 4, y0, x0 + SIDE, y1, mulA(SIDE_BG, alpha));

        // Title
        ctx.drawText(textRenderer, "NOVA", x0 + 12, y0 + 12, mulA(accent, alpha), false);
        ctx.drawText(textRenderer, "STYLES", x0 + 12 + textRenderer.getWidth("NOVA "), y0 + 12, mulA(TEXT, alpha), false);

        // Tabs
        int ty = y0 + 36;
        for (int i = 0; i < cats.size(); i++) {
            int tx1 = x0 + 8, tx2 = x0 + SIDE - 8, tyb = ty + 22;
            boolean hov = mouseX >= tx1 && mouseX < tx2 && mouseY >= ty && mouseY < tyb;
            boolean sel = i == category;
            tabHover[i] += ((hov || sel ? 1f : 0f) - tabHover[i]) * k;
            if (hov && clickL && !sel) {
                category = i;
                scroll = 0f;
                scrollTarget = 0f;
                sel = true;
            }
            int bg = sel ? withA(accent, 0x40) : withA(0xFFFFFF, Math.round(0x14 * tabHover[i]));
            rrect(ctx, tx1, ty, tx2, tyb, 3, mulA(bg, alpha));
            if (sel) rrect(ctx, tx1, ty + 4, tx1 + 2, tyb - 4, 1, mulA(accent, alpha));
            int tc = lerp(TEXT_DIM, TEXT, tabHover[i]);
            ctx.drawText(textRenderer, cats.get(i).name(), tx1 + 10, ty + (22 - 8) / 2, mulA(tc, alpha), false);
            ty += 26;
        }

        // Footer hints
        ctx.drawText(textRenderer, "LMB: next / toggle", x0 + 10, y1 - 30, mulA(TEXT_DIM, alpha), false);
        ctx.drawText(textRenderer, "RMB: prev / reset", x0 + 10, y1 - 19, mulA(TEXT_DIM, alpha), false);

        // Content
        Category cat = cats.get(category);
        int cx1 = x0 + SIDE + PAD, cx2 = x1 - PAD;
        ctx.drawText(textRenderer, cat.name(), cx1, y0 + 12, mulA(TEXT, alpha), false);
        ctx.fill(cx1, y0 + 25, cx1 + 22, y0 + 26, mulA(accent, alpha));

        int top = y0 + 34, bottom = y1 - PAD;
        int viewH = bottom - top;
        int contentH = cat.settings().size() * (ROW_H + GAP) - GAP;
        float maxScroll = Math.max(0, contentH - viewH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * k;

        ctx.enableScissor(cx1 - 2, top, cx2 + 2, bottom);
        int ry = top - Math.round(scroll);
        for (Setting s : cat.settings()) {
            drawRow(ctx, s, cx1, ry, cx2, mouseX, mouseY, clickL, clickR, left, k, alpha, accent, top, bottom);
            ry += ROW_H + GAP;
        }
        ctx.disableScissor();
    }

    private void drawRow(DrawContext ctx, Setting s, int x1, int y, int x2, int mx, int my,
                         boolean clickL, boolean clickR, boolean left, float k, float alpha,
                         int accent, int top, int bottom) {
        int y2 = y + ROW_H;
        boolean hov = mx >= x1 && mx < x2 && my >= Math.max(y, top) && my < Math.min(y2, bottom);
        rrect(ctx, x1, y, x2, y2, 3, mulA(hov ? ROW_HOVER : ROW_BG, alpha));
        int textY = y + (ROW_H - 8) / 2;
        ctx.drawText(textRenderer, s.label, x1 + 8, textY, mulA(TEXT, alpha), false);
        int rx = x2 - 8;

        if (s instanceof Setting.Bool b) {
            if (hov && clickL) b.value = !b.value;
            float target = b.value ? 1f : 0f;
            float a = anims.getOrDefault(b, target);
            a += (target - a) * k;
            anims.put(b, a);

            int tw = 26, th = 12;
            int tx = rx - tw, ty = y + (ROW_H - th) / 2;
            rrect(ctx, tx, ty, tx + tw, ty + th, 6, mulA(lerp(TRACK, accent, a), alpha));
            int kx = tx + 2 + Math.round(a * (tw - 4 - 8));
            rrect(ctx, kx, ty + 2, kx + 8, ty + 10, 4, mulA(WHITE, alpha));

        } else if (s instanceof Setting.Mode m) {
            if (hov && clickL) m.cycle(1);
            if (hov && clickR) m.cycle(-1);
            String v = m.current();
            int gt = textRenderer.getWidth(">");
            int lt = textRenderer.getWidth("<");
            int vw = textRenderer.getWidth(v);
            int gx = rx - gt;
            int vx = gx - 6 - vw;
            int lx = vx - 6 - lt;
            ctx.drawText(textRenderer, ">", gx, textY, mulA(TEXT_DIM, alpha), false);
            ctx.drawText(textRenderer, v, vx, textY, mulA(accent, alpha), false);
            ctx.drawText(textRenderer, "<", lx, textY, mulA(TEXT_DIM, alpha), false);

        } else if (s instanceof Setting.Num n) {
            if (hov && clickR) n.reset();
            String txt = String.format(Locale.ROOT, "%.2f", n.value);
            int tw = textRenderer.getWidth(txt);
            int trackX2 = rx - tw - 8;
            int trackX1 = trackX2 - 90;
            int trackY = y + ROW_H / 2 - 2;

            boolean overTrack = mx >= trackX1 - 3 && mx <= trackX2 + 3
                    && my >= Math.max(y + 3, top) && my < Math.min(y2 - 3, bottom);
            if (overTrack && clickL) dragging = n;
            if (dragging == n && left) {
                float t = Math.max(0f, Math.min(1f, (mx - trackX1) / (float) (trackX2 - trackX1)));
                n.setClamped(n.min + t * (n.max - n.min));
            }

            float frac = (n.value - n.min) / (n.max - n.min);
            int fx = trackX1 + Math.round(frac * (trackX2 - trackX1));
            rrect(ctx, trackX1, trackY, trackX2, trackY + 4, 2, mulA(TRACK, alpha));
            rrect(ctx, trackX1, trackY, Math.max(trackX1 + 2, fx), trackY + 4, 2, mulA(accent, alpha));
            rrect(ctx, fx - 3, trackY - 2, fx + 3, trackY + 6, 3, mulA(WHITE, alpha));
            ctx.drawText(textRenderer, txt, rx - tw, textY, mulA(TEXT_DIM, alpha), false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float) verticalAmount * 18f;
        return true;
    }

    @Override
    public void removed() {
        Settings.save();
        super.removed();
    }
}
