package io.github.roman16.backcountry.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WeatherView(float airC, float feelsC, Optional<Kind> falling, int intensity, int fromHour, int toHour, boolean now, float windKmh, int windFrom, float cloud,
        float highC, float lowC, List<Outlook> outlooks) {
    public static final WeatherView NONE = new WeatherView(0, 0, Optional.empty(), Day.DRY, 0, 0, false, 0, 0, 0, 0, 0, List.of());
    public static final StreamCodec<RegistryFriendlyByteBuf, WeatherView> STREAM_CODEC = StreamCodec.of(WeatherView::write, WeatherView::read);

    private static void write(RegistryFriendlyByteBuf buffer, WeatherView view) {
        buffer.writeFloat(view.airC);
        buffer.writeFloat(view.feelsC);
        buffer.writeByte(view.falling.map(Kind::ordinal).orElse(-1));
        buffer.writeByte(view.intensity);
        buffer.writeByte(view.fromHour);
        buffer.writeByte(view.toHour);
        buffer.writeBoolean(view.now);
        buffer.writeFloat(view.windKmh);
        buffer.writeByte(view.windFrom);
        buffer.writeFloat(view.cloud);
        buffer.writeFloat(view.highC);
        buffer.writeFloat(view.lowC);
        buffer.writeVarInt(view.outlooks.size());
        view.outlooks.forEach(outlook -> Outlook.STREAM_CODEC.encode(buffer, outlook));
    }

    private static WeatherView read(RegistryFriendlyByteBuf buffer) {
        float air = buffer.readFloat();
        float feels = buffer.readFloat();
        int falling = buffer.readByte();
        int intensity = buffer.readByte();
        int from = buffer.readByte();
        int to = buffer.readByte();
        boolean now = buffer.readBoolean();
        float wind = buffer.readFloat();
        int windFrom = buffer.readByte();
        float cloud = buffer.readFloat();
        float high = buffer.readFloat();
        float low = buffer.readFloat();
        int count = buffer.readVarInt();
        List<Outlook> outlooks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            outlooks.add(Outlook.STREAM_CODEC.decode(buffer));
        }
        return new WeatherView(air, feels, falling < 0 ? Optional.empty() : Optional.of(Kind.values()[falling]), intensity, from, to, now, wind, windFrom, cloud, high, low,
                List.copyOf(outlooks));
    }
}
