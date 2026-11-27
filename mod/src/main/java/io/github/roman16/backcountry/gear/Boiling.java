package io.github.roman16.backcountry.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Boiling(Water water, double ticks, boolean needles) {
    public static final Codec<Boiling> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Water.CODEC.fieldOf("water").forGetter(Boiling::water),
            Codec.DOUBLE.fieldOf("ticks").forGetter(Boiling::ticks),
            Codec.BOOL.optionalFieldOf("needles", false).forGetter(Boiling::needles)).apply(instance, Boiling::new));
    public static final StreamCodec<ByteBuf, Boiling> STREAM_CODEC = StreamCodec.composite(Water.STREAM_CODEC, Boiling::water, ByteBufCodecs.DOUBLE, Boiling::ticks,
            ByteBufCodecs.BOOL, Boiling::needles, Boiling::new);
}
