package io.github.roman16.backcountry.weather;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record Outlook(int day, Kind sky, Kind falling, int chance, float highC, float lowC, float windKmh, int windFrom) {
    public static final StreamCodec<ByteBuf, Outlook> STREAM_CODEC = StreamCodec.of(Outlook::write, Outlook::read);

    public boolean likely(double share) {
        return chance >= share * 100;
    }

    private static void write(ByteBuf buffer, Outlook outlook) {
        buffer.writeInt(outlook.day);
        buffer.writeByte(outlook.sky.ordinal());
        buffer.writeByte(outlook.falling.ordinal());
        buffer.writeByte(outlook.chance);
        buffer.writeFloat(outlook.highC);
        buffer.writeFloat(outlook.lowC);
        buffer.writeFloat(outlook.windKmh);
        buffer.writeByte(outlook.windFrom);
    }

    private static Outlook read(ByteBuf buffer) {
        return new Outlook(buffer.readInt(), Kind.values()[buffer.readByte()], Kind.values()[buffer.readByte()], buffer.readByte(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readByte());
    }
}
