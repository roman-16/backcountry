package io.github.roman16.backcountry;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.food.Fish;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Cuts {
    private static final Spec SPEC = read();
    private static Set<Item> items;
    private static Set<Item> stranded = Set.of();

    private Cuts() {}

    private record Spec(Set<ResourceLocation> creatures, Set<String> fish, Set<ResourceLocation> found, Map<String, List<ResourceLocation>> items, Map<String, Set<String>> mods) {}

    public static boolean cut(Item item) {
        return items().contains(item);
    }

    public static boolean cut(ItemStack stack) {
        return cut(stack.getItem());
    }

    public static boolean cut(EntityType<?> type) {
        return SPEC.creatures().contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static boolean livesOnTheIsland(ResourceLocation species) {
        return SPEC.fish().contains(species.getPath());
    }

    public static List<ItemStack> fish() {
        return SPEC.fish().stream().sorted().map(name -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fish.MOD, name)))).toList();
    }

    public static synchronized Set<Item> items() {
        if (items == null) {
            Set<Item> listed = new HashSet<>();
            SPEC.items().values().forEach(ids -> ids.forEach(id -> BuiltInRegistries.ITEM.getOptional(id).ifPresent(listed::add)));
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                Set<String> kept = SPEC.mods().get(id.getNamespace());
                if (kept != null && !kept.contains(id.getPath()) || item instanceof SpawnEggItem || Fish.is(item) && !livesOnTheIsland(id)) {
                    listed.add(item);
                }
            }
            items = Set.copyOf(listed);
        }
        return items;
    }

    public static List<String> unknown() {
        List<String> missing = new ArrayList<>();
        SPEC.items().values().forEach(ids -> ids.stream().filter(id -> !BuiltInRegistries.ITEM.containsKey(id)).forEach(id -> missing.add(id.toString())));
        SPEC.creatures().stream().filter(id -> !BuiltInRegistries.ENTITY_TYPE.containsKey(id)).forEach(id -> missing.add(id.toString()));
        SPEC.found().stream().filter(id -> !BuiltInRegistries.ITEM.containsKey(id)).forEach(id -> missing.add(id.toString()));
        SPEC.fish().stream().map(name -> ResourceLocation.fromNamespaceAndPath(Fish.MOD, name)).filter(id -> !BuiltInRegistries.ITEM.containsKey(id))
                .forEach(id -> missing.add(id.toString()));
        SPEC.mods().forEach((mod, kept) -> kept.stream().map(path -> ResourceLocation.fromNamespaceAndPath(mod, path)).filter(id -> !BuiltInRegistries.ITEM.containsKey(id))
                .forEach(id -> missing.add(id.toString())));
        return missing;
    }

    public static Set<Item> stranded() {
        return stranded;
    }

    @SubscribeEvent
    static void starting(ServerAboutToStartEvent event) {
        dropRecipes(event.getServer());
    }

    @SubscribeEvent
    static void reloaded(TagsUpdatedEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && server != null) {
            dropRecipes(server);
        }
    }

    private static void dropRecipes(MinecraftServer server) {
        RegistryAccess access = server.registryAccess();
        Collection<RecipeHolder<?>> all = server.getRecipeManager().getRecipes();
        List<RecipeHolder<?>> kept = all.stream().filter(recipe -> !makesACutItem(recipe, access) && !needsACutItem(recipe) && !cooksFoodInAFurnace(recipe, access)).toList();
        Set<Item> made = kept.stream().map(recipe -> recipe.value().getResultItem(access).getItem()).collect(Collectors.toSet());
        stranded = all.stream().filter(Cuts::needsACutItem).map(recipe -> recipe.value().getResultItem(access).getItem())
                .filter(item -> !made.contains(item) && !cut(item) && !SPEC.found().contains(BuiltInRegistries.ITEM.getKey(item))).collect(Collectors.toUnmodifiableSet());
        server.getRecipeManager().replaceRecipes(kept);
    }

    private static boolean makesACutItem(RecipeHolder<?> recipe, RegistryAccess access) {
        return cut(recipe.value().getResultItem(access));
    }

    private static boolean needsACutItem(RecipeHolder<?> recipe) {
        return recipe.value().getIngredients().stream().anyMatch(Cuts::onlyCut);
    }

    private static boolean onlyCut(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length > 0 && Arrays.stream(options).allMatch(Cuts::cut);
    }

    public static boolean cooksFoodInAFurnace(RecipeHolder<?> recipe, RegistryAccess access) {
        return recipe.value().getType() == RecipeType.SMELTING && recipe.value().getResultItem(access).has(DataComponents.FOOD);
    }

    @SubscribeEvent
    static void joining(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ExperienceOrb || cut(event.getEntity().getType())) {
            event.setCanceled(true);
        }
    }

    private static Spec read() {
        try (InputStream stream = Cuts.class.getResourceAsStream("/backcountry/cuts.toml")) {
            if (stream == null) {
                throw new IllegalStateException("backcountry/cuts.toml is missing from the mod jar");
            }
            Config table = new TomlParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            Map<String, List<ResourceLocation>> groups = new TreeMap<>();
            table.<Config>get("items").entrySet().forEach(entry -> groups.put(entry.getKey(), ids(entry.getValue())));
            Map<String, Set<String>> mods = new TreeMap<>();
            table.<Config>get("mods").entrySet().forEach(entry -> mods.put(entry.getKey(), entry.<List<?>>getValue().stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet())));
            return new Spec(Set.copyOf(ids(table.get("creatures"))), table.<List<?>>get("fish").stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet()),
                    Set.copyOf(ids(table.get("found"))), Map.copyOf(groups), Map.copyOf(mods));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static List<ResourceLocation> ids(List<?> values) {
        return values.stream().map(value -> ResourceLocation.parse(String.valueOf(value))).toList();
    }

    public static final class Loot extends LootModifier {
        public static final MapCodec<Loot> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, Loot::new));

        public Loot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            loot.removeIf(Cuts::cut);
            return loot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
