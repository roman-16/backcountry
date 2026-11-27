package io.github.roman16.backcountry.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.food.Fish;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public record Nutrition(double kcal, double fat, double protein, double carbs, double vitaminC, boolean raw, double poisoning, Optional<Double> days, double water) {
    public static final Codec<Nutrition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("kcal").forGetter(Nutrition::kcal),
            Codec.DOUBLE.fieldOf("fat").forGetter(Nutrition::fat),
            Codec.DOUBLE.fieldOf("protein").forGetter(Nutrition::protein),
            Codec.DOUBLE.fieldOf("carbs").forGetter(Nutrition::carbs),
            Codec.DOUBLE.optionalFieldOf("vitamin_c", 0.0).forGetter(Nutrition::vitaminC),
            Codec.BOOL.optionalFieldOf("raw", false).forGetter(Nutrition::raw),
            Codec.DOUBLE.optionalFieldOf("poisoning", 0.0).forGetter(Nutrition::poisoning),
            Codec.DOUBLE.optionalFieldOf("days").forGetter(Nutrition::days),
            Codec.DOUBLE.fieldOf("water").forGetter(Nutrition::water))
            .apply(instance, Nutrition::new));

    public static Optional<Nutrition> of(ItemStack stack) {
        Optional<Nutrition> food = Fish.is(stack.getItem()) ? Fish.nutrition(stack) : Optional.ofNullable(stack.getItemHolder().getData(Things.FOOD));
        if (stack.has(Things.DRIED.get())) {
            food = food.map(Nutrition::dried);
        }
        return stack.has(Things.POISONED.get()) ? food.map(Nutrition::poisoned) : food;
    }

    public Nutrition dried() {
        return new Nutrition(kcal, fat, protein, carbs, vitaminC, false, poisoning, days, water * Balance.get().drying().waterShare());
    }

    public Nutrition poisoned() {
        return new Nutrition(kcal, fat, protein, carbs, vitaminC, raw, 1, days, water);
    }
}
