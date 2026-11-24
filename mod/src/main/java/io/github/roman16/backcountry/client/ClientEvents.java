package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.gear.Hotbar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int SLOT = 18;
    private static final int ITEM = 16;

    private ClientEvents() {}

    @SubscribeEvent
    static void opening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen && ClientState.ended != null) {
            event.setNewScreen(new SummaryScreen(ClientState.ended));
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (ClientState.blackout > 0) {
            ClientState.blackout--;
        }
        if (minecraft.screen instanceof SummaryScreen summary && summary.arriving() && minecraft.player.isAlive() && !minecraft.player.isSpectator()) {
            minecraft.setScreen(null);
        }
        if (ClientState.year != null && minecraft.screen == null) {
            minecraft.setScreen(new SummaryScreen(ClientState.year));
            ClientState.year = null;
        }
    }

    @SubscribeEvent
    static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.clear();
    }

    @SubscribeEvent
    static void messenger(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(Things.SATELLITE_MESSENGER.get())) {
            Minecraft.getInstance().setScreen(new MessengerScreen());
        }
    }

    @SubscribeEvent
    static void tooltip(ItemTooltipEvent event) {
        Nutrition.of(event.getItemStack()).ifPresent(food -> event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                Component.translatable("tooltip.backcountry.food", Words.kcal(food.kcal()), Words.grams(food.fat())).withStyle(ChatFormatting.GRAY)));
    }

    @SubscribeEvent
    static void background(ContainerScreenEvent.Render.Background event) {
        if (event.getContainerScreen() instanceof InventoryScreen screen && !Minecraft.getInstance().player.isCreative()) {
            int x = screen.getGuiLeft() + Hotbar.BACK_X - 1;
            int y = screen.getGuiTop() + Hotbar.BACK_Y - 1;
            event.getGuiGraphics().blitSprite(SLOT_SPRITE, x, y, SLOT, SLOT);
        }
    }

    @SubscribeEvent
    static void locked(ContainerScreenEvent.Render.Foreground event) {
        for (Slot slot : event.getContainerScreen().getMenu().slots) {
            if (slot instanceof Hotbar.BodySlot body && body.locked()) {
                event.getGuiGraphics().fill(slot.x, slot.y, slot.x + ITEM, slot.y + ITEM, Look.LOCKED);
            }
        }
    }

    @SubscribeEvent
    static void lockedTooltip(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && screen.getSlotUnderMouse() instanceof Hotbar.BodySlot body && body.locked()
                && screen.getMenu().getCarried().isEmpty()) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, Component.translatable("tooltip.backcountry.needs_pack"), event.getMouseX(), event.getMouseY());
        }
    }
}
