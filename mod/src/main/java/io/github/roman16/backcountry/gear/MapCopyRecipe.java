package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Things;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class MapCopyRecipe extends CustomRecipe {
    public MapCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !drawn(input).isEmpty() && blanks(input) > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return drawn(input).copyWithCount(blanks(input) + 1);
    }

    private static ItemStack drawn(CraftingInput input) {
        ItemStack drawn = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(Things.BARK_MAP.get())) {
                return ItemStack.EMPTY;
            }
            if (stack.has(Things.MAP_SHEET.get())) {
                if (!drawn.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                drawn = stack;
            }
        }
        return drawn;
    }

    private static int blanks(CraftingInput input) {
        int blanks = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(Things.BARK_MAP.get()) && !stack.has(Things.MAP_SHEET.get())) {
                blanks++;
            }
        }
        return blanks;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Things.MAP_COPY.get();
    }
}
