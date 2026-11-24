package io.github.roman16.backcountry.client;

import java.util.Locale;
import net.minecraft.network.chat.Component;

final class Words {
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
