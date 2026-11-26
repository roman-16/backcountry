package io.github.roman16.backcountry.guide;

import static io.github.roman16.backcountry.guide.Step.need;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.guide.Check.Did;
import io.github.roman16.backcountry.guide.Check.Held;
import io.github.roman16.backcountry.guide.Check.Made;
import io.github.roman16.backcountry.guide.Check.Placed;
import io.github.roman16.backcountry.guide.Check.Reach;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public enum Task {
    STONE_AXE(Chapter.FIRST_DAY, Common.STONE_AXE,
            Step.of("rock", new Held(Common.ROCKS, 1)),
            Step.of("stick", new Held(Common.STICK, 1)),
            Step.of("cordage", new Held(Thing.of(Things.CORDAGE), 1), need(Thing.of(Things.PLANT_FIBRE), 4)),
            Step.of("axe", new Held(Common.STONE_AXE, 1), need(Common.ROCKS, 1), need(Common.STICK, 1), need(Thing.of(Things.CORDAGE), 1))),
    LIGHT_FIRE(Chapter.FIRST_DAY, Thing.of(Things.CAMPFIRE_ITEM),
            Step.of("log", new Held(Common.SOFTWOOD_LOGS, 1)),
            Step.of("softwood", new Held(Thing.of(Things.SOFTWOOD), 1)),
            Step.of("bow_drill", new Held(Thing.of(Things.BOW_DRILL), 1), need(Common.STICK, 2), need(Thing.of(Things.CORDAGE), 1), need(Thing.of(Things.SOFTWOOD), 1)),
            Step.of("tinder", new Held(Thing.of(Things.TINDER_BUNDLE), 1), need(Thing.of(Things.BIRCH_BARK), 2), need(Thing.of(Things.PLANT_FIBRE), 1)),
            Step.of("campfire", new Placed(Things.CAMPFIRE), need(Common.STICK, 3), need(Thing.of(ItemTags.LOGS), 3)),
            Step.of("lit", new Did(Deed.LIT_FIRE), need(Thing.of(Things.BOW_DRILL), 1), need(Thing.of(Things.TINDER_BUNDLE), 1), need(Common.STICK, 2)),
            Step.of("fed", new Did(Deed.FED_FIRE), need(Thing.of(Things.SOFTWOOD), 1))),
    BOIL_WATER(Chapter.FIRST_DAY, Thing.of(Things.CAMP_POT), Pace.IN_ORDER,
            Step.of("filled", new Did(Deed.FILLED_POT)),
            Step.of("hung", new Did(Deed.HUNG_POT)),
            Step.of("drunk", new Did(Deed.DRANK_BOILED))),
    FILL_BOTTLE(Chapter.FIRST_DAY, Thing.of(Things.WATER_BOTTLE),
            Step.of("drunk", new Did(Deed.DRANK)),
            Step.of("filled", new Did(Deed.BOTTLE_FROM_POT))),
    SLEEP_ROOFED(Chapter.FIRST_DAY, Thing.of("sleeping_bags:brown_sleeping_bag"),
            Step.of("roof", new Check.Sheltered()),
            Step.of("slept", new Did(Deed.SLEPT_ROOFED))),
    LAY_WOODPILE(Chapter.CAMP, Thing.of(Things.SOFTWOOD),
            Step.of("split", new Held(Common.FIREWOOD, 8)),
            Step.of("laid", new Did(Deed.LAID_WOODPILE))),
    MAKE_KNIFE(Chapter.CAMP, Thing.of(Things.KNIFE),
            Step.of("flint", new Held(Thing.of("minecraft:flint"), 1)),
            Step.of("knife", new Held(Thing.of(Things.KNIFE), 1), need(Thing.of("minecraft:flint"), 1), need(Common.STICK, 1), need(Thing.of(Things.CORDAGE), 1))),
    BUILD_CACHE(Chapter.CAMP, Thing.of(Things.FOOD_CACHE_ITEM),
            Step.of("planks", new Held(Thing.of(ItemTags.PLANKS), 7)),
            Step.of("cache", new Did(Deed.CACHE_OUT_OF_CAMP), need(Thing.of(ItemTags.PLANKS), 7), need(Thing.of(Things.CORDAGE), 1)),
            Step.of("food", new Check.FoodInACache())),
    BUILD_FURNACE(Chapter.CAMP, Thing.of("minecraft:furnace"),
            Step.of("cobblestone", new Held(Thing.of("minecraft:cobblestone"), 8)),
            Step.of("furnace", new Placed(() -> Blocks.FURNACE), need(Thing.of("minecraft:cobblestone"), 8)),
            Step.of("charcoal", new Held(Thing.of("minecraft:charcoal"), 1)),
            Step.of("torch", new Held(Thing.of("minecraft:torch"), 1), need(Thing.of("minecraft:charcoal"), 1), need(Common.STICK, 1))),
    SMELT_IRON(Chapter.CAMP, Thing.of("minecraft:iron_ingot"),
            Step.of("raw", new Held(Thing.of("minecraft:raw_iron"), 1)),
            Step.of("ingot", new Held(Thing.of("minecraft:iron_ingot"), 1))),
    LAND_FISH(Chapter.FOOD, Thing.of("riverfishing:stick_rod"),
            Step.of("rod", new Held(Thing.of("riverfishing:stick_rod"), 1), need(Common.STICK, 3)),
            Step.of("bait", new Held(Thing.of("riverfishing:worm"), 1)),
            Step.of("fish", new Did(Deed.LANDED_FISH))),
    TRAP_CATCH(Chapter.FOOD, Thing.of(Things.SMALL_SNARE_ITEM),
            Step.of("wire", new Held(Thing.of(Things.SNARE_WIRE), 1), need(Thing.of("minecraft:iron_nugget"), 2)),
            Step.of("stakes", new Held(Thing.of(Things.TRAP_STAKES), 1), need(Common.STICK, 2)),
            Step.of("set", new Placed(Things.SMALL_SNARE), need(Thing.of(Things.SNARE_WIRE), 1), need(Thing.of(Things.TRAP_STAKES), 1)),
            Step.of("catch", new Did(Deed.TRAP_CATCH))),
    COOK_MEAT(Chapter.FOOD, Thing.of("minecraft:cooked_rabbit"),
            Step.of("eaten", new Did(Deed.ATE_COOKED))),
    COOK_STEW(Chapter.FOOD, Thing.of(Things.COOKING_POT),
            Step.of("pot", new Placed(Things.COOKING_POT_BLOCK), need(Thing.of("minecraft:iron_ingot"), 5)),
            Step.of("served", new Did(Deed.SERVED_STEW), need(Thing.of("minecraft:bowl"), 1))),
    EAT_BERRIES(Chapter.FOOD, Thing.of(Things.SALMONBERRIES), new Season(6, 3),
            Step.of("eaten", new Did(Deed.ATE_BERRIES))),
    SPRUCE_TEA(Chapter.FOOD, Thing.of(Things.SPRUCE_NEEDLES),
            Step.of("needles", new Held(Thing.of(Things.SPRUCE_NEEDLES), 1)),
            Step.of("drunk", new Did(Deed.DRANK_TEA))),
    SMOKE_MEAT(Chapter.FOOD, Thing.of(Things.SMOKER_ITEM),
            Step.of("smoker", new Placed(Things.SMOKER), need(Thing.of(Things.CORDAGE), 2), need(Thing.of(ItemTags.LOGS), 3), need(Thing.of(Things.CAMPFIRE_ITEM), 1),
                    need(Thing.of("minecraft:cobblestone"), 3)),
            Step.of("smoked", new Held(Thing.with(Things.SMOKED, () -> Items.COOKED_BEEF), 1))),
    MAKE_BOW(Chapter.HUNTING, Thing.of(Things.LIGHT_HUNTING_BOW),
            Step.of("stave", new Held(Thing.of(Things.BOW_STAVE), 1), need(Common.HARDWOOD_LOGS, 1), need(Thing.of(Things.KNIFE), 1)),
            Step.of("seasoned", new Held(Thing.of(Things.SEASONED_BOW_STAVE), 1)),
            Step.of("string", new Held(Thing.of(Things.BOWSTRING), 1), need(Thing.of(Things.CORDAGE), 2)),
            Step.of("bow", new Held(Thing.any(Thing.of(Things.LIGHT_HUNTING_BOW), Thing.of(Things.HEAVY_HUNTING_BOW)), 1),
                    need(Thing.of(Things.SEASONED_BOW_STAVE), 1), need(Thing.of(Things.BOWSTRING), 1), need(Thing.of(Things.KNIFE), 1))),
    MAKE_ARROWS(Chapter.HUNTING, Thing.of(Things.BROADHEAD_ARROW),
            Step.of("feather", new Held(Thing.of("minecraft:feather"), 1)),
            Step.of("arrows", new Held(Common.ARROWS, 4), need(Common.STICK, 4), need(Thing.of("minecraft:feather"), 4), need(Thing.of("minecraft:flint"), 4))),
    TRACK_ANIMAL(Chapter.HUNTING, Thing.of(Things.PLANT_FIBRE),
            Step.of("sign", new Did(Deed.READ_SIGN)),
            Step.of("wind", new Did(Deed.CHECKED_WIND), need(Thing.of(Things.PLANT_FIBRE), 1)),
            Step.of("stalked", new Did(Deed.STALKED))),
    TAKE_ANIMAL(Chapter.HUNTING, Thing.of(Things.CARCASS_ITEM),
            Step.of("killed", new Did(Deed.KILLED_ANIMAL)),
            Step.of("dressed", new Did(Deed.DRESSED), need(Thing.of(Things.KNIFE), 1))),
    CURE_HIDE(Chapter.HUNTING, Thing.of(Things.CURED_HIDE),
            Step.of("scraped", new Held(Thing.of(Things.SCRAPED_HIDE), 1), need(Thing.of(Things.PELT), 1), need(Thing.of(Things.KNIFE), 1)),
            Step.of("pot", new Held(Thing.of(Things.COOKING_POT), 1), need(Thing.of("minecraft:iron_ingot"), 5)),
            Step.of("fat", new Held(Thing.of(Things.RENDERED_FAT), 1), need(Thing.of(Things.ANIMAL_FAT), 2)),
            Step.of("cured", new Held(Thing.of(Things.CURED_HIDE), 1), need(Thing.of(Things.SCRAPED_HIDE), 1), need(Thing.of(Things.RENDERED_FAT), 1))),
    HIDE_PACK(Chapter.HUNTING, Thing.of(Things.HIDE_PACK),
            Step.of("pack", new Held(Thing.of(Things.HIDE_PACK), 1)),
            Step.of("worn", new Check.WearsHidePack())),
    FOOD_STORES(Chapter.BEFORE_SNOW, Thing.of(Things.FOOD_CACHE_ITEM), true,
            Step.of("stored", new Check.FoodStores())),
    DRY_FIREWOOD(Chapter.BEFORE_SNOW, Thing.of(Things.HARDWOOD), true,
            Step.of("dried", new Check.DryFirewood())),
    HIDE_CLOTHING(Chapter.BEFORE_SNOW, Thing.of(Things.HIDE_COAT), true,
            Step.of("hat", new Made(Things.HIDE_HAT)),
            Step.of("coat", new Made(Things.HIDE_COAT)),
            Step.of("snowshoes", new Made(Things.SNOWSHOES))),
    ICE_FISHING(Chapter.WINTER, Thing.of(Things.TIP_UP_ITEM), new Season(11, 4),
            Step.of("auger", new Held(Thing.of("riverfishing:ice_auger"), 1), need(Thing.of("minecraft:iron_ingot"), 3), need(Common.STICK, 1)),
            Step.of("tip_up", new Placed(Things.TIP_UP), need(Thing.of(Things.TIP_UP_ITEM), 1)),
            Step.of("catch", new Did(Deed.TIP_UP_CATCH))),
    REACH_APRIL(Chapter.WINTER, Thing.of("minecraft:snowball"), true,
            Step.of("reached", new Reach(4))),
    REACH_MAY(Chapter.YEAR, Thing.of("minecraft:sunflower"), true,
            Step.of("reached", new Check.YearsEnd()));

    public enum Pace {
        ANY_ORDER,
        IN_ORDER
    }

    public record Season(int from, int to) {
        public boolean holds(int month) {
            return from <= to ? month >= from && month <= to : month >= from || month <= to;
        }
    }

    private static final class Common {
        static final Thing ARROWS = Thing.any(Thing.of(Things.BROADHEAD_ARROW), Thing.of(Things.BONE_ARROW), Thing.of(Things.BLUNT_ARROW));
        static final Thing FIREWOOD = Thing.any(Thing.of(Things.SOFTWOOD), Thing.of(Things.HARDWOOD));
        static final Thing HARDWOOD_LOGS = Thing.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(Backcountry.MODID, "hardwood_logs")));
        static final Thing ROCKS = Thing.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(Backcountry.MODID, "rocks")));
        static final Thing SOFTWOOD_LOGS = Thing.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(Backcountry.MODID, "softwood_logs")));
        static final Thing STICK = Thing.of("minecraft:stick");
        static final Thing STONE_AXE = Thing.of("minecraft:stone_axe");

        private Common() {}
    }

    private final Chapter chapter;
    private final Thing icon;
    private final boolean team;
    private final Optional<Season> season;
    private final Pace pace;
    private final List<Step> steps;

    Task(Chapter chapter, Thing icon, Step... steps) {
        this(chapter, icon, false, Optional.empty(), Pace.ANY_ORDER, steps);
    }

    Task(Chapter chapter, Thing icon, Pace pace, Step... steps) {
        this(chapter, icon, false, Optional.empty(), pace, steps);
    }

    Task(Chapter chapter, Thing icon, Season season, Step... steps) {
        this(chapter, icon, false, Optional.of(season), Pace.ANY_ORDER, steps);
    }

    Task(Chapter chapter, Thing icon, boolean team, Step... steps) {
        this(chapter, icon, team, Optional.empty(), Pace.ANY_ORDER, steps);
    }

    Task(Chapter chapter, Thing icon, boolean team, Optional<Season> season, Pace pace, Step... steps) {
        this.chapter = chapter;
        this.icon = icon;
        this.team = team;
        this.season = season;
        this.pace = pace;
        this.steps = List.of(steps);
    }

    public Pace pace() {
        return pace;
    }

    public Chapter chapter() {
        return chapter;
    }

    public boolean team() {
        return team;
    }

    public Optional<Season> season() {
        return season;
    }

    public boolean inSeason(int month) {
        return season.map(range -> range.holds(month)).orElse(true);
    }

    public List<Step> steps() {
        return steps;
    }

    public int full() {
        return (1 << steps.size()) - 1;
    }

    public ItemStack icon() {
        return icon.stack();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        Balance.Tasks rules = Balance.get().tasks();
        return switch (this) {
            case FOOD_STORES -> Component.translatable("task.backcountry." + id(), (int) rules.foodDays());
            case DRY_FIREWOOD -> Component.translatable("task.backcountry." + id(), (int) rules.dryFirewood());
            default -> Component.translatable("task.backcountry." + id());
        };
    }

    public Component why() {
        return Component.translatable("task.backcountry." + id() + ".why");
    }

    public Component step(Step step) {
        return Component.translatable("task.backcountry." + id() + "." + step.id());
    }

    public Component how(Step step) {
        return Component.translatable("task.backcountry." + id() + "." + step.id() + ".how");
    }
}
