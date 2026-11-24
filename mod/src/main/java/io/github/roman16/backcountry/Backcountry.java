package io.github.roman16.backcountry;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.net.Messages;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@Mod(Backcountry.MODID)
public final class Backcountry {
    public static final String MODID = "backcountry";
    private static final FoodProperties EDIBLE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build();
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MODID);

    static {
        DENSITY_FUNCTIONS.register("island", () -> IslandShape.CODEC);
    }

    public Backcountry(IEventBus bus) {
        DENSITY_FUNCTIONS.register(bus);
        Things.register(bus);
        bus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(Things.FOOD));
        bus.addListener(Messages::register);
        bus.addListener(BlockEntityTypeAddBlocksEvent.class, event -> event.modify(BlockEntityType.CAMPFIRE, Things.CAMPFIRE.get()));
        bus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(Things.ANIMAL));
        bus.addListener(EntityAttributeCreationEvent.class, event -> {
            event.put(Things.GROUSE.get(), Chicken.createAttributes().build());
            event.put(Things.LOON.get(), Chicken.createAttributes().build());
            event.put(Things.PTARMIGAN.get(), Chicken.createAttributes().build());
        });
        bus.addListener(ModifyDefaultComponentsEvent.class, event -> {
            event.modify(Items.BROWN_MUSHROOM, patch -> patch.set(DataComponents.FOOD, EDIBLE));
            event.modify(Items.RED_MUSHROOM, patch -> patch.set(DataComponents.FOOD, EDIBLE));
            event.modifyMatching(Fish::is, patch -> patch.set(DataComponents.FOOD, EDIBLE));
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
