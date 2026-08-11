package dev.hardcoremod.client;

import dev.hardcoremod.HcNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatScreen extends Screen {
    private static StatScreen open;

    private int points;
    private int shards;
    private int strLvl;
    private int digLvl;
    private int hpLvl;

    public StatScreen() {
        super(Component.literal("Stat Points"));
        open = this;
    }

    public static void receive(HcNetworking.StatSyncS2C p) {
        if (open != null) open.update(p);
    }

    private void update(HcNetworking.StatSyncS2C p) {
        points = p.points();
        shards = p.shards();
        strLvl = p.strLvl();
        digLvl = p.digLvl();
        hpLvl = p.hpLvl();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cw = 230;
        int x = width / 2 - cw / 2;
        int y = height / 2 - 30;
        addRenderableWidget(Button.builder(Component.literal("Sức mạnh  (+0.5 dmg)  [Cấp " + strLvl + " · Giá " + (strLvl + 1) + "]"),
                        b -> ClientPlayNetworking.send(new HcNetworking.StatUpgradeC2S("strength")))
                .bounds(x, y, cw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Tốc độ đào  (+0.15)  [Cấp " + digLvl + " · Giá " + (digLvl + 1) + "]"),
                        b -> ClientPlayNetworking.send(new HcNetworking.StatUpgradeC2S("dig")))
                .bounds(x, y + 24, cw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Máu  (+2 HP)  [Cấp " + hpLvl + " · Giá " + (hpLvl + 1) + "]"),
                        b -> ClientPlayNetworking.send(new HcNetworking.StatUpgradeC2S("hp")))
                .bounds(x, y + 48, cw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Xem tầng sát thương"),
                        b -> net.minecraft.client.Minecraft.getInstance().setScreen(new StackInfoScreen()))
                .bounds(x + cw / 2 - 75, y + 74, 150, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        HcGui.blurBackground(graphics);
        HcGui.drawAmethystBg(graphics, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        // Points / shards panel
        int pw = 230;
        int px = width / 2 - pw / 2;
        int py = height / 2 - 70;
        HcGui.drawPanel(graphics, px, py, pw, 34);
        HcGui.drawText(graphics, font, "Points: " + points, px + 8, py + 5, 0xFFFFD54F);
        HcGui.drawText(graphics, font, "Shards: " + shards + "/10", px + 8, py + 18, 0x7FD4FF);
        int bw = 90;
        int bx = px + pw - bw - 8;
        int by = py + 12;
        graphics.fill(bx, by, bx + bw, by + 5, 0x44000000);
        graphics.fill(bx, by, bx + (int) (bw * Math.min(1f, shards / 10f)), by + 5, 0x7FD4FF);

        HcGui.drawCenteredText(graphics, font, "Stat Points", width / 2, height / 2 - 88, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        super.onClose();
        open = null;
    }
}
