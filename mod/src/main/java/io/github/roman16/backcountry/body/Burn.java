package io.github.roman16.backcountry.body;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum Burn {
    RESTING,
    MOVING,
    LOAD,
    WORK,
    COLD,
    DIGESTION;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("messenger.backcountry.burn." + id());
    }
}
