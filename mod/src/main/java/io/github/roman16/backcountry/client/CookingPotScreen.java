package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.food.CookingPotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CookingPotScreen extends StoreScreen<CookingPotMenu> {
    private static final int STATUS_Y = 66;

    public CookingPotScreen(CookingPotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("cooking_pot.backcountry.foods"), PAD, CookingPotMenu.FOODS_Y + LABEL_OFFSET, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("cooking_pot.backcountry.water", Words.one(menu.waterL())), PAD, CookingPotMenu.STEW_Y + LABEL_OFFSET, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("cooking_pot.backcountry.stew"), CookingPotMenu.STEW_X - font.width(Component.translatable("cooking_pot.backcountry.stew")) - 4,
                CookingPotMenu.STEW_Y + LABEL_OFFSET, Look.TEXT, false);
        lines(graphics, menu.status(), STATUS_Y, Look.TEXT_QUIET);
    }
}
