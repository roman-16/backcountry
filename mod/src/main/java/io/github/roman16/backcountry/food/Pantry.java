package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class Pantry {
    private Pantry() {}

    public static boolean usable(ItemStack stack) {
        Optional<Nutrition> food = Nutrition.of(stack);
        return food.isPresent() && food.get().kcal() > 0 && !food.get().raw() && food.get().poisoning() <= 0 && !Spoilage.spoiled(stack)
                && !stack.has(Things.CONTAMINATED.get()) && !stack.has(Things.FOULED.get());
    }

    public static double kcal(ItemStack stack) {
        return Nutrition.of(stack).map(food -> food.kcal() * stack.getCount()).orElse(0.0);
    }

    public static boolean cookedMeat(ItemStack stack) {
        return meaty(stack) && Nutrition.of(stack).map(food -> !food.raw() && food.kcal() > 0).orElse(false);
    }

    public static boolean meaty(ItemStack stack) {
        return stack.is(Things.MEAT);
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
