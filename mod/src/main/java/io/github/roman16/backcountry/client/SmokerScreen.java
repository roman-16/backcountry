package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.food.SmokerEntity;
import io.github.roman16.backcountry.food.SmokerMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class SmokerScreen extends AbstractContainerScreen<SmokerMenu> {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 202;
    private static final int PAD = 8;
    private static final int LINE = 11;
    private static final int LABEL_OFFSET = 5;
    private static final int SMOKE_Y = 68;
    private static final int BAR_X = 60;
    private static final int BAR_HEIGHT = 5;
    private static final int MINUTES_AN_HOUR = 60;

    public SmokerScreen(SmokerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = SmokerMenu.INVENTORY_Y - LINE;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blitSprite(PANEL, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            graphics.blitSprite(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, SmokerMenu.SLOT, SmokerMenu.SLOT);
        }
        int left = leftPos + BAR_X;
        int right = leftPos + imageWidth - PAD;
        int top = topPos + SMOKE_Y + LINE + 1;
        graphics.fill(left, top, right, top + BAR_HEIGHT, Look.TRACK);
        double batch = menu.batchMinutes() / (Balance.get().smoker().batchHours() * MINUTES_AN_HOUR);
        graphics.fill(left, top, left + (int) Math.round((right - left) * Math.min(1, batch)), top + BAR_HEIGHT, Look.HOT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, Look.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("smoker.backcountry.hanging"), PAD, SmokerMenu.HANGING_Y + LABEL_OFFSET, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("smoker.backcountry.firewood"), PAD, SmokerMenu.FIREWOOD_Y + LABEL_OFFSET, Look.TEXT, false);
        SmokerEntity.Smoke smoke = menu.smoke();
        int colour = smoke == SmokerEntity.Smoke.RISING ? Look.TEXT : Look.TEXT_QUIET;
        Component level = Component.translatable("smoker.backcountry." + smoke.name().toLowerCase(Locale.ROOT));
        graphics.drawString(font, Component.translatable("smoker.backcountry.smoke", level, Words.duration(menu.burnMinutes())), PAD, SMOKE_Y, colour, false);
        graphics.drawString(font, Component.translatable("smoker.backcountry.batch"), PAD, SMOKE_Y + LINE, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("smoker.backcountry.batch_time", Words.duration(menu.batchMinutes()),
                Words.duration((int) (Balance.get().smoker().batchHours() * MINUTES_AN_HOUR))), PAD, SMOKE_Y + 2 * LINE + 2, Look.TEXT_QUIET, false);
    }
}
