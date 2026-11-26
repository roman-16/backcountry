package io.github.roman16.backcountry.survivor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Summary(Kind kind, int day, double bmi, double weightKg, double arrivedKg, int days, double kcal, List<Taken> animals, List<Float> weights, List<Line> survivors) {
    public enum Kind {
        DIED,
        EXTRACTED,
        LIVING,
        TAPPED_OUT,
        YEAR;

        public static final Codec<Kind> CODEC = Codec.STRING.xmap(name -> Kind.valueOf(name.toUpperCase(Locale.ROOT)), kind -> kind.name().toLowerCase(Locale.ROOT));
    }

    public record Taken(String entity, int count) {
        public static final Codec<Taken> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("entity").forGetter(Taken::entity),
                Codec.INT.fieldOf("count").forGetter(Taken::count))
                .apply(instance, Taken::new));
    }

    public record Line(String name, double fromKg, double toKg, int arrivedDay, int endedDay, Kind end) {
        public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Line::name),
                Codec.DOUBLE.fieldOf("from_kg").forGetter(Line::fromKg),
                Codec.DOUBLE.fieldOf("to_kg").forGetter(Line::toKg),
                Codec.INT.fieldOf("arrived_day").forGetter(Line::arrivedDay),
                Codec.INT.fieldOf("ended_day").forGetter(Line::endedDay),
                Kind.CODEC.fieldOf("end").forGetter(Line::end))
                .apply(instance, Line::new));
    }

    public static final Codec<Summary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(Summary::kind),
            Codec.INT.fieldOf("day").forGetter(Summary::day),
            Codec.DOUBLE.fieldOf("bmi").forGetter(Summary::bmi),
            Codec.DOUBLE.fieldOf("weight_kg").forGetter(Summary::weightKg),
            Codec.DOUBLE.fieldOf("arrived_kg").forGetter(Summary::arrivedKg),
            Codec.INT.fieldOf("days").forGetter(Summary::days),
            Codec.DOUBLE.fieldOf("kcal").forGetter(Summary::kcal),
            Taken.CODEC.listOf().fieldOf("animals").forGetter(Summary::animals),
            Codec.FLOAT.listOf().fieldOf("weights").forGetter(Summary::weights),
            Line.CODEC.listOf().fieldOf("survivors").forGetter(Summary::survivors))
            .apply(instance, Summary::new));

    public static final StreamCodec<ByteBuf, Summary> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
