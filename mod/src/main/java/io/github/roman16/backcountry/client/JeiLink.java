package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Cuts;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class JeiLink implements IModPlugin {
    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return Backcountry.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime available) {
        runtime = available;
        IIngredientManager ingredients = available.getIngredientManager();
        ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, ingredients.getAllItemStacks().stream().filter(Cuts::cut).toList());
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    static void recipes(ItemStack stack) {
        if (runtime != null) {
            runtime.getGuiRuntime().getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }
    }

    static boolean typing() {
        return runtime != null && runtime.getGuiRuntime().getIngredientListOverlay().hasKeyboardFocus();
    }
}
