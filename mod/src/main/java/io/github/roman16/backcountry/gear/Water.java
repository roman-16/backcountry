package io.github.roman16.backcountry.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Water(boolean boiled, float litres) {
    public static final Water EMPTY = new Water(false, 0);
    public static final Codec<Water> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("boiled").forGetter(Water::boiled),
            Codec.FLOAT.fieldOf("litres").forGetter(Water::litres))
            .apply(instance, Water::new));
    public static final StreamCodec<ByteBuf, Water> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Water::boiled, ByteBufCodecs.FLOAT, Water::litres, Water::new);
    private static final float NOTHING = 0.001F;

    public boolean empty() {
        return litres < NOTHING;
    }

    public Water add(boolean boiledWater, float amount, float capacity) {
        float total = Math.min(capacity, litres + amount);
        return new Water((empty() || boiled) && boiledWater, total);
    }

    public Water take(float amount) {
        float left = Math.max(0, litres - amount);
        return left < NOTHING ? EMPTY : new Water(boiled, left);
    }
}
