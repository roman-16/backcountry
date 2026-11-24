package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireMenu;
import io.github.roman16.backcountry.fire.FirewoodItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FireScreen extends AbstractContainerScreen<FireMenu> {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int WIDTH = 230;
    private static final int HEIGHT = 234;
    private static final int PAD = 8;
    private static final int LINE = 11;
    private static final int QUEUE_LABEL_Y = 25;
    private static final int SEASONING_Y = 39;
    private static final int HEAT_Y = 52;
    private static final int HEAT_BAR_X = 36;
    private static final int HEAT_BAR_HEIGHT = 5;
    private static final int BUTTONS_Y = 62;
    private static final int BUTTON_WIDTH = 68;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 5;
    private static final int TIMES_Y = 84;
    private static final int CHECKS_Y = 108;
    private static final int SECOND_COLUMN = 118;
    private final List<Button> settings = new ArrayList<>();

    public FireScreen(FireMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = FireMenu.INVENTORY_X;
        inventoryLabelY = FireMenu.INVENTORY_Y - LINE;
    }

    @Override
    protected void init() {
        super.init();
        settings.clear();
        for (Fire.Setting setting : Fire.Setting.values()) {
            int x = leftPos + PAD + setting.ordinal() * (BUTTON_WIDTH + BUTTON_GAP);
            settings.add(addRenderableWidget(Button.builder(Component.translatable("fire.backcountry." + setting.name().toLowerCase(Locale.ROOT)),
                    button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, setting.ordinal())).bounds(x, topPos + BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Button button : settings) {
            button.active = settings.indexOf(button) != menu.setting().ordinal();
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
            graphics.blitSprite(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, FireMenu.SLOT, FireMenu.SLOT);
        }
        int barLeft = leftPos + HEAT_BAR_X;
        int barRight = leftPos + imageWidth - PAD;
        int barTop = topPos + HEAT_Y + 1;
        graphics.fill(barLeft, barTop, barRight, barTop + HEAT_BAR_HEIGHT, Look.TRACK);
        int filled = (int) Math.round((barRight - barLeft) * Math.min(1, menu.heatC() / Fire.hottestC()));
        graphics.fill(barLeft, barTop, barLeft + filled, barTop + HEAT_BAR_HEIGHT, Look.HOT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, Look.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("fire.backcountry.queue"), PAD, QUEUE_LABEL_Y, Look.TEXT, false);
        for (int slot = 0; slot < Fire.QUEUE; slot++) {
            ItemStack piece = menu.slots.get(slot).getItem();
            if (!piece.isEmpty()) {
                String seasoned = FirewoodItem.percent(FirewoodItem.seasoning(piece)) + "%";
                int x = FireMenu.QUEUE_X + slot * FireMenu.QUEUE_PITCH + (FireMenu.SLOT - 2 - font.width(seasoned)) / 2;
                graphics.drawString(font, seasoned, x, SEASONING_Y, Look.TEXT_QUIET, false);
            }
        }
        graphics.drawString(font, Component.translatable("fire.backcountry.heat"), PAD, HEAT_Y, Look.TEXT, false);
        graphics.drawString(font, times(), PAD, TIMES_Y, Look.TEXT, false);
        if (menu.coalMinutes() > 0 && menu.lit()) {
            graphics.drawString(font, Component.translatable("fire.backcountry.coals", Words.duration(menu.coalMinutes())), PAD, TIMES_Y + LINE, Look.TEXT_QUIET, false);
        }
        check(graphics, PAD, CHECKS_Y, menu.queued(), Component.translatable("fire.backcountry.firewood_queued"));
        check(graphics, SECOND_COLUMN, CHECKS_Y, menu.banked(), Component.translatable("fire.backcountry.coals_banked"));
        check(graphics, PAD, CHECKS_Y + LINE, menu.roofed(), Component.translatable("fire.backcountry.out_of_rain"));
        graphics.drawString(font, Component.translatable("fire.backcountry.cook"), SECOND_COLUMN, CHECKS_Y + LINE, Look.TEXT_QUIET, false);
        if (menu.packed()) {
            graphics.drawString(font, Component.translatable("fire.backcountry.packed"), PAD, CHECKS_Y + 2 * LINE, Look.DANGER, false);
        }
    }

    private Component times() {
        String queued = Words.duration(menu.queuedMinutes());
        if (menu.lit()) {
            return Component.translatable("fire.backcountry.burning", Words.duration(menu.burningMinutes()), queued);
        }
        if (menu.coalMinutes() > 0) {
            return Component.translatable("fire.backcountry.settled", Words.duration(menu.coalMinutes()), queued);
        }
        return Component.translatable("fire.backcountry.cold");
    }

    private void check(GuiGraphics graphics, int x, int y, boolean done, Component label) {
        graphics.drawString(font, done ? "✓" : "✗", x, y, done ? Look.GOOD : Look.DANGER, false);
        graphics.drawString(font, label, x + font.width("✗ "), y, done ? Look.TEXT : Look.TEXT_QUIET, false);
    }
}
