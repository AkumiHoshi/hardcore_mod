package dev.hardcoremod.client;

import dev.hardcoremod.Element;
import dev.hardcoremod.HcCombat;
import dev.hardcoremod.HcNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * HUD counters:
 * - Top-right: element-colored panel with icon + element name + accumulated damage,
 *   fading smoothly over 7 s, flashing on new hits.
 * - Top-left: "Tăng sát thương" combo stacks with a time-remaining bar.
 */
public final class DmgCounter {
    private static int lastStacks;
    private static int lastMaxStacks;
    private static long stacksExpire;

    private static float accTotal;
    private static String accElement = "";
    private static long accTick;
    private static int flashTicks;

    private DmgCounter() {
    }

    public static void receive(HcNetworking.DmgCounterS2C p) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        long now = client.level.getLevelData().getGameTime();
        if (p.stacks() > 0) {
            lastStacks = p.stacks();
            lastMaxStacks = p.maxStacks();
            stacksExpire = now + HcCombat.STACK_DURATION;
        }
        // Only player-attack packets carry a real total; damage-taken packets send 0.
        if (p.accTotal() > 0) {
            if (p.accTotal() > accTotal) flashTicks = 7;
            accTotal = p.accTotal();
            accElement = p.elementId();
            accTick = now;
        }
    }

    public static void renderHud(GuiGraphics graphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        long now = client.level.getLevelData().getGameTime();
        var font = client.font;
        int sw = client.getWindow().getGuiScaledWidth();

        if (flashTicks > 0) flashTicks--;
        renderStacks(graphics, font, now);
        renderDamage(graphics, font, sw, now);
    }

    /** Expose stack info for the StatScreen: {stacks, maxStacks, secondsLeft}. */
    public static int[] getStackInfo() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return new int[]{0, 0, 0};
        long now = client.level.getLevelData().getGameTime();
        if (lastStacks <= 0 || now >= stacksExpire) return new int[]{0, 0, 0};
        return new int[]{lastStacks, lastMaxStacks, (int) ((stacksExpire - now) / 20)};
    }

    private static String fmt(float v) {
        if (v >= 1_000_000f) return String.format(Locale.ROOT, "%.1fM", v / 1_000_000f);
        if (v >= 10_000f) return String.format(Locale.ROOT, "%.1fk", v / 1_000f);
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** Top-left: combo stacks with a remaining-time bar. */
    private static void renderStacks(GuiGraphics graphics, net.minecraft.client.gui.Font font, long now) {
        if (lastStacks <= 0 || now >= stacksExpire) return;
        long remain = stacksExpire - now;
        int sec = (int) (remain / 20);
        String txt = "Tăng sát thương  x" + lastStacks + "/" + lastMaxStacks
                + "  (" + (sec / 60) + ":" + String.format("%02d", sec % 60) + ")";

        int x = 4, y = 22;
        int w = font.width(txt) + 12;
        int h = 18;

        graphics.fill(x + 1, y + 1, x + w + 1, y + h + 1, 0x66000000); // shadow
        graphics.fill(x, y, x + w, y + h, 0xC0151522);
        graphics.fill(x, y, x + 2, y + h, 0xFFBB86FC);
        graphics.fill(x + 2, y, x + w, y + 1, 0x88BB86FC);
        graphics.fill(x + 2, y + h - 1, x + w, y + h, 0x88BB86FC);
        graphics.fill(x, y, x + 1, y + 1, 0xFFBB86FC); // corner accents
        graphics.fill(x, y + h - 1, x + 1, y + h, 0xFFBB86FC);

        HcGui.drawText(graphics, font, txt, x + 7, y + 2, 0xFFFFD54F);

        float frac = (float) remain / HcCombat.STACK_DURATION;
        int bw = w - 8;
        graphics.fill(x + 4, y + h - 3, x + 4 + bw, y + h - 1, 0x44000000);
        graphics.fill(x + 4, y + h - 3, x + 4 + (int) (bw * frac), y + h - 1, 0xFFBB86FC);
    }

    /** Top-right: element panel with icon, name and accumulated damage. */
    private static void renderDamage(GuiGraphics graphics, net.minecraft.client.gui.Font font, int sw, long now) {
        long age = now - accTick;
        if (age < 0 || age >= HcCombat.ACC_WINDOW) return;

        float t = (float) age / HcCombat.ACC_WINDOW;
        float fade = 1.0f - t * t; // ease-out fade
        int alpha = Math.max(24, (int) (255 * fade));
        boolean flash = flashTicks > 0;

        Element el = Element.byId(accElement);
        int elColor = el != null ? el.color : 0xFFFFFF;
        String label = el != null ? el.name : "DMG";
        String dmg = fmt(accTotal);

        int icon = 9;
        int pad = 5;
        int gap = 5;
        int w = pad + icon + gap + font.width(label) + 6 + font.width(dmg) + pad;
        int h = 18;
        int x = sw - w - 4 + (flash ? 5 : 0); // slide-in on new hits
        int y = 6;

        // drop shadow
        graphics.fill(x + 1, y + 1, x + w + 1, y + h + 1, (alpha << 24) | 0x000000);

        // two-tone panel
        graphics.fill(x, y, x + w, y + h, (alpha << 24) | 0x14141F);
        graphics.fill(x, y, x + w, y + (h * 2) / 5, (alpha << 24) | 0x22223A);

        // border (flashes white on new hits)
        int border = flash ? 0xFFFFFF : elColor;
        graphics.fill(x, y, x + w, y + 1, (alpha << 24) | border);
        graphics.fill(x, y + h - 1, x + w, y + h, (alpha << 24) | border);
        graphics.fill(x, y, x + 1, y + h, (alpha << 24) | border);
        graphics.fill(x + w - 1, y, x + w, y + h, (alpha << 24) | border);
        graphics.fill(x, y, x + 1, y + 1, (alpha << 24) | 0xFFFFFF); // corner pop
        graphics.fill(x + w - 1, y, x + w, y + 1, (alpha << 24) | 0xFFFFFF);

        // element gem icon
        int iy = y + (h - icon) / 2;
        graphics.fill(x + pad, iy, x + pad + icon, iy + icon, (alpha << 24) | elColor);
        graphics.fill(x + pad + 1, iy + 1, x + pad + icon - 1, iy + icon - 1, (alpha << 24) | 0x30FFFFFF);

        // name (element color) + damage (white)
        int tx = x + pad + icon + gap;
        int ty = y + (h - 9) / 2;
        HcGui.drawText(graphics, font, label, tx, ty, (alpha << 24) | elColor);
        HcGui.drawText(graphics, font, dmg, tx + font.width(label) + 6, ty, (alpha << 24) | 0xFFFFFF);
    }
}
