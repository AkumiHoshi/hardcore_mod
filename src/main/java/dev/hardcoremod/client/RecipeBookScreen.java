package dev.hardcoremod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Recipe book: shows how to craft and upgrade all custom books, by category.
 * Opened with /akumiyuukiirecipe book. All text drawn centered (proven path).
 */
public class RecipeBookScreen extends Screen {
    private static final String[] PAGE_TITLES = {
            "Tăng sát thương kèm theo (tối đa V)",
            "Tăng sát thương nguyên tố (tối đa XX)",
            "Tăng tỉ lệ nguyên tố (tối đa V)",
    };

    private static final String[][] PAGES = {
            {
                    "Sách + Kim cương + 2 Sắt  ->  Cuốn I",
                    "",
                    "Nâng cấp (ANVIL): 2 cuốn cùng cấp -> cấp +1",
                    "  I + I = II, II + II = III, ... tối đa V",
                    "",
                    "Tác dụng: mỗi đòn +% sát thương kiếm + % stat,",
                    "nhận 1 tầng sát thương (reset 5 phút,",
                    "mỗi tầng +2% (I) den +5% (V), toi da 5-20 tang).",
            },
            {
                    "Lửa:      Sách + Kim cương + Bột lửa",
                    "Băng:     Sách + Kim cương + Băng xanh",
                    "Lượng tử: Sách + Kim cương + Mảnh thạch anh tím",
                    "Sét:      Sách + Kim cương + Thuốc súng",
                    "Nước:     Sách + Kim cương + Mảnh vỏ sò",
                    "Đất:      Sách + Kim cương + Đá hắc ám",
                    "Gió:      Sách + Kim cương + Màng Phantom",
                    "Vật lý:   Sách + Kim cương + Vàng",
                    "",
                    "Nâng cấp (ANVIL): 2 cuốn cùng cấp -> cấp +1",
                    "Tác dụng: I-X +10%/cấp, XI+ +5%/cấp (max 20).",
            },
            {
                    "Lửa:      Sách + Kim cương + Đá đỏ + Bột lửa",
                    "Băng:     Sách + Kim cương + Đá đỏ + Băng xanh",
                    "Lượng tử: Sách + Kim cương + Đá đỏ + Mảnh thạch anh tím",
                    "Sét:      Sách + Kim cương + Đá đỏ + Thuốc súng",
                    "Nước:     Sách + Kim cương + Đá đỏ + Mảnh vỏ sò",
                    "Đất:      Sách + Kim cương + Đá đỏ + Đá hắc ám",
                    "Gió:      Sách + Kim cương + Đá đỏ + Màng Phantom",
                    "Vật lý:   Sách + Kim cương + Đá đỏ + Vàng",
                    "",
                    "Nâng cấp (ANVIL): 2 cuốn cùng cấp -> cấp +1",
                    "Tác dụng: +8% tỉ lệ hiệu ứng nguyên tố / cấp.",
            },
    };

    private int page;

    public RecipeBookScreen() {
        super(Component.literal("Recipe Book"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cw = 100;
        int x = width / 2 - cw - 8;
        addRenderableWidget(Button.builder(Component.literal("Trang truoc"),
                        b -> {
                            page = Math.max(0, page - 1);
                            rebuild();
                        })
                .bounds(x, height - 40, cw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Trang sau"),
                        b -> {
                            page = Math.min(PAGES.length - 1, page + 1);
                            rebuild();
                        })
                .bounds(x + cw + 16, height - 40, cw, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        HcGui.blurBackground(graphics);
        HcGui.drawAmethystBg(graphics, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        HcGui.drawCenteredText(graphics, font, "Công thức chế tạo sách", width / 2, 14, 0xFFFFFF);
        HcGui.drawCenteredText(graphics, font, PAGE_TITLES[page] + "  -  Trang " + (page + 1) + "/" + PAGES.length,
                width / 2, 28, 0xFFD54F);

        String[] rows = PAGES[page];
        int y = 52;
        for (String row : rows) {
            int color = 0xDDDDDD;
            if (row.startsWith("Nâng cấp")) color = 0x9BD67E;
            if (row.startsWith("Tác dụng")) color = 0x7FD4FF;
            if (row.startsWith("Sách")) color = 0xFFFFFF;
            if (row.startsWith("  I + I")) color = 0x888888;
            HcGui.drawCenteredText(graphics, font, row, width / 2, y, color);
            y += 12;
        }
        HcGui.drawCenteredText(graphics, font, "ESC de dong", width / 2, height - 18, 0x888888);
    }
}
