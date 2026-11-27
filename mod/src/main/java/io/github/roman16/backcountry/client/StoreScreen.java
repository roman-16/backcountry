package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.BlockMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

abstract class StoreScreen<M extends BlockMenu> extends AbstractContainerScreen<M> {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 202;
    static final int PAD = 8;
    static final int LINE = 10;
    static final int LABEL_OFFSET = 5;

    StoreScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = BlockMenu.INVENTORY_Y - LINE - 1;
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
            graphics.blitSprite(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, BlockMenu.SLOT, BlockMenu.SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, Look.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Look.TEXT, false);
    }

    int lines(GuiGraphics graphics, Component text, int y, int colour) {
        List<FormattedCharSequence> wrapped = font.split(text, imageWidth - 2 * PAD);
        for (FormattedCharSequence line : wrapped) {
            graphics.drawString(font, line, PAD, y, colour, false);
            y += LINE;
        }
        return y;
    }
}
