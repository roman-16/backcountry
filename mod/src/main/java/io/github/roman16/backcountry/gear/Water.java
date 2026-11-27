package io.github.roman16.backcountry.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Things;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public record Water(Drink drink, boolean boiled, Source source, float litres) {
    public enum Drink implements StringRepresentable {
        WATER,
        SPRUCE_TEA,
        BIRCH_SAP,
        SEA_WATER;

        public static final Codec<Drink> CODEC = StringRepresentable.fromEnum(Drink::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Source implements StringRepresentable {
        LAKE,
        STREAM;

        public static final Codec<Source> CODEC = StringRepresentable.fromEnum(Source::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Source at(Level level, BlockPos pos) {
            FluidState fluid = level.getFluidState(pos);
            return fluid.is(Things.MOVING_WATER) || fluid.getFlow(level, pos).lengthSqr() > 0 ? STREAM : LAKE;
        }
    }

    public static final Water EMPTY = new Water(Drink.WATER, false, Source.LAKE, 0);
    public static final Codec<Water> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Drink.CODEC.optionalFieldOf("drink", Drink.WATER).forGetter(Water::drink),
            Codec.BOOL.fieldOf("boiled").forGetter(Water::boiled),
            Source.CODEC.optionalFieldOf("source", Source.LAKE).forGetter(Water::source),
            Codec.FLOAT.fieldOf("litres").forGetter(Water::litres))
            .apply(instance, Water::new));
    public static final StreamCodec<ByteBuf, Water> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.idMapper(index -> Drink.values()[index], Drink::ordinal),
            Water::drink, ByteBufCodecs.BOOL, Water::boiled, ByteBufCodecs.idMapper(index -> Source.values()[index], Source::ordinal), Water::source, ByteBufCodecs.FLOAT,
            Water::litres, Water::new);
    private static final float NOTHING = 0.001F;

    public static Water boiled(float litres) {
        return new Water(Drink.WATER, true, Source.LAKE, litres);
    }

    public static Water drawn(Source source, float litres) {
        return new Water(Drink.WATER, false, source, litres);
    }

    public boolean empty() {
        return litres < NOTHING;
    }

    public boolean risky() {
        return drink == Drink.WATER && !boiled;
    }

    public boolean takes(Water poured) {
        return empty() || risky() || !poured.risky();
    }

    public Water add(Water poured, float amount, float capacity) {
        float total = Math.min(capacity, litres + amount);
        if (empty()) {
            return new Water(poured.drink, poured.boiled, poured.source, total);
        }
        Drink mixed = drink == poured.drink ? drink : Drink.WATER;
        boolean safe = !risky() && !poured.risky();
        boolean lake = risky() && source == Source.LAKE || poured.risky() && poured.source == Source.LAKE;
        return new Water(mixed, mixed == Drink.WATER ? safe : boiled && poured.boiled, lake ? Source.LAKE : Source.STREAM, total);
    }

    public Water take(float amount) {
        float left = Math.max(0, litres - amount);
        return left < NOTHING ? EMPTY : new Water(drink, boiled, source, left);
    }

    public Water boil() {
        return new Water(drink, true, source, litres);
    }
}
