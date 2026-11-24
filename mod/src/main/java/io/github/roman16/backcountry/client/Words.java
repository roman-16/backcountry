package io.github.roman16.backcountry.client;

import java.util.Locale;
import net.minecraft.network.chat.Component;

final class Words {
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final int DAY_START_HOUR = 6;
    private static final int HOURS = 24;

    private Words() {}

    static String kcal(double kcal) {
        return String.format(Locale.ENGLISH, "%,d", Math.round(kcal));
    }

    static String one(double number) {
        return String.format(Locale.ROOT, "%.1f", number);
    }

    static String grams(double grams) {
        return Math.abs(grams - Math.round(grams)) < 0.05 ? Long.toString(Math.round(grams)) : one(grams);
    }

    static String degrees(double celsius) {
        return Math.round(celsius) + " °C";
    }

    static String clock(int hoursIntoDay) {
        return String.format(Locale.ROOT, "%02d:00", (hoursIntoDay + DAY_START_HOUR) % HOURS);
    }

    static String compass(int from) {
        return COMPASS[Math.floorMod(from, COMPASS.length)];
    }

    static String percent(double share) {
        return Math.round(share * 100) + "%";
    }

    static Component comfort(int comfort) {
        return Component.translatable("messenger.backcountry.comfort." + switch (comfort) {
            case -2 -> "very_cold";
            case -1 -> "cold";
            case 1 -> "warm";
            case 2 -> "hot";
            default -> "comfortable";
        });
    }

    static Component trend(int trend) {
        return Component.translatable("messenger.backcountry.trend." + (trend < 0 ? "cooling" : trend > 0 ? "warming" : "steady"));
    }
}
