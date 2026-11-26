package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Backcountry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum Condition {
    SEVERE_HYPOTHERMIA(false, false),
    SEVERE_FROSTBITE(false, false),
    INFECTED_WOUND(true, true),
    FROSTBITE(false, false),
    HYPOTHERMIA(false, false),
    BURNS(true, true),
    GUT_INFECTION(true, false),
    FOOD_POISONING(true, false),
    SCURVY(false, false),
    PARASITES(true, false),
    EXHAUSTED(false, false),
    FROSTNIP(false, false),
    BLISTERS(true, true);

    private final boolean bandaged;
    private final boolean medKit;

    Condition(boolean medKit, boolean bandaged) {
        this.medKit = medKit;
        this.bandaged = bandaged;
    }

    public boolean curedByMedKit() {
        return medKit;
    }

    public boolean takesBandage() {
        return bandaged;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("condition.backcountry." + id());
    }

    public Component began() {
        return Component.translatable("condition.backcountry." + id() + ".began");
    }

    public ResourceLocation icon() {
        return Backcountry.id("hud/" + switch (this) {
            case FROSTNIP, FROSTBITE, SEVERE_FROSTBITE, HYPOTHERMIA, SEVERE_HYPOTHERMIA -> "frost";
            default -> id();
        });
    }

    public boolean deadly() {
        return this == SEVERE_FROSTBITE || this == SEVERE_HYPOTHERMIA;
    }
}
