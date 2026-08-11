package dev.hardcoremod.client;

import dev.hardcoremod.HcNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ReplayScreen extends Screen {
    private final boolean hasBackup;
    private final double mult;
    private final double netherMult;
    private final double endMult;

    private boolean voteActive;
    private int votesLoad;
    private int votesContinue;
    private int secondsLeft;
    private boolean voted;

    public ReplayScreen(boolean hasBackup, double mult, double netherMult, double endMult) {
        super(Component.literal("Replay Game"));
        this.hasBackup = hasBackup;
        this.mult = mult;
        this.netherMult = netherMult;
        this.endMult = endMult;
    }

    public void onVoteState(HcNetworking.ReplayVoteStateS2C s) {
        boolean wasActive = voteActive;
        voteActive = s.active();
        votesLoad = s.votesLoad();
        votesContinue = s.votesContinue();
        secondsLeft = s.secondsLeft();
        voted = s.voted();
        if (wasActive && !s.active()) {
            onClose(); // vote resolved — close the screen
        }
    }

    @Override
    protected void init() {
        int cw = 280;
        int x = width / 2 - cw / 2;
        int y = height / 2 - 34;
        addRenderableWidget(Button.builder(Component.literal(voteActive ? "Vote: Chơi lại từ backup" : "Chơi lại từ backup  (Quái −25%)"),
                        b -> {
                            ClientPlayNetworking.send(new HcNetworking.ReplayChoiceC2S(true));
                            if (voteActive) {
                                voted = true;
                            } else {
                                onClose();
                            }
                        })
                .bounds(x, y, cw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(voteActive ? "Vote: Chơi tiếp" : "Chơi tiếp  (+25% quái, tạo backup)"),
                        b -> {
                            ClientPlayNetworking.send(new HcNetworking.ReplayChoiceC2S(false));
                            if (voteActive) {
                                voted = true;
                            } else {
                                onClose();
                            }
                        })
                .bounds(x, y + 28, cw, 20).build());
        if (!hasBackup) {
            addRenderableWidget(Button.builder(Component.literal("Chưa có backup — hãy chọn \"Chơi tiếp\" trước"),
                            b -> onClose())
                    .bounds(x, y + 56, cw, 20).build());
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

        double effective = mult * netherMult * endMult;
        String diff = "Độ khó hiện tại: " + Math.round(effective * 100) + "%";
        if (netherMult > 1) diff += "  ·  Nether ×" + Math.round(netherMult);
        if (endMult > 1) diff += "  ·  End ×" + Math.round(endMult);

        HcGui.drawCenteredText(graphics, font, "Replay Game", width / 2, height / 2 - 96, 0xFFFFFF);
        HcGui.drawCenteredText(graphics, font, diff, width / 2, height / 2 - 80, 0xFFD54F);

        if (voteActive) {
            HcGui.drawCenteredText(graphics, font, "VOTE:  Chơi lại " + votesLoad + "  |  Chơi tiếp " + votesContinue
                    + "  —  còn " + secondsLeft + "s", width / 2, height / 2 - 62, 0x7FD4FF);
            HcGui.drawCenteredText(graphics, font, voted ? "Bạn đã vote ✓" : "Bấm nút để vote — bên nhiều phiếu hơn thắng",
                    width / 2, height / 2 - 46, voted ? 0x9BD67E : 0xAAAAAA);
        } else {
            HcGui.drawCenteredText(graphics, font, "Tất cả người chơi đã chết", width / 2, height / 2 - 62, 0xAAAAAA);
        }
    }
}
