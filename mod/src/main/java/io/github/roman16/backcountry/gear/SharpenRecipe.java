package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public final class SharpenRecipe extends CustomRecipe {
    public SharpenRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static boolean edged(ItemStack stack) {
        return stack.is(Things.EDGED) && stack.isDamaged();
    }

    private static ItemStack[] parts(CraftingInput input) {
        ItemStack tool = ItemStack.EMPTY;
        ItemStack stone = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Things.SHARPENING_STONE.get()) && stone.isEmpty()) {
                stone = stack;
            } else if (edged(stack) && tool.isEmpty()) {
                tool = stack;
            } else {
                return null;
            }
        }
        return tool.isEmpty() || stone.isEmpty() ? null : new ItemStack[] {tool, stone};
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return parts(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] parts = parts(input);
        if (parts == null) {
            return ItemStack.EMPTY;
        }
        ItemStack tool = parts[0].copyWithCount(1);
        int restored = (int) Math.ceil(tool.getMaxDamage() * Balance.get().kit().sharpenShare());
        tool.setDamageValue(Math.max(0, tool.getDamageValue() - restored));
        return tool;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(Things.SHARPENING_STONE.get())) {
                ItemStack worn = stack.copyWithCount(1);
                worn.setDamageValue(worn.getDamageValue() + 1);
                remaining.set(slot, worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn);
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<SharpenRecipe> getSerializer() {
        return Things.SHARPEN.get();
    }

    public static final class Serializer extends SimpleCraftingRecipeSerializer<SharpenRecipe> {
        public Serializer() {
            super(SharpenRecipe::new);
        }
    }
}
