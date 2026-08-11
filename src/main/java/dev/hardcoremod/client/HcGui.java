package dev.hardcoremod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class HcGui {
    public static final Identifier AMETHYST = Identifier.fromNamespaceAndPath("minecraft", "textures/block/amethyst_block.png");

    private HcGui() {
    }

    /**
     * Text is deferred via GuiRenderState and silently dropped when the scissor
     * stack is empty at submit time (GuiRenderState.findAppropriateNode returns
     * false when the text bounds are null). Push a full-screen scissor so text
     * always renders.
     */
    private static void pushFullScreenScissor(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        g.enableScissor(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    public static void drawText(GuiGraphics g, Font font, String text, int x, int y, int color) {
        pushFullScreenScissor(g);
        g.drawString(font, text, x, y, color, true);
        g.disableScissor();
    }

    public static void drawCenteredText(GuiGraphics g, Font font, String text, int x, int y, int color) {
        pushFullScreenScissor(g);
        g.drawCenteredString(font, text, x, y, color);
        g.disableScissor();
    }

    /**
     * Blur is only allowed once per frame in vanilla; when a mod screen opens in
     * the same frame another blurred screen closes, the game crashes with
     * "Can only blur once per frame". Guard it so the screen always renders.
     */
    public static void blurBackground(GuiGraphics g) {
        try {
            g.blurBeforeThisStratum();
        } catch (IllegalStateException ignored) {
        }
    }

    /** Tiled amethyst block background with a dark overlay. */
    public static void drawAmethystBg(GuiGraphics g, int width, int height) {
        for (int ty = 0; ty < height; ty += 16) {
            for (int tx = 0; tx < width; tx += 16) {
                int w = Math.min(16, width - tx);
                int h = Math.min(16, height - ty);
                g.blit(RenderPipelines.GUI_TEXTURED, AMETHYST, tx, ty, 0.0f, 0.0f, w, h, 16, 16);
            }
        }
        g.fill(0, 0, width, height, 0x8A101018);
    }

    /** Rounded-ish dark panel with accent border. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xDD1B1B2A);
        g.fill(x, y, x + w, y + 1, 0xFFBB86FC);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFBB86FC);
        g.fill(x, y, x + 1, y + h, 0xFFBB86FC);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFBB86FC);
    }

    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, float frac) {
        g.fill(x - 1, y - 1, x + w + 1, y + 6, 0xFF000000);
        g.fill(x, y, x + w, y + 4, 0xFF333344);
        if (frac > 0) {
            int fw = (int) (w * Math.min(1, frac));
            g.fill(x, y, x + fw, y + 4, 0xFFBB86FC);
        }
    }
}
