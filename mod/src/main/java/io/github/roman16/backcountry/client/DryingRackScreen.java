package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.food.DryingRackEntity;
import io.github.roman16.backcountry.food.DryingRackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DryingRackScreen extends StoreScreen<DryingRackMenu> {
    private static final int LINES_Y = 42;

    public DryingRackScreen(DryingRackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("rack.backcountry.hanging"), PAD, DryingRackMenu.HANGING_Y + LABEL_OFFSET, Look.TEXT, false);
        int y = LINES_Y;
        for (int slot = 0; slot < DryingRackEntity.SLOTS; slot++) {
            if (menu.slots.get(slot).hasItem()) {
                y = lines(graphics, menu.line(slot), y, Look.TEXT_QUIET);
            }
        }
        if (menu.rained()) {
            lines(graphics, Component.translatable("message.backcountry.rack_rained"), y, Look.TEXT);
        }
    }
}
