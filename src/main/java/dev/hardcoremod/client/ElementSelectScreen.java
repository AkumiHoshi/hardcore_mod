package dev.hardcoremod.client;

import dev.hardcoremod.Element;
import dev.hardcoremod.HcNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Element selection, structured exactly like StatScreen (vanilla buttons,
 * plain text) which is proven to render text correctly.
 */
public class ElementSelectScreen extends Screen {
    private static final String[] NAMES = {"Lửa", "Băng", "Lượng tử", "Sét", "Nước", "Đất", "Gió", "Vật lý"};
    private static final String[] DESCS = {
            "Đốt cháy kẻ địch · Miễn nhiễm lửa",
            "Đóng băng kẻ địch · Miễn nhiễm đóng băng",
            "Né 10% đòn · 2x sát thương ngẫu nhiên",
            "Triệu hồi sét đánh · Miễn nhiễm sét",
            "Hạ gục mục tiêu đang cháy +50%",
            "Đòn đánh hất văng · Kháng knockback",
            "Hất tung kẻ địch · Miễn nhiễm ngã",
            "Đòn nặng +50% · Miễn nhiễm nổ",
    };

    public ElementSelectScreen() {
        super(Component.literal("Chọn nguyên tố của bạn"));
    }

    /** Block all keyboard input while choosing an element. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return true;
    }

    @Override
    protected void init() {
        int cw = 190;
        int x0 = width / 2 - cw - 8;
        int x1 = width / 2 + 8;
        int y = height / 2 - 100;
        Element[] els = Element.values();
        for (int i = 0; i < els.length; i++) {
            int x = (i % 2 == 0) ? x0 : x1;
            int yy = y + (i / 2) * 48;
            Element el = els[i];
            addRenderableWidget(Button.builder(
                            Component.literal(el.name + "  —  " + DESCS[i]),
                            b -> {
                                ClientPlayNetworking.send(new HcNetworking.ElementChoiceC2S(el.id()));
                                onClose();
                            })
                    .bounds(x, yy, cw, 42).build());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        HcGui.blurBackground(graphics);
        HcGui.drawAmethystBg(graphics, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        HcGui.drawCenteredText(graphics, font, "Chọn nguyên tố của bạn", width / 2, height / 2 - 122, 0xFFFFFF);
        HcGui.drawCenteredText(graphics, font, "Mỗi nguyên tố có lợi ích và hiệu ứng riêng", width / 2, height / 2 - 108, 0xAAAAAA);
    }
}
