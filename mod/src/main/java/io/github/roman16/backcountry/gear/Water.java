package io.github.roman16.backcountry.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public record Water(Drink drink, boolean boiled, float litres) {
    public enum Drink implements StringRepresentable {
        WATER,
        SPRUCE_TEA,
        BIRCH_SAP;

        public static final Codec<Drink> CODEC = StringRepresentable.fromEnum(Drink::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Water EMPTY = new Water(Drink.WATER, false, 0);
    public static final Codec<Water> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Drink.CODEC.optionalFieldOf("drink", Drink.WATER).forGetter(Water::drink),
            Codec.BOOL.fieldOf("boiled").forGetter(Water::boiled),
            Codec.FLOAT.fieldOf("litres").forGetter(Water::litres))
            .apply(instance, Water::new));
    public static final StreamCodec<ByteBuf, Water> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.idMapper(index -> Drink.values()[index], Drink::ordinal),
            Water::drink, ByteBufCodecs.BOOL, Water::boiled, ByteBufCodecs.FLOAT, Water::litres, Water::new);
    private static final float NOTHING = 0.001F;

    public static Water of(boolean boiled, float litres) {
        return new Water(Drink.WATER, boiled, litres);
    }

    public boolean empty() {
        return litres < NOTHING;
    }

    public Water add(Water poured, float amount, float capacity) {
        float total = Math.min(capacity, litres + amount);
        Drink mixed = empty() || drink == poured.drink ? poured.drink : Drink.WATER;
        return new Water(mixed, (empty() || boiled) && poured.boiled, total);
    }

    public Water take(float amount) {
        float left = Math.max(0, litres - amount);
        return left < NOTHING ? EMPTY : new Water(drink, boiled, left);
    }

    public Water boil() {
        return new Water(drink, true, litres);
    }
}
