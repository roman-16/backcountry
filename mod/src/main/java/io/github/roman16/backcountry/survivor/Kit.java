package io.github.roman16.backcountry.survivor;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.gear.Water;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum Kit {
    AXE(() -> List.of(new ItemStack(Items.IRON_AXE))),
    BOW_SAW(() -> List.of(new ItemStack(Things.BOW_SAW.get()))),
    BELT_KNIFE(() -> List.of(new ItemStack(Things.BELT_KNIFE.get()))),
    FERRO_ROD(() -> List.of(new ItemStack(Things.FERRO_ROD.get()))),
    CAMP_POT(() -> List.of(new ItemStack(Things.CAMP_POT.get()))),
    GILL_NET(() -> List.of(new ItemStack(Things.GILL_NET_ITEM.get()))),
    SNARE_WIRE(() -> List.of(new ItemStack(Things.SNARE_WIRE.get(), Balance.get().kit().snareWire()))),
    FISHING_KIT(() -> List.of(new ItemStack(mod("riverfishing", "line_mono_010"), Balance.get().kit().monoLines()),
            new ItemStack(mod("riverfishing", "hook_10"), Balance.get().kit().hooks()))),
    HUNTING_BOW(() -> List.of(new ItemStack(Things.LIGHT_HUNTING_BOW.get()), new ItemStack(Things.BROADHEAD_ARROW.get(), Balance.get().kit().broadheads()))),
    TARP(() -> List.of(new ItemStack(Things.TARP.get(), Balance.get().kit().tarps()))),
    PARACORD(() -> List.of(new ItemStack(Things.ROPE.get(), Balance.get().kit().rope()))),
    SLEEPING_BAG(() -> List.of(new ItemStack(mod("sleeping_bags", "brown_sleeping_bag")))),
    SLEEPING_PAD(() -> List.of(new ItemStack(Things.SLEEPING_PAD.get()))),
    ICE_AUGER(() -> List.of(new ItemStack(mod("riverfishing", "ice_auger")))),
    SHOVEL(() -> List.of(new ItemStack(Items.IRON_SHOVEL))),
    PILOT_BREAD(() -> List.of(new ItemStack(Things.PILOT_BREAD.get(), Balance.get().kit().pilotBread()))),
    PEMMICAN(() -> List.of(new ItemStack(mod("cnc", "pemmican"), Balance.get().kit().pemmican()))),
    SHARPENING_STONE(() -> List.of(new ItemStack(Things.SHARPENING_STONE.get())));

    public static final List<Kit> CHOSEN = List.of(AXE, BOW_SAW, BELT_KNIFE, FERRO_ROD, CAMP_POT, GILL_NET, SNARE_WIRE, FISHING_KIT, SLEEPING_BAG, PILOT_BREAD);

    private final Supplier<List<ItemStack>> items;

    Kit(Supplier<List<ItemStack>> items) {
        this.items = items;
    }

    private static Item mod(String namespace, String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public List<ItemStack> items() {
        return items.get();
    }

    public ItemStack icon() {
        return items().getFirst();
    }

    public Component title() {
        return Component.translatable("kit.backcountry." + name().toLowerCase(Locale.ROOT));
    }

    public static List<ItemStack> issued() {
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        bottle.set(Things.WATER.get(), Water.boiled((float) Balance.get().water().bottleL()));
        return List.of(new ItemStack(Things.SATELLITE_MESSENGER.get()), new ItemStack(Things.BEAR_SPRAY.get()), bottle, new ItemStack(Things.MED_KIT.get()),
                new ItemStack(Things.HEADLAMP.get()));
    }

    public static List<Kit> valid(List<Kit> picks) {
        List<Kit> kept = new ArrayList<>();
        for (Kit pick : picks) {
            if (!kept.contains(pick) && kept.size() < Balance.get().kit().picks()) {
                kept.add(pick);
            }
        }
        return kept;
    }
}
