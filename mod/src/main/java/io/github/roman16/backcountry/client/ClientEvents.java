package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.fire.Lights;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.food.Spoilage;
import io.github.roman16.backcountry.gear.Hotbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final double WATER_SHOWN = 0.005;
    private static final String ADVANCEMENTS = "gui.advancements";
    private static final String STATISTICS = "gui.stats";
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
    static void paused(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }
        button(event, ADVANCEMENTS).ifPresent(advancements -> button(event, STATISTICS).ifPresent(statistics -> {
            event.removeListener(advancements);
            statistics.setWidth(statistics.getRight() - advancements.getX());
            statistics.setX(advancements.getX());
        }));
    }

    private static Optional<Button> button(ScreenEvent.Init.Post event, String key) {
        return event.getListenersList().stream()
                .filter(listener -> listener instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key))
                .map(Button.class::cast)
                .findFirst();
    }

    @SubscribeEvent
    static void burning(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(Things.BURN.get())) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    Component.translatable("tooltip.backcountry.burning", (int) Math.ceil(Lights.left(stack))).withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Nutrition.of(stack).ifPresent(food -> {
            List<Component> lines = new ArrayList<>();
            lines.add((food.water() >= WATER_SHOWN ? Component.translatable("tooltip.backcountry.food_water", Words.kcal(food.kcal()), Words.grams(food.fat()), Words.litres(food.water()))
                    : Component.translatable("tooltip.backcountry.food", Words.kcal(food.kcal()), Words.grams(food.fat()))).withStyle(ChatFormatting.GRAY));
            if (food.raw()) {
                lines.add(Component.translatable("tooltip.backcountry.raw").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(Things.ANIMAL_FAT.get())) {
                lines.add(Component.translatable("tooltip.backcountry.render_fat").withStyle(ChatFormatting.GRAY));
            }
            double air = ClientState.weather.airC();
            if (Spoilage.spoiled(stack)) {
                lines.add(Component.translatable("tooltip.backcountry.spoiled").withStyle(ChatFormatting.RED));
            } else {
                Spoilage.daysLeft(stack, air).ifPresent(days -> lines.add(Component.translatable("tooltip.backcountry.spoils", Words.days(days), Words.degrees(air))
                        .withStyle(ChatFormatting.GRAY)));
            }
            if (stack.has(Things.SMOKED.get())) {
                lines.add(Component.translatable("tooltip.backcountry.smoked").withStyle(ChatFormatting.GRAY));
            }
            if (stack.has(Things.CONTAMINATED.get())) {
                lines.add(Component.translatable("tooltip.backcountry.contaminated").withStyle(ChatFormatting.RED));
            }
            if (stack.has(Things.SPAWNED_OUT.get())) {
                lines.add(Component.translatable("tooltip.backcountry.spawned_out").withStyle(ChatFormatting.RED));
            }
            if (stack.has(Things.POISONED.get())) {
                lines.add(Component.translatable("tooltip.backcountry.poisoned").withStyle(ChatFormatting.RED));
            }
            if (Fish.frozenThrough(stack)) {
                lines.add(Component.translatable("tooltip.backcountry.frozen_through").withStyle(ChatFormatting.AQUA));
            }
            event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
        });
    }

    @SubscribeEvent
    static void tooFull(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() == Minecraft.getInstance().player && !Minecraft.getInstance().player.isCreative() && !Minecraft.getInstance().player.isSpectator()
                && Metabolism.tooFull(ClientState.stomachKcal(), event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void background(ContainerScreenEvent.Render.Background event) {
        if (event.getContainerScreen() instanceof InventoryScreen screen && !Minecraft.getInstance().player.isCreative()) {
            for (Hotbar.Worn worn : Hotbar.Worn.values()) {
                event.getGuiGraphics().blitSprite(SLOT_SPRITE, screen.getGuiLeft() + Hotbar.WORN_X - 1, screen.getGuiTop() + worn.y - 1, SLOT, SLOT);
            }
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
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && screen.getSlotUnderMouse() instanceof Hotbar.BodySlot body
                && screen.getMenu().getCarried().isEmpty()) {
            body.needs().ifPresent(needs -> event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, needs, event.getMouseX(), event.getMouseY()));
        }
    }
}
