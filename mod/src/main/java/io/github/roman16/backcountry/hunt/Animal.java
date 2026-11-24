package io.github.roman16.backcountry.hunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Things;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

public record Animal(TagKey<Biome> habitat, List<Integer> months, List<Integer> group, double perDay, int cap, double kg, List<Double> vital, List<Cut> cuts,
        boolean nearWater, boolean scavenger, Optional<Season> season, double restDays) {
    private static final List<Integer> ALL_YEAR = List.of(1, 12);
    private static final List<Integer> ALONE = List.of(1, 1);
    private static final List<Double> CHEST = List.of(0.4, 0.8);

    public enum Season {
        RUT,
        FEEDING;

        public static final Codec<Season> CODEC = Codec.STRING.xmap(name -> Season.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
                season -> season.name().toLowerCase(java.util.Locale.ROOT));
    }

    public record Cut(Item item, int count) {
        public static final Codec<Cut> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Cut::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Cut::count)).apply(instance, Cut::new));

        public ItemStack stack() {
            return new ItemStack(item, count);
        }
    }

    public static final Codec<Animal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.hashedCodec(Registries.BIOME).fieldOf("habitat").forGetter(Animal::habitat),
            Codec.INT.listOf().optionalFieldOf("months", ALL_YEAR).forGetter(Animal::months),
            Codec.INT.listOf().optionalFieldOf("group", ALONE).forGetter(Animal::group),
            Codec.DOUBLE.fieldOf("per_day").forGetter(Animal::perDay),
            Codec.INT.fieldOf("cap").forGetter(Animal::cap),
            Codec.DOUBLE.fieldOf("kg").forGetter(Animal::kg),
            Codec.DOUBLE.listOf().optionalFieldOf("vital", CHEST).forGetter(Animal::vital),
            Cut.CODEC.listOf().optionalFieldOf("cuts", List.of()).forGetter(Animal::cuts),
            Codec.BOOL.optionalFieldOf("near_water", false).forGetter(Animal::nearWater),
            Codec.BOOL.optionalFieldOf("scavenger", false).forGetter(Animal::scavenger),
            Season.CODEC.optionalFieldOf("season").forGetter(Animal::season),
            Codec.DOUBLE.optionalFieldOf("rest_days", 0.0).forGetter(Animal::restDays)).apply(instance, Animal::new));

    public static Optional<Animal> of(EntityType<?> type) {
        return Optional.ofNullable(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).getData(Things.ANIMAL));
    }

    public boolean inSeason(int month) {
        return month >= months.get(0) && month <= months.get(1);
    }
}
