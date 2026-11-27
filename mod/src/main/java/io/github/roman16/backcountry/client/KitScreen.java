package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.net.Messages;
import io.github.roman16.backcountry.survivor.Kit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class KitScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int COLUMNS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int CELL = 24;
    private static final int PAD = 10;
    private static final int LINE = 12;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TITLE_ROOM = 48;
    private final List<Kit> picked = new ArrayList<>();
    private Button arrive;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int titleTop;

    public KitScreen() {
        super(Component.translatable("kit.backcountry.title"));
    }

    @Override
    protected void init() {
        int rows = (Kit.values().length + COLUMNS - 1) / COLUMNS;
        panelWidth = COLUMNS * CELL + 2 * PAD;
        panelHeight = PAD + LINE + rows * CELL + PAD;
        panelLeft = (width - panelWidth) / 2;
        titleTop = Math.max(PAD, (height - TITLE_ROOM - panelHeight - PAD - BUTTON_HEIGHT) / 2);
        panelTop = titleTop + TITLE_ROOM;
        for (Kit pick : Kit.values()) {
            int index = pick.ordinal();
            addRenderableWidget(new Pick(panelLeft + PAD + index % COLUMNS * CELL, panelTop + PAD + LINE + index / COLUMNS * CELL, pick));
        }
        arrive = addRenderableWidget(Button.builder(Component.translatable("kit.backcountry.arrive"), button -> arrive(List.copyOf(picked))).bounds((width - BUTTON_WIDTH) / 2, panelTop + panelHeight + PAD, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    public void arrive(List<Kit> picks) {
        arrive.active = false;
        Messages.picks(picks);
        onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, Look.ENDED_TOP, Look.ENDED_BOTTOM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(PANEL, panelLeft, panelTop, panelWidth, panelHeight);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().scale(2, 2, 2);
        graphics.drawCenteredString(font, title, width / 4, titleTop / 2, Look.TEXT);
        graphics.pose().popPose();
        int issuedY = titleTop + 2 * LINE;
        for (FormattedCharSequence line : font.split(Component.translatable("kit.backcountry.issued"), width - 4 * PAD)) {
            graphics.drawCenteredString(font, line, width / 2, issuedY, Look.TEXT_QUIET);
            issuedY += font.lineHeight + 1;
        }
        Component count = Component.translatable("kit.backcountry.count", picked.size(), Balance.get().kit().picks());
        graphics.drawString(font, count, panelLeft + panelWidth - PAD - font.width(count), panelTop + PAD, Look.TEXT, false);
        graphics.drawString(font, Component.translatable("kit.backcountry.picks"), panelLeft + PAD, panelTop + PAD, Look.TEXT_QUIET, false);
    }

    private void toggle(Kit pick) {
        if (picked.contains(pick)) {
            picked.remove(pick);
        } else if (picked.size() < Balance.get().kit().picks()) {
            picked.add(pick);
        }
    }

    private static Component contents(Kit pick) {
        return Component.literal(pick.items().stream().map(stack -> stack.getCount() > 1 ? stack.getCount() + " " + stack.getHoverName().getString()
                : stack.getHoverName().getString()).collect(Collectors.joining(", ")));
    }

    private final class Pick extends AbstractButton {
        private final Kit pick;
        private final ItemStack icon;

        Pick(int x, int y, Kit pick) {
            super(x, y, SLOT_SIZE, SLOT_SIZE, pick.title());
            this.pick = pick;
            this.icon = pick.icon();
            setTooltip(Tooltip.create(pick.title().copy().append(CommonComponents.NEW_LINE).append(contents(pick).copy().withStyle(ChatFormatting.GRAY))));
        }

        @Override
        public void onPress() {
            toggle(pick);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = picked.contains(pick);
            graphics.blitSprite(SLOT, getX(), getY(), SLOT_SIZE, SLOT_SIZE);
            if (chosen) {
                graphics.fill(getX() + 1, getY() + 1, getX() + SLOT_SIZE - 1, getY() + SLOT_SIZE - 1, Look.KIT_PICKED);
            }
            graphics.renderItem(icon, getX() + 1, getY() + 1);
            if (icon.getCount() > 1) {
                graphics.renderItemDecorations(font, icon, getX() + 1, getY() + 1);
            }
            if (isHoveredOrFocused()) {
                graphics.fill(getX() + 1, getY() + 1, getX() + SLOT_SIZE - 1, getY() + SLOT_SIZE - 1, Look.KIT_HOVER);
            }
            if (chosen) {
                graphics.renderOutline(getX() - 1, getY() - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Look.KIT_OUTLINE);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.translatable(picked.contains(pick) ? "kit.backcountry.narrate_picked" : "kit.backcountry.narrate", pick.title()));
        }
    }
}
