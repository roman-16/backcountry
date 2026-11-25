package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Cuts;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class CutsInJei implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Backcountry.id("cuts");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        IIngredientManager ingredients = runtime.getIngredientManager();
        ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, ingredients.getAllItemStacks().stream().filter(Cuts::cut).toList());
    }
}
