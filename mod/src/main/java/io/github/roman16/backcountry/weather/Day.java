package io.github.roman16.backcountry.weather;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record Day(int day, float driftC, float cloud, float windKmh, int windFrom, int intensity, int fromHour, int toHour, boolean thunder) {
    public static final int DRY = 0;
    public static final int LIGHT = 1;
    public static final int MODERATE = 2;
    public static final int HEAVY = 3;
    public static final StreamCodec<ByteBuf, Day> STREAM_CODEC = StreamCodec.of(Day::write, Day::read);

    public static Day calm(int day) {
        return new Day(day, 0, 0, 0, 0, DRY, 0, 0, false);
    }

    public boolean wet() {
        return intensity != DRY;
    }

    public boolean falling(double hour) {
        return wet() && hour >= fromHour && hour < toHour;
    }

    private static void write(ByteBuf buffer, Day day) {
        buffer.writeInt(day.day);
        buffer.writeFloat(day.driftC);
        buffer.writeFloat(day.cloud);
        buffer.writeFloat(day.windKmh);
        buffer.writeByte(day.windFrom);
        buffer.writeByte(day.intensity);
        buffer.writeByte(day.fromHour);
        buffer.writeByte(day.toHour);
        buffer.writeBoolean(day.thunder);
    }

    private static Day read(ByteBuf buffer) {
        return new Day(buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readByte(), buffer.readByte(), buffer.readByte(),
                buffer.readByte(), buffer.readBoolean());
    }
}
