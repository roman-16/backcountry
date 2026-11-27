package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Cuts;
import io.github.roman16.backcountry.Things;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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

    private static Map<Source.Page, List<ItemStack>> catalysts() {
        return Map.ofEntries(
                Map.entry(Source.Page.GATHERING, List.of()),
                Map.entry(Source.Page.DRESSING, List.of(new ItemStack(Things.KNIFE.get()), new ItemStack(Things.BELT_KNIFE.get()))),
                Map.entry(Source.Page.COOKING_POT, List.of(new ItemStack(Things.COOKING_POT.get()))),
                Map.entry(Source.Page.SMOKER, List.of(new ItemStack(Things.SMOKER_ITEM.get()))),
                Map.entry(Source.Page.DRYING_RACK, List.of(new ItemStack(Things.DRYING_RACK_ITEM.get()))),
                Map.entry(Source.Page.SALTING, List.of(new ItemStack(Things.SALT.get()))),
                Map.entry(Source.Page.CAMP_POT, List.of(new ItemStack(Things.CAMP_POT.get()))),
                Map.entry(Source.Page.BIRCH_TAP, List.of(new ItemStack(Things.BIRCH_TAP.get()))),
                Map.entry(Source.Page.WOODPILE, List.of(new ItemStack(Things.SOFTWOOD.get()))),
                Map.entry(Source.Page.TRAPS, List.of(new ItemStack(Things.SMALL_SNARE_ITEM.get()), new ItemStack(Things.LARGE_SNARE_ITEM.get()),
                        new ItemStack(Things.DEADFALL_ITEM.get()), new ItemStack(Things.MOUSE_TRAP_ITEM.get()), new ItemStack(Things.GILL_NET_ITEM.get()),
                        new ItemStack(Things.TIP_UP_ITEM.get()), new ItemStack(Things.FISH_WHEEL_ITEM.get()))),
                Map.entry(Source.Page.SHARPENING, List.of(new ItemStack(Things.SHARPENING_STONE.get()))),
                Map.entry(Source.Page.MAPS, List.of(new ItemStack(Things.BARK_MAP.get()))),
                Map.entry(Source.Page.LIGHTING, List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()))));
    }

    private static ItemStack icon(Source.Page page) {
        return page == Source.Page.GATHERING ? new ItemStack(Things.PLANT_FIBRE.get()) : catalysts().get(page).getFirst();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (Source.Page page : Source.Page.values()) {
            registration.addRecipeCategories(new SourceCategory(page, gui, icon(page)));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        List<Source> sources = JeiSources.all(Minecraft.getInstance().level);
        for (Source.Page page : Source.Page.values()) {
            registration.addRecipes(SourceCategory.type(page), sources.stream().filter(source -> source.page() == page).toList());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (Cuts.cut(stack)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String info = "jei.backcountry.info." + (id.getNamespace().equals(Backcountry.MODID) ? id.getPath() : id.getNamespace() + "." + id.getPath());
            String hint = ClientEvents.hintKey(id);
            if (I18n.exists(info)) {
                registration.addItemStackInfo(stack, Component.translatable(info));
            } else if (I18n.exists(hint)) {
                registration.addItemStackInfo(stack, Component.translatable(hint));
            }
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                return List.of(Hud.panel());
            }
        });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        catalysts().forEach((page, stacks) -> registration.addRecipeCatalysts(SourceCategory.type(page), stacks.toArray(ItemStack[]::new)));
        registration.addRecipeCatalysts(RecipeTypes.CAMPFIRE_COOKING, new ItemStack(Things.CAMPFIRE_ITEM.get()), new ItemStack(Things.STONE_FIREPLACE_ITEM.get()));
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

    static boolean listShown() {
        return runtime != null && runtime.getGuiRuntime().getIngredientListOverlay().isListDisplayed();
    }

    static boolean typing() {
        return runtime != null && runtime.getGuiRuntime().getIngredientListOverlay().hasKeyboardFocus();
    }
}
