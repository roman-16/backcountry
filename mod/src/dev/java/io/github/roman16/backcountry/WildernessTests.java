package io.github.roman16.backcountry;

import io.github.roman16.backcountry.fire.Lights;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class WildernessTests {
    private static final double HALF_AN_HOUR = 0.5;
    private static final double TWO_HOURS = 2;
    private static final int FRAME_WIDTH = 4;
    private static final int FRAME_HEIGHT = 5;
    private static final ResourceLocation KEPT_FISH = ResourceLocation.fromNamespaceAndPath("riverfishing", "northern_pike");
    private static final ResourceLocation CUT_FISH = ResourceLocation.fromNamespaceAndPath("riverfishing", "bluegill");

    private WildernessTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static List<RecipeHolder<?>> recipes(GameTestHelper helper) {
        return List.copyOf(helper.getLevel().getServer().getRecipeManager().getRecipes());
    }

    private static String name(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @GameTest(template = "empty")
    public static void everyCutNamesSomething(GameTestHelper helper) {
        List<String> unknown = Cuts.unknown();
        check(helper, unknown.isEmpty(), "cuts.toml names what the pack does not hold: " + String.join(", ", unknown));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noRecipeMakesOrNeedsACutItem(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> wrong = recipes(helper).stream()
                .filter(recipe -> Cuts.cut(recipe.value().getResultItem(server.registryAccess()))
                        || recipe.value().getIngredients().stream().anyMatch(ingredient -> ingredient.getItems().length > 0
                                && Arrays.stream(ingredient.getItems()).allMatch(Cuts::cut)))
                .map(recipe -> recipe.id().toString()).sorted().toList();
        check(helper, wrong.isEmpty(), "recipes that make or need a cut item: " + String.join(", ", wrong));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noItemLosesItsRecipesUncut(GameTestHelper helper) {
        List<String> stranded = Cuts.stranded().stream().map(WildernessTests::name).sorted().toList();
        check(helper, stranded.isEmpty(), "items whose every recipe needs a cut item; cut them in cuts.toml or give them a recipe: " + String.join(", ", stranded));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theFurnaceCooksNoFood(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> cooking = recipes(helper).stream()
                .filter(recipe -> recipe.value().getType() == RecipeType.SMELTING && recipe.value().getResultItem(server.registryAccess()).has(DataComponents.FOOD))
                .map(recipe -> recipe.id().toString()).toList();
        check(helper, cooking.isEmpty(), "the furnace still cooks: " + String.join(", ", cooking));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCompassClockAndLeadAreMadeOnTheIsland(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        check(helper, uses(server, "compass", Items.GLASS_PANE) && uses(server, "compass", Items.IRON_INGOT), "the compass is not iron around a glass pane");
        check(helper, uses(server, "clock", Items.GLASS_PANE) && uses(server, "clock", Items.GOLD_INGOT), "the clock is not gold around a glass pane");
        check(helper, uses(server, "lead", Things.ROPE.get()), "the lead is not made from rope");
        helper.succeed();
    }

    private static boolean uses(MinecraftServer server, String recipe, Item item) {
        return server.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(recipe))
                .map(holder -> holder.value().getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(item)))).orElse(false);
    }

    @GameTest(template = "empty")
    public static void aPortalNeverLights(GameTestHelper helper) {
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                if (x == 0 || x == FRAME_WIDTH - 1 || y == 0 || y == FRAME_HEIGHT - 1) {
                    helper.setBlock(new BlockPos(x + 1, y + 1, 2), Blocks.OBSIDIAN.defaultBlockState());
                }
            }
        }
        BlockPos inside = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(inside, BaseFireBlock.getState(helper.getLevel(), inside));
        helper.runAfterDelay(2, () -> {
            check(helper, !helper.getLevel().getBlockState(inside).is(Blocks.NETHER_PORTAL), "the obsidian frame opened a portal");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void animalsNeitherBreedNorTame(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Rabbit doe = helper.spawn(EntityType.RABBIT, new BlockPos(2, 1, 2));
        Rabbit buck = helper.spawn(EntityType.RABBIT, new BlockPos(3, 1, 2));
        doe.spawnChildFromBreeding(level, buck);
        AABB room = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
        check(helper, level.getEntitiesOfClass(Rabbit.class, room).size() == 2, "two hares bred a third");
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(5, 1, 5));
        wolf.tame(BackcountryTests.survivor(helper));
        check(helper, !wolf.isTame(), "a wolf was tamed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boneMealGrowsNothing(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.WHEAT.defaultBlockState());
        BlockPos wheat = helper.absolutePos(new BlockPos(2, 1, 2));
        BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), wheat, BackcountryTests.survivor(helper));
        check(helper, helper.getLevel().getBlockState(wheat).getValue(CropBlock.AGE) == 0, "bone meal grew the wheat");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nothingCutJoinsTheWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spot = helper.absolutePos(new BlockPos(2, 1, 2));
        check(helper, !level.addFreshEntity(new ExperienceOrb(level, spot.getX(), spot.getY(), spot.getZ(), 4)), "an XP orb dropped");
        EntityType.BEE.spawn(level, spot, MobSpawnType.COMMAND);
        check(helper, level.getEntitiesOfClass(Bee.class, new AABB(spot).inflate(8)).isEmpty(), "a bee came out");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void logsNeedAnAxe(GameTestHelper helper) {
        ServerPlayer survivor = BackcountryTests.survivor(helper);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.SPRUCE_LOG.defaultBlockState());
        BlockPos log = helper.absolutePos(new BlockPos(2, 1, 2));
        ServerLevel level = helper.getLevel();
        check(helper, !survivor.hasCorrectToolForDrops(level.getBlockState(log), level, log), "bare hands took a log");
        survivor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_AXE));
        check(helper, survivor.hasCorrectToolForDrops(level.getBlockState(log), level, log), "a stone axe could not fell a log");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aTorchBurnsAwayAndALanternGoesDark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.TORCH.defaultBlockState());
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.LANTERN.defaultBlockState());
        BlockPos torch = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos lantern = helper.absolutePos(new BlockPos(4, 1, 4));
        Lights.light(level.getServer(), torch, Lights.Kind.TORCH.fullHours());
        Lights.light(level.getServer(), lantern, Lights.Kind.LANTERN.fullHours());
        Lights.burn(level, torch, Lights.Kind.TORCH.fullHours() - HALF_AN_HOUR);
        check(helper, level.getBlockState(torch).is(Blocks.TORCH), "the torch burnt out early");
        Lights.burn(level, torch, HALF_AN_HOUR + HALF_AN_HOUR);
        Lights.burn(level, lantern, Lights.Kind.LANTERN.fullHours() + HALF_AN_HOUR);
        check(helper, level.getBlockState(torch).isAir(), "the torch burnt on past its hours");
        check(helper, level.getBlockState(lantern).is(Things.EMPTY_LANTERN.get()), "the lantern burnt on past its hours");
        Lights.refill(level, lantern);
        check(helper, level.getBlockState(lantern).is(Blocks.LANTERN) && !level.getBlockState(lantern).getValue(LanternBlock.HANGING),
                "rendered fat did not light the lantern again");
        check(helper, Lights.left(level.getServer(), lantern).orElse(0.0) == Lights.Kind.LANTERN.fullHours(), "the refilled lantern is not full");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aTorchInHandBurnsDown(GameTestHelper helper) {
        ServerPlayer survivor = BackcountryTests.survivor(helper);
        ItemStack torches = new ItemStack(Items.TORCH, 2);
        survivor.setItemInHand(InteractionHand.MAIN_HAND, torches);
        Lights.burnHeld(survivor, Lights.Kind.TORCH.fullHours() - TWO_HOURS);
        check(helper, torches.getCount() == 2 && Math.abs(Lights.left(torches) - TWO_HOURS) < HALF_AN_HOUR, "the held torch has " + Lights.left(torches) + " h left");
        Lights.burnHeld(survivor, TWO_HOURS + HALF_AN_HOUR);
        check(helper, torches.getCount() == 1 && Lights.left(torches) == Lights.Kind.TORCH.fullHours(), "the burnt torch did not give way to a fresh one");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theDifficultyStaysNormal(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "difficulty peaceful");
        check(helper, server.getWorldData().getDifficulty() == Difficulty.NORMAL && server.getWorldData().isDifficultyLocked(), "the difficulty is "
                + server.getWorldData().getDifficulty());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyAlaskasFishLiveOnTheIsland(GameTestHelper helper) throws ReflectiveOperationException {
        check(helper, Cuts.livesOnTheIsland(KEPT_FISH) && !Cuts.livesOnTheIsland(CUT_FISH), "the island's fish are not Alaska's");
        Class<?> engine = Class.forName("com.riverfishing.engine.BiteEngine");
        Class<?> profiles = Class.forName("com.riverfishing.fish.FishProfileManager");
        Class<?> context = Class.forName("com.riverfishing.engine.BiteContext");
        Object profile = profiles.getMethod("byId", ResourceLocation.class).invoke(profiles.getMethod("get").invoke(null), CUT_FISH);
        Method score = engine.getMethod("environmentScore", profile.getClass(), context);
        check(helper, (double) score.invoke(null, profile, context.getConstructor().newInstance()) == 0, "a bluegill finds water to live in");
        check(helper, Cuts.cut(BuiltInRegistries.ITEM.get(CUT_FISH)), "a bluegill still shows in JEI");
        helper.succeed();
    }
}
