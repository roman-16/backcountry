package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.fire.WoodpileMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WoodpileScreen extends StoreScreen<WoodpileMenu> {
    private static final int STATUS_Y = 62;

    public WoodpileScreen(WoodpileMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Balance.Firewood firewood = Balance.get().firewood();
        int y = lines(graphics, Component.translatable("message.backcountry.woodpile", menu.pieces(), firewood.pilePieces(), FirewoodItem.percent(menu.seasoning()),
                FirewoodItem.percent(firewood.catches())), STATUS_Y, Look.TEXT);
        lines(graphics, Component.translatable("woodpile.backcountry.drying", FirewoodItem.percent(firewood.dryPerDay())), y, Look.TEXT_QUIET);
    }
}
