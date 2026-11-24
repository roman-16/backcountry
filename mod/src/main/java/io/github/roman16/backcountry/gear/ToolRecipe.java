package io.github.roman16.backcountry.gear;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Things;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class ToolRecipe extends ShapelessRecipe {
    private final List<Ingredient> materials;
    private final Ingredient tool;
    private final ItemStack made;

    public ToolRecipe(List<Ingredient> materials, Ingredient tool, ItemStack made) {
        super("", CraftingBookCategory.MISC, made, all(materials, tool));
        this.materials = materials;
        this.tool = tool;
        this.made = made;
    }

    private static NonNullList<Ingredient> all(List<Ingredient> materials, Ingredient tool) {
        List<Ingredient> all = new ArrayList<>(materials);
        all.add(tool);
        return NonNullList.of(Ingredient.EMPTY, all.toArray(Ingredient[]::new));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack used = input.getItem(slot);
            if (tool.test(used)) {
                ItemStack worn = used.copyWithCount(1);
                worn.setDamageValue(worn.getDamageValue() + 1);
                remaining.set(slot, worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn);
                break;
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Things.TOOL_RECIPE.get();
    }

    public static final class Serializer implements RecipeSerializer<ToolRecipe> {
        private static final MapCodec<ToolRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.materials),
                Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(recipe -> recipe.tool),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.made)).apply(instance, ToolRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ToolRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.materials, Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.tool,
                ItemStack.STREAM_CODEC, recipe -> recipe.made, ToolRecipe::new);

        @Override
        public MapCodec<ToolRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
