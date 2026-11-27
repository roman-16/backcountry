package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Cuts;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.food.StewRecipe;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.trap.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

final class JeiSources {
    private static final int PERCENT = 100;
    private static final int OUTPUT_LIMIT = 15;

    private JeiSources() {}

    static List<Source> all(Level level) {
        List<Source> sources = new ArrayList<>();
        gathering(sources);
        dressing(sources);
        stews(sources, level.getRecipeManager(), level);
        smoking(sources, level.getRecipeManager(), level);
        preserving(sources);
        campPot(sources);
        birchTap(sources);
        woodpile(sources);
        traps(sources);
        sharpening(sources);
        maps(sources);
        lighting(sources);
        return sources.stream().filter(source -> source.outputs().stream().noneMatch(Cuts::cut)).toList();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static List<ItemStack> tagged(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag).map(set -> set.stream().map(holder -> new ItemStack(holder.value())).filter(stack -> !Cuts.cut(stack)).toList())
                .orElse(List.of());
    }

    private static Component chance(double share) {
        return Component.translatable("jei.backcountry.chance", Math.round(share * PERCENT));
    }

    private static void gathering(List<Source> sources) {
        Balance balance = Balance.get();
        List<ItemStack> grass = List.of(new ItemStack(Items.SHORT_GRASS), new ItemStack(Items.TALL_GRASS), new ItemStack(Items.FERN), new ItemStack(Items.LARGE_FERN));
        sources.add(new Source(Source.Page.GATHERING, List.of(grass), List.of(new ItemStack(Things.PLANT_FIBRE.get())), chance(balance.plantFibre().chance())));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.SPRUCE_LEAVES)), tagged(Things.KNIVES)),
                List.of(new ItemStack(Things.SPRUCE_NEEDLES.get())), Component.translatable("jei.backcountry.gathering.needles")));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.BIRCH_LOG)), tagged(ItemTags.AXES)), List.of(new ItemStack(Things.BIRCH_BARK.get())),
                Component.translatable("jei.backcountry.gathering.bark")));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(stack("projectvibrantjourneys:ferrous_gravel")), tagged(ItemTags.SHOVELS)),
                List.of(new ItemStack(Items.RAW_IRON), new ItemStack(Items.GRAVEL)), Component.translatable("jei.backcountry.gathering.iron", Math.round(balance.mining().rawIronChance() * PERCENT))));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.GRAVEL)), tagged(ItemTags.SHOVELS)), List.of(new ItemStack(Items.FLINT)),
                Component.translatable("jei.backcountry.gathering.flint")));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.GRASS_BLOCK), new ItemStack(Items.DIRT)), tagged(ItemTags.SHOVELS)),
                List.of(stack("riverfishing:worm")), Component.translatable("jei.backcountry.gathering.worm", Math.round(balance.bait().dryWormChance() * PERCENT),
                        Math.round(balance.bait().rainWormChance() * PERCENT))));
        sources.add(new Source(Source.Page.GATHERING, List.of(grass), List.of(new ItemStack(Things.CRICKET.get())),
                Component.translatable("jei.backcountry.gathering.cricket", Math.round(balance.bait().cricketChance() * PERCENT))));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(stack("projectvibrantjourneys:natural_cobweb")), tagged(Things.KNIVES)), List.of(new ItemStack(Items.STRING)),
                Component.translatable("jei.backcountry.gathering.string")));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.SAND)), tagged(ItemTags.SHOVELS)), List.of(new ItemStack(Things.CLAMS.get())),
                Component.translatable("jei.backcountry.gathering.clams", Math.round(balance.coast().clamChance() * PERCENT))));
        sources.add(new Source(Source.Page.GATHERING, List.of(), List.of(new ItemStack(Things.MUSSELS.get())), Component.translatable("jei.backcountry.gathering.mussels")));
        sources.add(new Source(Source.Page.GATHERING, List.of(), List.of(FirewoodItem.seasoned(new ItemStack(Things.SOFTWOOD.get()), 1)),
                Component.translatable("jei.backcountry.gathering.driftwood")));
        sources.add(new Source(Source.Page.GATHERING, List.of(), List.of(new ItemStack(Things.GOOSE_EGG.get())), Component.translatable("jei.backcountry.gathering.goose_eggs")));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.PODZOL)), tagged(ItemTags.SHOVELS)),
                List.of(new ItemStack(Things.SPRUCE_ROOTS.get())), Component.translatable("jei.backcountry.gathering.roots", Math.round(balance.forage().rootsChance() * PERCENT))));
        sources.add(new Source(Source.Page.GATHERING, List.of(List.of(new ItemStack(Items.SPRUCE_LOG)), tagged(ItemTags.AXES), tagged(Things.KNIVES)),
                List.of(new ItemStack(Things.SPRUCE_PITCH.get())), Component.translatable("jei.backcountry.gathering.pitch", Math.round(balance.trail().pitchDays()))));
        sources.add(new Source(Source.Page.GATHERING, List.of(tagged(Things.TINDER_FUNGI), List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()))),
                List.of(new ItemStack(Things.SMOULDERING_FUNGUS.get())), Component.translatable("jei.backcountry.gathering.fungus", Math.round(balance.fire().fungusHours()))));
    }

    private static void dressing(List<Source> sources) {
        for (Map.Entry<ResourceKey<EntityType<?>>, Animal> entry : BuiltInRegistries.ENTITY_TYPE.getDataMap(Things.ANIMAL).entrySet()) {
            Animal animal = entry.getValue();
            if (animal.cuts().isEmpty()) {
                continue;
            }
            ItemStack carcass = new ItemStack(Things.CARCASS_ITEM.get());
            carcass.set(Things.ANIMAL_TYPE.get(), entry.getKey().location());
            int fattest = animal.fat().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<ItemStack> cuts = animal.cuts().stream().map(cut -> new ItemStack(cut.item(), cut.item() == Things.ANIMAL_FAT.get() && fattest > 0 ? fattest : cut.count()))
                    .toList();
            Component note = animal.kg() >= Balance.get().hunting().carryKg() ? Component.translatable("jei.backcountry.dressing.heavy", Math.round(animal.kg()))
                    : Component.translatable("jei.backcountry.dressing", Math.round(animal.kg()));
            String habitat = animal.habitat().location().getPath();
            note = note.copy().append(" ").append(Component.translatable("jei.backcountry.dressing.lives", Component.translatable("jei.backcountry.habitat." + habitat),
                    animal.months().equals(List.of(1, 12)) ? Component.translatable("jei.backcountry.all_year")
                            : Component.translatable("jei.backcountry.months", Words.month(animal.months().get(0)), Words.month(animal.months().get(1)))));
            if (fattest > 0) {
                note = note.copy().append(" ").append(Component.translatable("jei.backcountry.dressing.fat", animal.fat().stream().mapToInt(Integer::intValue).min().orElse(0),
                        fattest));
            }
            sources.add(new Source(Source.Page.DRESSING, List.of(List.of(carcass), tagged(Things.KNIVES)), cuts, note));
        }
    }

    private static void stews(List<Source> sources, RecipeManager recipes, Level level) {
        for (RecipeHolder<StewRecipe> holder : recipes.getAllRecipesFor(Things.STEWING.get())) {
            StewRecipe recipe = holder.value();
            List<List<ItemStack>> inputs = recipe.ingredients().stream().map(ingredient -> Arrays.stream(ingredient.getItems()).filter(stack -> !Cuts.cut(stack)).toList()).toList();
            sources.add(new Source(Source.Page.COOKING_POT, inputs, List.of(recipe.result().copyWithCount(recipe.servings())),
                    Component.translatable("jei.backcountry.cooking_pot", recipe.servings())));
        }
    }

    private static void smoking(List<Source> sources, RecipeManager recipes, Level level) {
        for (var holder : recipes.getAllRecipesFor(RecipeType.SMOKING)) {
            ItemStack[] raw = holder.value().getIngredients().getFirst().getItems();
            if (raw.length == 0 || Nutrition.of(raw[0]).filter(Nutrition::raw).isEmpty()) {
                continue;
            }
            ItemStack smoked = holder.value().getResultItem(level.registryAccess()).copy();
            smoked.set(Things.SMOKED.get(), net.minecraft.util.Unit.INSTANCE);
            sources.add(new Source(Source.Page.SMOKER, List.of(Arrays.asList(raw), List.of(new ItemStack(Things.SOFTWOOD.get()), new ItemStack(Things.HARDWOOD.get()))),
                    List.of(smoked), Component.translatable("jei.backcountry.smoker", Balance.get().smoker().batchHours())));
        }
    }

    private static void preserving(List<Source> sources) {
        for (ItemStack raw : tagged(TagKey.create(net.minecraft.core.registries.Registries.ITEM, io.github.roman16.backcountry.Backcountry.id("dryable")))) {
            ItemStack dried = raw.copy();
            dried.set(Things.DRIED.get(), net.minecraft.util.Unit.INSTANCE);
            sources.add(new Source(Source.Page.DRYING_RACK, List.of(List.of(raw)), List.of(dried),
                    Component.translatable("jei.backcountry.drying_rack", Balance.get().drying().days())));
            ItemStack salted = raw.copy();
            salted.set(Things.SALTED.get(), net.minecraft.util.Unit.INSTANCE);
            sources.add(new Source(Source.Page.SALTING, List.of(List.of(raw), List.of(new ItemStack(Things.SALT.get()))), List.of(salted),
                    Component.translatable("jei.backcountry.salting")));
        }
    }

    private static ItemStack pot(Water water) {
        ItemStack pot = new ItemStack(Things.CAMP_POT.get());
        pot.set(Things.WATER.get(), water);
        return pot;
    }

    private static void campPot(List<Source> sources) {
        float litres = (float) Balance.get().water().potL();
        sources.add(new Source(Source.Page.CAMP_POT, List.of(List.of(pot(Water.drawn(Water.Source.LAKE, litres)), pot(Water.drawn(Water.Source.STREAM, litres))),
                List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()))), List.of(pot(Water.boiled(litres))), Component.translatable("jei.backcountry.camp_pot.boil")));
        sources.add(new Source(Source.Page.CAMP_POT, List.of(List.of(pot(Water.drawn(Water.Source.LAKE, litres))), List.of(new ItemStack(Things.SPRUCE_NEEDLES.get())),
                List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()))), List.of(pot(new Water(Water.Drink.SPRUCE_TEA, true, Water.Source.LAKE, litres))),
                Component.translatable("jei.backcountry.camp_pot.tea")));
        float sap = (float) Balance.get().forage().syrupL();
        sources.add(new Source(Source.Page.CAMP_POT, List.of(List.of(pot(new Water(Water.Drink.SEA_WATER, false, Water.Source.LAKE, litres))),
                List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()))), List.of(new ItemStack(Things.SALT.get(), (int) Math.floor(litres * Balance.get().drying().saltPerL()))),
                Component.translatable("jei.backcountry.camp_pot.salt")));
        sources.add(new Source(Source.Page.CAMP_POT, List.of(List.of(pot(new Water(Water.Drink.BIRCH_SAP, false, Water.Source.LAKE, sap))),
                List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()))), List.of(new ItemStack(Things.BIRCH_SYRUP.get())), Component.translatable("jei.backcountry.camp_pot.syrup")));
    }

    private static void birchTap(List<Source> sources) {
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        bottle.set(Things.WATER.get(), new Water(Water.Drink.BIRCH_SAP, false, Water.Source.LAKE, (float) Balance.get().water().bottleL()));
        sources.add(new Source(Source.Page.BIRCH_TAP, List.of(List.of(new ItemStack(Items.BIRCH_LOG)), List.of(new ItemStack(Things.BIRCH_TAP.get()))), List.of(bottle),
                Component.translatable("jei.backcountry.birch_tap.note")));
    }

    private static void woodpile(List<Source> sources) {
        for (Item wood : List.of(Things.SOFTWOOD.get(), Things.HARDWOOD.get())) {
            ItemStack green = new ItemStack(wood);
            ItemStack dry = new ItemStack(wood);
            dry.set(Things.SEASONING.get(), PERCENT);
            sources.add(new Source(Source.Page.WOODPILE, List.of(List.of(green)), List.of(dry), Component.translatable("jei.backcountry.woodpile.note")));
        }
    }

    private static void sharpening(List<Source> sources) {
        for (ItemStack tool : tagged(Things.EDGED)) {
            ItemStack worn = tool.copy();
            worn.setDamageValue(worn.getMaxDamage() / 2);
            ItemStack sharp = worn.copy();
            sharp.setDamageValue(Math.max(0, worn.getDamageValue() - (int) Math.ceil(worn.getMaxDamage() * Balance.get().kit().sharpenShare())));
            sources.add(new Source(Source.Page.SHARPENING, List.of(List.of(worn), List.of(new ItemStack(Things.SHARPENING_STONE.get()))), List.of(sharp),
                    Component.translatable("jei.backcountry.sharpening", Math.round(Balance.get().kit().sharpenShare() * PERCENT), Balance.get().kit().sharpeningStoneUses())));
        }
    }

    private static void maps(List<Source> sources) {
        sources.add(new Source(Source.Page.MAPS, List.of(List.of(new ItemStack(Things.BARK_MAP.get())), List.of(new ItemStack(Things.BARK_MAP.get()))),
                List.of(new ItemStack(Things.BARK_MAP.get(), 2)), Component.translatable("jei.backcountry.maps")));
    }

    private static void lighting(List<Source> sources) {
        List<ItemStack> fire = List.of(new ItemStack(Things.CAMPFIRE_ITEM.get()));
        List<ItemStack> kindling = List.of(new ItemStack(Items.STICK, Balance.get().fire().kindlingSticks()));
        sources.add(new Source(Source.Page.LIGHTING, List.of(fire, List.of(new ItemStack(Things.TINDER_BUNDLE.get()), new ItemStack(Things.PITCH_TINDER.get())), kindling,
                List.of(new ItemStack(Things.BOW_DRILL.get()), new ItemStack(Things.FERRO_ROD.get()))), fire, Component.translatable("jei.backcountry.lighting")));
        sources.add(new Source(Source.Page.LIGHTING, List.of(fire, List.of(new ItemStack(Things.TINDER_BUNDLE.get())), kindling,
                List.of(new ItemStack(Things.SMOULDERING_FUNGUS.get()))), fire, Component.translatable("jei.backcountry.lighting.fungus")));
    }

    private static void traps(List<Source> sources) {
        for (Kind kind : Kind.values()) {
            ItemStack trap = new ItemStack(BuiltInRegistries.ITEM.get(Backcountry.id(kind.name().toLowerCase(Locale.ROOT))));
            List<List<ItemStack>> inputs = kind.baited() ? List.of(List.of(trap), List.of(new ItemStack(kind.bait()))) : List.of(List.of(trap));
            List<ItemStack> outputs = new ArrayList<>();
            for (ResourceLocation animal : kind.animals()) {
                ItemStack carcass = new ItemStack(Things.CARCASS_ITEM.get());
                carcass.set(Things.ANIMAL_TYPE.get(), animal);
                outputs.add(carcass);
            }
            if (kind.fish()) {
                Cuts.fish().stream().limit(OUTPUT_LIMIT).forEach(outputs::add);
            }
            if (outputs.isEmpty()) {
                outputs.add(new ItemStack(Things.MOUSE_DROPPINGS.get()));
            }
            sources.add(new Source(Source.Page.TRAPS, inputs, outputs, Component.translatable("jei.backcountry.traps." + kind.name().toLowerCase(Locale.ROOT))));
        }
        Balance.FishWheel wheel = Balance.get().fishWheel();
        sources.add(new Source(Source.Page.TRAPS, List.of(List.of(new ItemStack(Things.FISH_WHEEL_ITEM.get()))),
                List.of(stack("riverfishing:sockeye_salmon"), stack("riverfishing:coho_salmon"), stack("riverfishing:pink_salmon"), stack("riverfishing:whitefish"),
                        stack("riverfishing:grayling")),
                Component.translatable("jei.backcountry.traps.fish_wheel", Math.round(wheel.runPerDay()), Math.round(wheel.othersPerDay()))));
    }

}
