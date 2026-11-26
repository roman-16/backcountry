package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.hunt.CarcassItem;
import io.github.roman16.backcountry.hunt.Track;
import io.github.roman16.backcountry.hunt.Tracks;
import io.github.roman16.backcountry.trap.TrapBlock;
import io.github.roman16.backcountry.trap.TrapEntity;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class TrapTests {
    private static final double LONG_WAIT_HOURS = 1024;
    private static final int PIKE_GRAMS = 3900;
    private static final double PIKE_KCAL = 1879.8;
    private static final double CLOSE = 0.5;

    private TrapTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static ItemStack fish(String species, int grams) {
        ItemStack fish = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riverfishing", species)));
        CompoundTag data = new CompoundTag();
        data.putString("Species", "riverfishing:" + species);
        data.putInt("WeightG", grams);
        fish.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return fish;
    }

    private static TrapEntity trap(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof TrapEntity trap)) {
            throw new IllegalStateException("the trap has no block entity");
        }
        return trap;
    }

    @GameTest(template = "empty")
    public static void aSnareCatchesSmallGame(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(pos, Things.SMALL_SNARE.get().defaultBlockState());
        BlockPos at = helper.absolutePos(pos);
        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withLevel(helper.getLevel()),
                "fillbiome %d %d %d %d %d %d minecraft:taiga".formatted(at.getX() - 4, at.getY() - 4, at.getZ() - 4, at.getX() + 4, at.getY() + 4, at.getZ() + 4));
        TrapEntity snare = trap(helper, pos);
        snare.watch(helper.getLevel(), LONG_WAIT_HOURS);
        check(helper, snare.caught().is(Things.CARCASS_ITEM.get()), "a snare set for " + LONG_WAIT_HOURS + " h in " + helper.getLevel().getBiome(helper.absolutePos(pos))
                .unwrapKey().orElseThrow().location() + " caught " + snare.caught());
        EntityType<?> caught = CarcassItem.animal(snare.caught());
        check(helper, caught == EntityType.RABBIT || BuiltInRegistries.ENTITY_TYPE.getKey(caught).getPath().matches("squirrel|grouse|ptarmigan"), "a snare caught a " + caught);
        check(helper, helper.getBlockState(pos).getValue(TrapBlock.SPRUNG), "the full snare did not show sprung");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aGillNetCatchesFish(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Things.GILL_NET.get().defaultBlockState().setValue(TrapBlock.WATERLOGGED, true));
        TrapEntity net = trap(helper, pos);
        net.watch(helper.getLevel(), LONG_WAIT_HOURS);
        check(helper, Fish.is(net.caught().getItem()) && Nutrition.of(net.caught()).map(Nutrition::kcal).orElse(0.0) > 0, "the gill net caught " + net.caught());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aMouseTrapGuardsTheCache(GameTestHelper helper) {
        BlockPos cache = new BlockPos(2, 1, 2);
        helper.setBlock(cache, Things.FOOD_CACHE.get().defaultBlockState());
        Level level = helper.getLevel();
        int reach = Balance.get().traps().miceBlocks();
        check(helper, !TrapBlock.guards(level, helper.absolutePos(cache), reach), "an unguarded cache was guarded");
        helper.setBlock(cache.east(2).below(), Blocks.STONE);
        helper.setBlock(cache.east(2), Things.MOUSE_TRAP.get().defaultBlockState());
        check(helper, TrapBlock.guards(level, helper.absolutePos(cache), reach), "a mouse trap beside the cache did not guard it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishGiveKcalByWeight(GameTestHelper helper) {
        ItemStack pike = fish("northern_pike", PIKE_GRAMS);
        Nutrition raw = Nutrition.of(pike).orElseThrow();
        check(helper, Math.abs(raw.kcal() - PIKE_KCAL) < CLOSE && raw.raw() && raw.vitaminC() > 0, "a 3.9 kg pike gave " + raw);
        check(helper, pike.has(DataComponents.FOOD), "raw fish cannot be eaten");
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack salmon = fish("sockeye_salmon", 2000);
        salmon.set(Things.SPAWNED_OUT.get(), Unit.INSTANCE);
        Metabolism.eat(player, salmon);
        check(helper, Metabolism.body(player).has(Condition.FOOD_POISONING), "a spawned-out salmon did not poison");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void riverFishingFollowsTheIslandsYear(GameTestHelper helper) throws ReflectiveOperationException {
        Class<?> calendar = Class.forName("com.riverfishing.engine.Calendar");
        Method dayOfYear = calendar.getMethod("dayOfYear", Level.class);
        int day = (int) dayOfYear.invoke(null, helper.getLevel());
        int yearDays = calendar.getField("YEAR_DAYS").getInt(null);
        check(helper, day >= 0 && day < yearDays, "River Fishing's day " + day + " is outside its year of " + yearDays);
        int month = io.github.roman16.backcountry.body.Calendar.date(io.github.roman16.backcountry.body.Calendar.day(helper.getLevel())).getMonthValue();
        Object season = calendar.getMethod("season", Level.class).invoke(null, helper.getLevel());
        String expected = month >= 3 && month <= 5 ? "SPRING" : month >= 6 && month <= 8 ? "SUMMER" : month >= 9 && month <= 11 ? "AUTUMN" : "WINTER";
        check(helper, ((Enum<?>) season).name().equals(expected), "River Fishing says " + season + " in month " + month);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSnareOnAHareRunKnowsIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.SNOW_BLOCK);
        helper.setBlock(pos, Things.SMALL_SNARE.get().defaultBlockState());
        TrapEntity snare = trap(helper, pos);
        check(helper, !snare.onRun(helper.getLevel(), Track.HARE), "a snare with no prints near stood on a run");
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.SNOW_BLOCK);
        Tracks.mark(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 2)), Track.HARE, Direction.NORTH, helper.getLevel().getDayTime(), false);
        check(helper, snare.onRun(helper.getLevel(), Track.HARE), "a snare beside fresh hare prints was not on a run");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void twoNuggetsTwistIntoSnareWire(GameTestHelper helper) {
        ItemStack nugget = new ItemStack(Items.IRON_NUGGET);
        CraftingInput grid = CraftingInput.of(2, 1, List.of(nugget, nugget.copy()));
        ItemStack made = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(recipe -> recipe.value().assemble(grid, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        check(helper, made.is(Things.SNARE_WIRE.get()), "two iron nuggets made " + made);
        helper.succeed();
    }
}
