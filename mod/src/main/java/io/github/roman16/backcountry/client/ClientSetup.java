package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.fire.Lights;
import io.github.roman16.backcountry.hunt.Hunting;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private static final float DRAW_TICKS = 20;
    private static final int NORTH_BLOCKS = 30_000_000;
    private static final int BAR_WIDTH = 13;
    private static final int BAR_X = 2;
    private static final int BAR_Y = 13;
    private static final String INVMOVE = "invmove";

    private ClientSetup() {}

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(Things.FIRE_MENU.get(), FireScreen::new);
        event.register(Things.FOOD_CACHE_MENU.get(), CacheScreen::new);
        event.register(Things.SMOKER_MENU.get(), SmokerScreen::new);
    }

    @SubscribeEvent
    static void colours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level == null || pos == null ? Look.WATER : BiomeColors.getAverageWaterColor(level, pos), Things.CAMP_POT_BLOCK.get());
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Things.GROUSE.get(), BirdRenderer::new);
        event.registerEntityRenderer(Things.LOON.get(), BirdRenderer::new);
        event.registerEntityRenderer(Things.PTARMIGAN.get(), BirdRenderer::new);
        event.registerEntityRenderer(Things.SLED_ENTITY.get(), SledRenderer::new);
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(Items.COMPASS, ResourceLocation.withDefaultNamespace("angle"), new CompassItemPropertyFunction((level, stack, entity) ->
                    GlobalPos.of(level.dimension(), entity.blockPosition().north(NORTH_BLOCKS))));
            for (Item bow : new Item[] {Things.LIGHT_HUNTING_BOW.get(), Things.HEAVY_HUNTING_BOW.get()}) {
                ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0
                        : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / DRAW_TICKS);
                ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1 : 0);
            }
            if (ModList.get().isLoaded(INVMOVE)) {
                InvMoveLink.register();
            }
        });
    }

    @SubscribeEvent
    static void decorations(RegisterItemDecorationsEvent event) {
        for (Item light : new Item[] {Items.TORCH, Items.LANTERN}) {
            event.register(light, (graphics, font, stack, x, y) -> {
                if (!stack.has(Things.BURN.get())) {
                    return false;
                }
                int width = Math.round(BAR_WIDTH * (float) (Lights.left(stack) / Lights.Kind.of(stack).orElseThrow().fullHours()));
                graphics.fill(RenderType.guiOverlay(), x + BAR_X, y + BAR_Y, x + BAR_X + BAR_WIDTH, y + BAR_Y + 2, Look.BAR_BACK);
                graphics.fill(RenderType.guiOverlay(), x + BAR_X, y + BAR_Y, x + BAR_X + width, y + BAR_Y + 1, Look.BURNING);
                return true;
            });
        }
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
