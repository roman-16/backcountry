package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Cuts;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
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

    static boolean typing() {
        return runtime != null && runtime.getGuiRuntime().getIngredientListOverlay().hasKeyboardFocus();
    }
}
