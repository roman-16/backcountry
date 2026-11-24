package io.github.roman16.backcountry.weather;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum Kind {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SNOW,
    THUNDERSTORM,
    BLIZZARD;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ResourceLocation icon() {
        return Backcountry.id("hud/" + id());
    }

    public Component title() {
        return Component.translatable("weather.backcountry." + id());
    }

    public Component title(int intensity) {
        return this == RAIN || this == SNOW ? Component.translatable("weather.backcountry." + id() + "." + intensity) : title();
    }

    public static Kind sky(double cloud) {
        Balance.Weather rules = Balance.get().weather();
        return cloud < rules.clearBelow() ? CLEAR : cloud < rules.cloudyAbove() ? PARTLY_CLOUDY : CLOUDY;
    }

    public static Kind falling(double airC, double windKmh, boolean thunder) {
        Balance.Weather rules = Balance.get().weather();
        if (airC <= rules.snowBelowC()) {
            return windKmh >= rules.blizzardWindKmh() ? BLIZZARD : SNOW;
        }
        return thunder ? THUNDERSTORM : RAIN;
    }
}
