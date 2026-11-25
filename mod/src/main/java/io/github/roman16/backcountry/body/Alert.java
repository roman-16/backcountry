package io.github.roman16.backcountry.body;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum Alert {
    NEW_TASKS(false),
    SNOW_TOMORROW(true),
    BLIZZARD_TOMORROW(true),
    THUNDERSTORM_TOMORROW(true),
    FROST_TONIGHT(true),
    WEIGHT_DROPPED(false),
    MICE_IN_CACHE(false),
    MOOSE_RUT(false),
    BEARS_FEEDING(false),
    BODY_WASTING(false);

    private final boolean daily;

    Alert(boolean daily) {
        this.daily = daily;
    }

    public boolean daily() {
        return daily;
    }

    public boolean dayLong() {
        return this == NEW_TASKS || this == WEIGHT_DROPPED || this == MICE_IN_CACHE || this == MOOSE_RUT || this == BEARS_FEEDING;
    }

    public boolean danger() {
        return this == BODY_WASTING;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("alert.backcountry." + id());
    }
}
