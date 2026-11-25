package io.github.roman16.backcountry.food;

import com.mojang.serialization.Codec;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

public enum Berry implements StringRepresentable {
    BANEBERRY,
    BLUEBERRY,
    HIGHBUSH_CRANBERRY,
    SALMONBERRY;

    public static final Codec<Berry> CODEC = StringRepresentable.fromEnum(Berry::values);

    public Item berries() {
        return switch (this) {
            case BANEBERRY -> Things.BANEBERRIES.get();
            case BLUEBERRY -> Things.BLUEBERRIES.get();
            case HIGHBUSH_CRANBERRY -> Things.HIGHBUSH_CRANBERRIES.get();
            case SALMONBERRY -> Things.SALMONBERRIES.get();
        };
    }

    public List<Double> months() {
        Balance.Berries berries = Balance.get().berries();
        return switch (this) {
            case BANEBERRY -> berries.baneberryMonths();
            case BLUEBERRY -> berries.blueberryMonths();
            case HIGHBUSH_CRANBERRY -> berries.highbushCranberryMonths();
            case SALMONBERRY -> berries.salmonberryMonths();
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
