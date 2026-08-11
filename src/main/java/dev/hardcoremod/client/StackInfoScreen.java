package dev.hardcoremod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shows current "Tăng sát thương" stacks: count, per-stack bonus, total bonus, time left.
 */
public class StackInfoScreen extends Screen {

    public StackInfoScreen() {
        super(Component.literal("Tầng sát thương"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        HcGui.blurBackground(graphics);
        HcGui.drawAmethystBg(graphics, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int[] info = DmgCounter.getStackInfo();
        int stacks = info[0];
        int maxStacks = info[1];
        int secLeft = info[2];

        int[] maxByLevel = {0, 5, 7, 10, 15, 20};
        double[] pctByLevel = {0, 0.02, 0.03, 0.04, 0.045, 0.05};
        int level = 0;
        for (int i = 1; i <= 5; i++) {
            if (maxStacks == maxByLevel[i]) level = i;
        }
        double perStack = level > 0 ? pctByLevel[level] * 100 : 0;
        double total = stacks * perStack;

        int cx = width / 2;
        int y = height / 2 - 70;
        HcGui.drawCenteredText(graphics, font, "Tầng sát thương", cx, y, 0xFFFFFF);
        HcGui.drawCenteredText(graphics, font, stacks > 0
                ? "Số tầng hiện tại: " + stacks + " / " + maxStacks
                : "Chưa có tầng nào (đánh quái bằng kiếm có enchant)", cx, y + 22, 0xFFFFD54F);
        HcGui.drawCenteredText(graphics, font, String.format("Mỗi tầng cộng thêm: +%.1f%% sát thương", perStack), cx, y + 40, 0xAAAAAA);
        HcGui.drawCenteredText(graphics, font, String.format("Tổng cộng thêm hiện tại: +%.1f%%", total), cx, y + 58, 0x9BD67E);
        HcGui.drawCenteredText(graphics, font, "Thời gian còn lại: " + (secLeft / 60) + ":" + String.format("%02d", secLeft % 60), cx, y + 76, 0x7FD4FF);
        HcGui.drawCenteredText(graphics, font, "Mỗi lần đánh trúng: +1 tầng, reset thời gian về 5 phút", cx, y + 100, 0x888888);
    }
}
