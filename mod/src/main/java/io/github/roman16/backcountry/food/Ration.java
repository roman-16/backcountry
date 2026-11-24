package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum Ration {
    LEAN,
    NORMAL,
    FEAST;

    public double mealKcal() {
        Balance.Rations rations = Balance.get().rations();
        return switch (this) {
            case LEAN -> rations.leanKcal();
            case NORMAL -> rations.normalKcal();
            case FEAST -> rations.feastKcal();
        };
    }

    public double dayKcal() {
        return 2 * mealKcal();
    }

    public Component title() {
        return Component.translatable("ration.backcountry." + name().toLowerCase(Locale.ROOT));
    }
}
