package io.github.roman16.backcountry;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.net.Messages;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@Mod(Backcountry.MODID)
public final class Backcountry {
    public static final String MODID = "backcountry";
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MODID);

    static {
        DENSITY_FUNCTIONS.register("island", () -> IslandShape.CODEC);
    }

    public Backcountry(IEventBus bus) {
        DENSITY_FUNCTIONS.register(bus);
        Things.register(bus);
        bus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(Things.FOOD));
        bus.addListener(Messages::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
