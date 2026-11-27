package io.github.roman16.backcountry.guide;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public sealed interface Check {
    record Held(Thing thing, int count) implements Check {}

    record Did(Deed deed) implements Check {}

    record Placed(Supplier<? extends Block> block) implements Check {}

    record Sheltered() implements Check {}

    record WearsHidePack() implements Check {}

    record FoodInACache() implements Check {}

    record FoodStores() implements Check {}

    record DryFirewood() implements Check {}

    record Made(Supplier<? extends Item> item) implements Check {}

    record Reach(int month) implements Check {}

    record YearsEnd() implements Check {}

    record InCabin() implements Check {}

    record WarmCabin() implements Check {}

    record TallCache() implements Check {}

    record Surveys(int count) implements Check {}

    record Noted(int count) implements Check {}
}
