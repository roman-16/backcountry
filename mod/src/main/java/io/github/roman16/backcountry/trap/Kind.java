package io.github.roman16.backcountry.trap;

import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.food.Spoilage;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum Kind {
    SMALL_SNARE(List.of("minecraft:rabbit", "wildernature:squirrel", "backcountry:grouse", "backcountry:ptarmigan"), false, false),
    LARGE_SNARE(List.of("cnc:beaver"), false, false),
    DEADFALL(List.of("minecraft:rabbit", "wildernature:squirrel"), false, true),
    MOUSE_TRAP(List.of(), false, true),
    GILL_NET(List.of(), true, false),
    TIP_UP(List.of(), true, false);

    private final List<String> animals;
    private final boolean fish;
    private final boolean mice;

    Kind(List<String> animals, boolean fish, boolean mice) {
        this.animals = animals;
        this.fish = fish;
        this.mice = mice;
    }

    public List<ResourceLocation> animals() {
        return animals.stream().map(ResourceLocation::parse).toList();
    }

    public boolean fish() {
        return fish;
    }

    public boolean keepsMiceOff() {
        return mice;
    }

    public boolean baited() {
        return this == SMALL_SNARE || this == LARGE_SNARE || this == DEADFALL || this == TIP_UP;
    }

    public Item bait() {
        return this == TIP_UP ? Things.GAME_GUTS.get() : Things.CRICKET.get();
    }

    public boolean takes(ItemStack stack) {
        return baited() && stack.is(bait()) && !Spoilage.spoiled(stack);
    }
}
