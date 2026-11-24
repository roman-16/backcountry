package io.github.roman16.backcountry.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Things;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public record Nutrition(double kcal, double fat, double protein, double carbs, double vitaminC, boolean raw, double poisoning) {
    public static final Codec<Nutrition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("kcal").forGetter(Nutrition::kcal),
            Codec.DOUBLE.fieldOf("fat").forGetter(Nutrition::fat),
            Codec.DOUBLE.fieldOf("protein").forGetter(Nutrition::protein),
            Codec.DOUBLE.fieldOf("carbs").forGetter(Nutrition::carbs),
            Codec.DOUBLE.optionalFieldOf("vitamin_c", 0.0).forGetter(Nutrition::vitaminC),
            Codec.BOOL.optionalFieldOf("raw", false).forGetter(Nutrition::raw),
            Codec.DOUBLE.optionalFieldOf("poisoning", 0.0).forGetter(Nutrition::poisoning))
            .apply(instance, Nutrition::new));

    public static Optional<Nutrition> of(ItemStack stack) {
        return Optional.ofNullable(stack.getItemHolder().getData(Things.FOOD));
    }
}
