package io.github.roman16.backcountry.trap;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

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
        return this == SMALL_SNARE || this == LARGE_SNARE || this == DEADFALL;
    }
}
