package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Backcountry;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Hewing {
    private Hewing() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    static void starting(ServerAboutToStartEvent event) {
        hew(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void reloaded(TagsUpdatedEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && server != null) {
            hew(server);
        }
    }

    private static void hew(MinecraftServer server) {
        RegistryAccess access = server.registryAccess();
        List<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes().stream().<RecipeHolder<?>>map(recipe -> hewn(recipe, access)).toList();
        server.getRecipeManager().replaceRecipes(recipes);
    }

    public static boolean planksByHand(RecipeHolder<?> recipe, RegistryAccess access) {
        return recipe.value() instanceof CraftingRecipe crafting && !(crafting instanceof ToolRecipe) && crafting.getResultItem(access).is(ItemTags.PLANKS)
                && crafting.getIngredients().size() == 1 && Arrays.stream(crafting.getIngredients().getFirst().getItems()).anyMatch(stack -> stack.is(ItemTags.LOGS));
    }

    private static RecipeHolder<?> hewn(RecipeHolder<?> recipe, RegistryAccess access) {
        if (!planksByHand(recipe, access)) {
            return recipe;
        }
        ItemStack[] logs = recipe.value().getIngredients().getFirst().getItems();
        ItemStack[] stripped = Arrays.stream(logs).filter(stack -> stack.is(Tags.Items.STRIPPED_LOGS) || stack.is(Tags.Items.STRIPPED_WOODS)).toArray(ItemStack[]::new);
        Ingredient wood = Ingredient.of(stripped.length > 0 ? stripped : logs);
        return new RecipeHolder<>(recipe.id(), new ToolRecipe(List.of(wood), Ingredient.of(ItemTags.AXES), recipe.value().getResultItem(access).copy()));
    }
}
