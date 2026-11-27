package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.gear.ItemRules;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.Worn;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.Tags;

public final class Load {
    private static final ItemRules WEIGHTS = new ItemRules(Balance.get().weight());
    private Load() {}

    public static double kg(ServerPlayer player, Body body) {
        double total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            total += kg(player.getInventory().getItem(slot));
        }
        for (Worn worn : Worn.values()) {
            total += kg(worn.get(player));
        }
        return total;
    }

    public static double kg(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Balance.Load load = Balance.get().load();
        double each = load.itemKg();
        var nutrition = Nutrition.of(stack);
        Optional<Double> weighed = WEIGHTS.find(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (weighed.isPresent()) {
            each = weighed.get();
        } else if (nutrition.isPresent()) {
            each = nutrition.get().kcal() * load.foodKgPerKcal() * (stack.has(Things.DRIED.get()) ? Balance.get().drying().weightShare() : 1);
        } else if (stack.getItem() instanceof FirewoodItem) {
            each = load.firewoodKg();
        } else if (stack.is(ItemTags.LOGS)) {
            each = load.logKg();
        } else if (stack.getItem() instanceof BlockItem block && (block.getBlock().defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD)
                || block.getBlock().defaultBlockState().is(Tags.Blocks.ORES) || block.getBlock().defaultBlockState().is(Tags.Blocks.COBBLESTONES))) {
            each = load.stoneKg();
        } else if (stack.getItem() instanceof BlockItem) {
            each = load.blockKg();
        } else if (stack.getItem() instanceof ArmorItem) {
            each = load.clothingKg();
        } else if (stack.getItem() instanceof TieredItem) {
            each = load.toolKg();
        }
        double contents = stack.getOrDefault(Things.WATER.get(), Water.EMPTY).litres();
        for (ItemStack inside : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
            contents += kg(inside);
        }
        return each * stack.getCount() + contents;
    }
}
