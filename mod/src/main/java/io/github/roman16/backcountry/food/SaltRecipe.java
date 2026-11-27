package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public final class SaltRecipe extends CustomRecipe {
    public SaltRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static boolean salts(ItemStack stack) {
        return Pantry.meaty(stack) && Nutrition.of(stack).map(Nutrition::raw).orElse(false) && !stack.has(Things.SALTED.get()) && !Spoilage.spoiled(stack);
    }

    private static ItemStack meat(CraftingInput input) {
        ItemStack meat = ItemStack.EMPTY;
        boolean salt = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Things.SALT.get()) && !salt) {
                salt = true;
            } else if (salts(stack) && meat.isEmpty()) {
                meat = stack;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return salt ? meat : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !meat(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack salted = meat(input).copyWithCount(1);
        if (!salted.isEmpty()) {
            salted.set(Things.SALTED.get(), Unit.INSTANCE);
        }
        return salted;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<SaltRecipe> getSerializer() {
        return Things.SALTING.get();
    }

    public static final class Serializer extends SimpleCraftingRecipeSerializer<SaltRecipe> {
        public Serializer() {
            super(SaltRecipe::new);
        }
    }
}
