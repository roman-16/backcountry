package io.github.roman16.backcountry.hunt;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum Track implements StringRepresentable {
    MOOSE,
    CARIBOU,
    BEAR,
    WOLF,
    WOLVERINE,
    HARE;

    public static final Codec<Track> CODEC = StringRepresentable.fromEnum(Track::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean snowOnly() {
        return this == HARE;
    }

    public boolean trails() {
        return this != HARE;
    }

    public Component sign(boolean droppings) {
        return Component.translatable("sign.backcountry." + getSerializedName() + (droppings ? ".droppings" : ".tracks"));
    }
}
