package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.survivor.Team;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class Fish {
    public static final String CAUGHT = "fish";
    private static final String MOD = "riverfishing";
    private static final String KIND = "FishItem";
    private static final double GRAMS_A_KG = 1000;

    private Fish() {}

    public static boolean is(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(MOD) && item.getClass().getSimpleName().equals(KIND);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static Optional<Nutrition> nutrition(ItemStack stack) {
        CompoundTag data = data(stack);
        if (!data.contains("WeightG")) {
            return Optional.empty();
        }
        Balance.Fish fish = Balance.get().fish();
        double kg = data.getInt("WeightG") / GRAMS_A_KG;
        boolean raw = !data.getBoolean("Cooked");
        double poisoning = stack.has(Things.SPAWNED_OUT.get()) ? 1 : 0;
        return Optional.of(new Nutrition(kg * fish.kcalPerKg(), kg * fish.fatGPerKg(), kg * fish.proteinGPerKg(), 0, raw ? kg * fish.vitaminCMgPerKg() : 0, raw, poisoning,
                Optional.of(raw ? fish.rawDays() : fish.cookedDays()), 0));
    }

    public static void land(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !is(stack.getItem()) || stack.has(Things.LANDED.get()) || !data(stack).contains("WeightG")) {
            return;
        }
        stack.set(Things.LANDED.get(), Unit.INSTANCE);
        if (data(stack).getString("Species").contains("salmon") && Hunting.months(Seasons.month(player.level()), Balance.get().fish().spawnedOutMonths())) {
            stack.set(Things.SPAWNED_OUT.get(), Unit.INSTANCE);
        }
        if (Metabolism.lives(player)) {
            Metabolism.body(player).animals.merge(CAUGHT, stack.getCount(), Integer::sum);
            Team.get(player.server).caught(stack.getCount());
        }
    }
}
