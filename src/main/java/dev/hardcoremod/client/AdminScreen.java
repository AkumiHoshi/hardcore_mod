package dev.hardcoremod.client;

import dev.hardcoremod.AdminManager;
import dev.hardcoremod.Element;
import dev.hardcoremod.HcNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdminScreen extends Screen {
    private static AdminScreen open;
    private static HcNetworking.AdminStateS2C lastState;

    private final String password;
    private HcNetworking.AdminStateS2C state;
    private int rulePage;
    private boolean needsRebuild = true;
    private EditBox commandBox;

    public AdminScreen(String password) {
        super(Component.literal("Admin Panel"));
        this.password = password;
        this.state = lastState;
        open = this;
    }

    public static void receive(HcNetworking.AdminStateS2C s) {
        lastState = s;
        if (open != null) {
            open.state = s;
            open.needsRebuild = true;
        }
    }

    private void send(String action, int value) {
        ClientPlayNetworking.send(new HcNetworking.AdminActionC2S(password, action, value));
    }

    private void runCommand() {
        if (commandBox == null) return;
        String cmd = commandBox.getValue().trim();
        if (cmd.isEmpty()) return;
        ClientPlayNetworking.send(new HcNetworking.AdminCommandC2S(password, cmd));
        commandBox.setValue("");
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (commandBox != null && commandBox.isFocused() && event.key() == 257) {
            runCommand();
            return true;
        }
        return super.keyPressed(event);
    }

    private void rebuild() {
        clearWidgets();
        if (state == null) return;
        int w = width;
        int h = height;
        boolean tall = h >= 300;
        int y0 = tall ? 42 : 34;
        int rowH = tall ? 22 : 20;
        int btnH = tall ? 18 : 16;
        int btnPad = tall ? 74 : 70;
        int ruleRow = tall ? 17 : 16;

        int xL = Math.max(8, w / 2 - 208);
        int xR = w / 2 + 8;
        int[] lvl = {state.strLvl(), state.digLvl(), state.hpLvl()};
        String[] statLabels = {"Sức mạnh " + lvl[0], "Đào " + lvl[1], "Máu " + lvl[2]};
        String[] statActions = {"str", "dig", "hp"};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int yy = y0 + i * rowH;
            drawLabel(xL, yy, statLabels[idx]);
            addRenderableWidget(Button.builder(Component.literal("+1"), b -> send(statActions[idx], 1))
                    .bounds(xL + btnPad, yy, 34, btnH).build());
            addRenderableWidget(Button.builder(Component.literal("+5"), b -> send(statActions[idx], 5))
                    .bounds(xL + btnPad + 38, yy, 34, btnH).build());
        }
        int yP = y0 + 3 * rowH + 6;
        drawLabel(xL, yP, "Points " + state.points());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> send("points", 1)).bounds(xL + btnPad, yP, 34, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> send("points", 10)).bounds(xL + btnPad + 38, yP, 38, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("+100"), b -> send("points", 100)).bounds(xL + btnPad + 80, yP, 44, btnH).build());
        int yS = yP + rowH;
        drawLabel(xL, yS, "Shards " + state.shards());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> send("shards", 1)).bounds(xL + btnPad, yS, 34, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> send("shards", 10)).bounds(xL + btnPad + 38, yS, 38, btnH).build());

        // Right column: element / game mode / replay / backup
        String elText = "Nguyên tố: " + (Element.byId(state.element()) != null ? Element.byId(state.element()).name : "Chưa chọn");
        drawLabel(xR, y0, elText);
        addRenderableWidget(Button.builder(Component.literal("Chọn lại"), b -> send("element", 0))
                .bounds(xR + font.width(elText) + 8, y0, 66, btnH).build());

        String gmText = "Game mode";
        drawLabel(xR, y0 + rowH + 4, gmText);
        int gmX = xR + font.width(gmText) + 8;
        String[] modes = {"Survival", "Creative", "Adventure", "Spectator"};
        for (int i = 0; i < 4; i++) {
            int mi = i;
            addRenderableWidget(Button.builder(Component.literal(modes[i]), b -> send("gamemode", mi))
                    .bounds(gmX + (i % 2) * 62, y0 + rowH + 4 + (i / 2) * (btnH + 4), 58, btnH).build());
        }

        int yR1 = y0 + 3 * rowH + 4;
        drawLabel(xR, yR1, "Replay");
        addRenderableWidget(Button.builder(Component.literal("Mở Replay"), b -> send("replay", 0))
                .bounds(xR + font.width("Replay") + 8, yR1, 88, btnH).build());

        int yR2 = yR1 + rowH;
        String bkText = "Backup cũ: " + (state.keepOldBackups() ? "GIỮ" : "XÓA");
        drawLabel(xR, yR2, bkText);
        addRenderableWidget(Button.builder(Component.literal("Đổi"), b -> send("keepbackup", 0))
                .bounds(xR + font.width(bkText) + 8, yR2, 44, btnH).build());

        // Rules (5 per page)
        int yT = y0 + 5 * rowH + 10;
        drawLabel(xL, yT, "Gamerules (trang " + (rulePage + 1) + "/2)");
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            rulePage = 0;
            needsRebuild = true;
        }).bounds(w - 96, yT, 40, btnH).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            rulePage = 1;
            needsRebuild = true;
        }).bounds(w - 52, yT, 40, btnH).build());
        for (int i = 0; i < 5; i++) {
            int idx = rulePage * 5 + i;
            if (idx >= AdminManager.RULES.size()) break;
            var entry = AdminManager.RULES.get(idx);
            boolean on = (state.rules() & (1 << idx)) != 0;
            int yy = yT + 18 + i * ruleRow;
            drawLabel(xL, yy, (on ? "§2" : "§4") + "● " + entry.label());
            addRenderableWidget(Button.builder(Component.literal(on ? "ON" : "OFF"),
                            b -> send(on ? "rule_off" : "rule_on", idx))
                    .bounds(w - 52, yy, 40, btnH).build());
        }

        // Command box
        int yC = yT + 18 + 5 * ruleRow + 6;
        String kept = commandBox != null ? commandBox.getValue() : "";
        commandBox = new EditBox(font, xL, yC, Math.max(60, w - xL - 68 - 4), btnH, Component.literal("Lệnh"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("Gõ lệnh rồi bấm Chạy hoặc Enter..."));
        commandBox.setValue(kept);
        addRenderableWidget(commandBox);
        addRenderableWidget(Button.builder(Component.literal("Chạy"), b -> runCommand())
                .bounds(w - 68, yC, 60, btnH).build());
    }

    private void drawLabel(int x, int y, String text) {
        labels.add(new int[]{x, y});
        labelTexts.add(text);
    }

    private final java.util.List<int[]> labels = new java.util.ArrayList<>();
    private final java.util.List<String> labelTexts = new java.util.ArrayList<>();

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        HcGui.blurBackground(graphics);
        HcGui.drawAmethystBg(graphics, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (needsRebuild) {
            needsRebuild = false;
            labels.clear();
            labelTexts.clear();
            rebuild();
        }
        super.render(graphics, mouseX, mouseY, delta);
        HcGui.drawCenteredText(graphics, font, "Admin Panel", width / 2, 16, 0xFFFFFF);
        for (int i = 0; i < labels.size(); i++) {
            int[] p = labels.get(i);
            HcGui.drawText(graphics, font, labelTexts.get(i), p[0], p[1], 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        open = null;
    }
}
