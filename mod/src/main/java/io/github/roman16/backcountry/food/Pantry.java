package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class Pantry {
    private static final double PROTEIN_KCAL_PER_GRAM = 4;
    private static final double MEATY_SHARE = 0.5;

    private Pantry() {}

    public static boolean usable(ItemStack stack) {
        Optional<Nutrition> food = Nutrition.of(stack);
        return food.isPresent() && food.get().kcal() > 0 && !food.get().raw() && food.get().poisoning() <= 0 && !Spoilage.spoiled(stack)
                && !stack.has(Things.CONTAMINATED.get());
    }

    public static double kcal(ItemStack stack) {
        return Nutrition.of(stack).map(food -> food.kcal() * stack.getCount()).orElse(0.0);
    }

    public static boolean cookedMeat(ItemStack stack) {
        return Nutrition.of(stack).map(food -> !food.raw() && food.kcal() > 0 && food.protein() * PROTEIN_KCAL_PER_GRAM >= food.kcal() * MEATY_SHARE).orElse(false);
    }

    public static boolean meaty(ItemStack stack) {
        return Nutrition.of(stack).map(food -> food.raw() || food.protein() * PROTEIN_KCAL_PER_GRAM >= food.kcal() * MEATY_SHARE).orElse(false);
    }

    public static double usableKcal(List<ItemStack> stacks) {
        return stacks.stream().filter(Pantry::usable).mapToDouble(Pantry::kcal).sum();
    }

    public static double meatyKcal(List<ItemStack> stacks) {
        return stacks.stream().filter(Pantry::meaty).mapToDouble(Pantry::kcal).sum();
    }

    public static Comparator<ItemStack> soonestSpoiling(double airC) {
        return Comparator.comparingDouble(stack -> Spoilage.daysLeft(stack, airC).orElse(Double.MAX_VALUE));
    }
}
