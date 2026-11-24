package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.Things;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private static final float DRAW_TICKS = 20;

    private ClientSetup() {}

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(Things.FIRE_MENU.get(), FireScreen::new);
        event.register(Things.FOOD_CACHE_MENU.get(), CacheScreen::new);
        event.register(Things.SMOKER_MENU.get(), SmokerScreen::new);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Things.GROUSE.get(), BirdRenderer::new);
        event.registerEntityRenderer(Things.LOON.get(), BirdRenderer::new);
        event.registerEntityRenderer(Things.PTARMIGAN.get(), BirdRenderer::new);
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (Item bow : new Item[] {Things.LIGHT_HUNTING_BOW.get(), Things.HEAVY_HUNTING_BOW.get()}) {
                ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0
                        : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / DRAW_TICKS);
                ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1 : 0);
            }
        });
    }

    static final class BirdRenderer extends ChickenRenderer {
        BirdRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(Chicken bird) {
            String name = bird.getType() == Things.LOON.get() ? "loon" : bird.getType() == Things.GROUSE.get() ? "grouse" : winter() ? "ptarmigan_winter" : "ptarmigan";
            return Backcountry.id("textures/entity/" + name + ".png");
        }

        private static boolean winter() {
            return Hunting.months(Calendar.date(ClientState.view.day()).getMonthValue(), Balance.get().director().whiteMonths());
        }
    }
}
