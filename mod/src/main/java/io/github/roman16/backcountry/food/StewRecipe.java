package io.github.roman16.backcountry.food;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Things;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record StewRecipe(List<Ingredient> ingredients, ItemStack result) implements Recipe<StewRecipe.Pot> {
    public record Pot(List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    @Override
    public boolean matches(Pot pot, Level level) {
        List<ItemStack> left = new ArrayList<>(pot.items().stream().filter(stack -> !stack.isEmpty()).toList());
        if (left.size() != ingredients.size()) {
            return false;
        }
        for (Ingredient ingredient : ingredients) {
            ItemStack found = left.stream().filter(ingredient).findFirst().orElse(null);
            if (found == null) {
                return false;
            }
            left.remove(found);
        }
        return true;
    }

    public boolean takes(ItemStack stack) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    @Override
    public ItemStack assemble(Pot pot, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Things.STEW.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Things.STEWING.get();
    }

    public static final class Serializer implements RecipeSerializer<StewRecipe> {
        private static final MapCodec<StewRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(StewRecipe::ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(StewRecipe::result)).apply(instance, StewRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, StewRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), StewRecipe::ingredients, ItemStack.STREAM_CODEC, StewRecipe::result, StewRecipe::new);

        @Override
        public MapCodec<StewRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StewRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
