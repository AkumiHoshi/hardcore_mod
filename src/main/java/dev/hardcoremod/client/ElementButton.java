package dev.hardcoremod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Two-line button with an element-colored accent bar.
 */
public class ElementButton extends AbstractWidget implements GuiEventListener {
    private final String name;
    private final String desc;
    private final int tint;
    private final Runnable onPress;

    public ElementButton(int x, int y, int width, int height, String name, String desc, int tint, Runnable onPress) {
        super(x, y, width, height, Component.literal(name));
        this.name = name;
        this.desc = desc;
        this.tint = tint;
        this.onPress = onPress;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean bl) {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        boolean hover = isHoveredOrFocused();
        g.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xE0404060 : 0xC02A2A3A);
        g.fill(getX(), getY(), getX() + 3, getY() + height, tint);
        if (hover) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0x22FFFFFF);
        }
        var font = Minecraft.getInstance().font;
        int cx = getX() + width / 2;
        HcGui.drawCenteredText(g, font, name, cx, getY() + 5, 0xFFFFFF);
        HcGui.drawCenteredText(g, font, desc, cx, getY() + height - 11, 0xC9C9C9);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, name);
    }
}
