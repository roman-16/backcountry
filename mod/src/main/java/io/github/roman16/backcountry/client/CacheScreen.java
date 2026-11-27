package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.CacheMenu;
import io.github.roman16.backcountry.food.Ration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class CacheScreen extends AbstractContainerScreen<CacheMenu> {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int HEIGHT = 272;
    private static final int PAD = 8;
    private static final int LINE = 10;
    private static final int FIRST_LINE = 18;
    private static final int BUTTONS_Y = 110;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 2;
    private final List<Button> rations = new ArrayList<>();

    public CacheScreen(CacheMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = CacheMenu.WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = CacheMenu.INVENTORY_Y - LINE - 1;
    }

    @Override
    protected void init() {
        super.init();
        rations.clear();
        for (Ration ration : Ration.values()) {
            int x = leftPos + PAD + ration.ordinal() * (BUTTON_WIDTH + BUTTON_GAP);
            rations.add(addRenderableWidget(Button.builder(ration.title(), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ration.ordinal()))
                    .bounds(x, topPos + BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Button button : rations) {
            button.active = rations.indexOf(button) != menu.ration().ordinal();
        }
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
            graphics.blitSprite(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, CacheMenu.SLOT, CacheMenu.SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, Look.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Look.TEXT, false);
        int y = FIRST_LINE;
        y = line(graphics, Component.translatable("cache.backcountry.food", Words.kcal(menu.kcal())), y, Look.TEXT);
        y = line(graphics, Component.translatable("cache.backcountry.days", Words.one(menu.days()), menu.ration().title(), menu.survivors()), y, Look.TEXT);
        y = line(graphics, Component.translatable("cache.backcountry.fat", Words.kcal(menu.fatKcal())), y, Look.TEXT_QUIET);
        y = line(graphics, Component.translatable("cache.backcountry.spoiling", Words.one(menu.spoiling()), Words.degrees(menu.airC())), y, Look.TEXT_QUIET);
        y = switch (menu.reach()) {
            case TALL -> line(graphics, Component.translatable("cache.backcountry.tall"), y, Look.GOOD);
            case LADDER -> line(graphics, Component.translatable("cache.backcountry.ladder"), y, Look.DANGER);
            case GROUND -> line(graphics, Component.translatable("cache.backcountry.ground"), y, Look.DANGER);
        };
        if (menu.scented()) {
            line(graphics, Component.translatable("cache.backcountry.scent"), y, Look.DANGER);
        }
    }

    private int line(GuiGraphics graphics, Component text, int y, int colour) {
        for (FormattedCharSequence wrapped : font.split(text, imageWidth - 2 * PAD)) {
            graphics.drawString(font, wrapped, PAD, y, colour, false);
            y += LINE;
        }
        return y;
    }
}
