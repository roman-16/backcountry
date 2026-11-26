package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.mixin.ImageButtonAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class RecipeBooks {
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("recipe_book/button");

    private RecipeBooks() {}

    @SubscribeEvent
    static void close(ScreenEvent.Init.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            for (RecipeBookType type : RecipeBookType.values()) {
                player.getRecipeBook().setOpen(type, false);
            }
        }
    }

    @SubscribeEvent
    static void hide(ScreenEvent.Init.Post event) {
        event.getListenersList().stream().filter(RecipeBooks::button).toList().forEach(event::removeListener);
    }

    private static boolean button(GuiEventListener listener) {
        return listener instanceof ImageButton button && ((ImageButtonAccessor) button).backcountry$sprites().enabled().equals(BUTTON);
    }
}
