package io.github.roman16.backcountry.food;

import com.mojang.serialization.Codec;
import io.github.roman16.backcountry.Balance;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Age(float days) {
    public static final Codec<Age> CODEC = Codec.FLOAT.xmap(Age::new, Age::days);
    public static final StreamCodec<ByteBuf, Age> STREAM_CODEC = ByteBufCodecs.FLOAT.map(Age::new, Age::days);

    private long window() {
        return (long) Math.floor(days / Balance.get().spoilage().stackDays());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Age age && age.window() == window();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(window());
    }
}
