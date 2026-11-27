package io.github.roman16.backcountry;

import io.github.roman16.backcountry.fire.Lights;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    private static final int BIOME_REACH = 8;
    private static final long DROP_OFF_SEED = 1;
    private static final int GRID = 3;

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
    public static void noTwoRecipesShareAGrid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<RecipeHolder<CraftingRecipe>> crafting = level.getServer().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
        List<String> shared = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> recipe : crafting) {
            grid(recipe.value()).ifPresent(grid -> {
                List<String> others = crafting.stream()
                        .filter(other -> other != recipe && !other.value().getIngredients().isEmpty() && other.value().matches(grid, level))
                        .map(other -> other.id().toString()).sorted().toList();
                if (!others.isEmpty()) {
                    shared.add(recipe.id() + " with " + String.join(", ", others));
                }
            });
        }
        check(helper, shared.isEmpty(), "recipes one grid makes, so the game picks one of them: " + String.join("; ", shared.stream().sorted().toList()));
        helper.succeed();
    }

    private static Optional<CraftingInput> grid(CraftingRecipe recipe) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty() || ingredients.stream().anyMatch(ingredient -> !ingredient.isEmpty() && ingredient.getItems().length == 0)) {
            return Optional.empty();
        }
        List<ItemStack> items = new ArrayList<>(ingredients.stream().map(ingredient -> ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0].copy()).toList());
        if (recipe instanceof ShapedRecipe shaped) {
            return Optional.of(CraftingInput.of(shaped.getWidth(), shaped.getHeight(), items));
        }
        int width = Math.min(GRID, items.size());
        int height = (items.size() + GRID - 1) / GRID;
        while (items.size() < width * height) {
            items.add(ItemStack.EMPTY);
        }
        return Optional.of(CraftingInput.of(width, height, items));
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
    public static void theLeadIsMadeFromRope(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
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
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.SPRUCE_SAPLING.defaultBlockState());
        BlockPos sapling = helper.absolutePos(new BlockPos(2, 1, 2));
        BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), sapling, BackcountryTests.survivor(helper));
        check(helper, helper.getLevel().getBlockState(sapling).is(Blocks.SPRUCE_SAPLING) && helper.getLevel().getBlockState(sapling).getValue(SaplingBlock.STAGE) == 0,
                "bone meal grew the sapling");
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

    @GameTest(template = "empty")
    public static void theSeaIsSalt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> sea : List.of(Biomes.BEACH, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE)) {
            check(helper, biomes.getHolderOrThrow(sea).is(Things.SALT_WATER), sea.location() + " is not salt");
        }
        for (ResourceKey<Biome> fresh : List.of(Biomes.FROZEN_RIVER, Biomes.RIVER, Biomes.TAIGA)) {
            check(helper, !biomes.getHolderOrThrow(fresh).is(Things.SALT_WATER), fresh.location() + " is salt");
        }
        BlockPos water = new BlockPos(1, 1, 1);
        helper.setBlock(water, Blocks.WATER.defaultBlockState());
        ServerPlayer survivor = BackcountryTests.survivor(helper);
        Vec3 above = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        survivor.moveTo(above.x, above.y, above.z, 0, 90);
        survivor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Things.WATER_BOTTLE.get()));
        biome(helper, water, Biomes.COLD_OCEAN);
        survivor.getMainHandItem().use(level, survivor, InteractionHand.MAIN_HAND);
        check(helper, WaterVesselItem.water(survivor.getMainHandItem()).empty(), "the bottle filled from the sea");
        biome(helper, water, Biomes.RIVER);
        survivor.getMainHandItem().use(level, survivor, InteractionHand.MAIN_HAND);
        check(helper, !WaterVesselItem.water(survivor.getMainHandItem()).empty(), "the bottle did not fill from a river");
        helper.succeed();
    }

    private static void biome(GameTestHelper helper, BlockPos at, ResourceKey<Biome> biome) {
        BlockPos from = helper.absolutePos(at).offset(-BIOME_REACH, 0, -BIOME_REACH).atY(helper.getLevel().getMinBuildHeight());
        BlockPos to = helper.absolutePos(at).offset(BIOME_REACH, BIOME_REACH, BIOME_REACH);
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s",
                from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), biome.location()));
    }

    @GameTest(template = "empty")
    public static void theDropOffIsOnTheIslandsCoast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryAccess access = level.registryAccess();
        Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(
                access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)), settings);
        RandomState state = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), DROP_OFF_SEED);
        Optional<DropOff.Found> found = DropOff.find(level, generator, state);
        check(helper, found.isPresent(), "seed " + DROP_OFF_SEED + " has no drop-off");
        BlockPos shore = found.get().shore();
        Direction seaward = found.get().seaward();
        int seaLevel = generator.getSeaLevel();
        check(helper, generator.getBaseHeight(shore.getX(), shore.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, state) == shore.getY()
                && generator.getBaseHeight(shore.getX(), shore.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, state) == shore.getY(), "the drop-off " + shore.toShortString() + " is not on dry ground");
        check(helper, shore.getY() <= seaLevel + Balance.get().dropOff().shoreRiseBlocks(), "the drop-off stands " + (shore.getY() - seaLevel) + " blocks above the sea");
        BlockPos ahead = shore.relative(seaward);
        check(helper, generator.getBaseHeight(ahead.getX(), ahead.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, state) == seaLevel
                && generator.getBaseHeight(ahead.getX(), ahead.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, state) < seaLevel, "the drop-off " + shore.toShortString() + " does not face the sea");
        DensityFunction continents = state.router().continents();
        boolean ocean = false;
        for (int step = 1; step <= Balance.get().dropOff().stepBlocks(); step++) {
            BlockPos out = shore.relative(seaward, step);
            ocean |= continents.compute(new DensityFunction.SinglePointContext(out.getX(), 0, out.getZ())) <= IslandShape.LAND;
        }
        check(helper, ocean, "the water the drop-off " + shore.toShortString() + " faces is not the ocean");
        check(helper, DropOff.find(level, generator, RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), DROP_OFF_SEED)).equals(found),
                "seed " + DROP_OFF_SEED + " drew a second drop-off");
        helper.succeed();
    }
}
