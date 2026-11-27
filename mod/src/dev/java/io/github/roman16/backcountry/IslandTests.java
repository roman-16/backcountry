package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.fire.SmoulderingFungusItem;
import io.github.roman16.backcountry.gear.MapCopyRecipe;
import io.github.roman16.backcountry.gear.MapSheets;
import io.github.roman16.backcountry.guide.Chapter;
import io.github.roman16.backcountry.guide.FieldNotes;
import io.github.roman16.backcountry.guide.Year;
import io.github.roman16.backcountry.hunt.Migration;
import io.github.roman16.backcountry.trail.BlazeBlock;
import io.github.roman16.backcountry.trap.FishWheelBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class IslandTests {
    private static final int JANUARY = 1;
    private static final int JUNE = 6;
    private static final int DECEMBER = 12;
    private static final int HOMESTEAD_TASKS = 9;
    private static final List<String> NEW_ANIMALS = List.of("animalgarden_porcupine:porcupine", "cnc:coyote", "cnc:marmot", "fowlplay:crow", "fowlplay:robin",
            "fowlplay:sparrow", "minecraft:fox", "minecraft:goat");

    private IslandTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), face, helper.absolutePos(pos), false);
    }

    @GameTest(template = "empty")
    public static void fieldNotesKeepEachFindOnce(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        String moose = FieldNotes.animal(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("alexsmobs", "moose")));
        FieldNotes notes = FieldNotes.get(player.server);
        int before = notes.of(player).size();
        FieldNotes.note(player, moose);
        FieldNotes.note(player, moose);
        check(helper, notes.of(player).size() == before + 1, "noting the moose twice made " + (notes.of(player).size() - before) + " notes");
        FieldNotes.note(player, "animal:minecraft:wandering_trader");
        check(helper, notes.of(player).size() == before + 1, "a creature off the island went into the notes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyNewAnimalHasAFieldNote(GameTestHelper helper) {
        List<String> catalogue = FieldNotes.catalogue();
        for (String animal : NEW_ANIMALS) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(animal));
            check(helper, catalogue.contains(FieldNotes.animal(type)), animal + " has no place in the field notes");
        }
        check(helper, catalogue.contains(FieldNotes.animal(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("alexsmobs", "cachalot_whale")))),
                "the whale has no place in the field notes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void surveyingNeedsCharcoal(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        player.getInventory().clearContent();
        check(helper, MapSheets.cannotSurvey(player).isPresent(), "a survey began without charcoal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aCopiedMapSharesItsDrawing(GameTestHelper helper) {
        ItemStack drawn = new ItemStack(Things.BARK_MAP.get());
        drawn.set(Things.MAP_SHEET.get(), 7);
        ItemStack blank = new ItemStack(Things.BARK_MAP.get());
        MapCopyRecipe recipe = new MapCopyRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(2, 1, List.of(drawn, blank));
        check(helper, recipe.matches(input, helper.getLevel()), "a drawn and a blank map made no copy");
        ItemStack copies = recipe.assemble(input, helper.getLevel().registryAccess());
        check(helper, copies.getCount() == 2 && copies.get(Things.MAP_SHEET.get()) == 7, "the copy did not share the drawing");
        CraftingInput blanks = CraftingInput.of(2, 1, List.of(blank, blank.copy()));
        check(helper, !recipe.matches(blanks, helper.getLevel()), "two blank maps copied a drawing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSpruceBlazeWeepsPitch(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        helper.setBlock(new BlockPos(3, 1, 4), Blocks.SPRUCE_LOG.defaultBlockState());
        BlockPos blaze = new BlockPos(3, 1, 3);
        helper.setBlock(blaze, Things.BLAZE.get().defaultBlockState().setValue(BlazeBlock.FACING, Direction.NORTH).setValue(BlazeBlock.PITCH, true));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Things.KNIFE.get()));
        BlockState state = helper.getBlockState(blaze);
        state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, blaze, Direction.NORTH));
        check(helper, player.getInventory().contains(new ItemStack(Things.SPRUCE_PITCH.get())), "the knife scraped no pitch");
        check(helper, !helper.getBlockState(blaze).getValue(BlazeBlock.PITCH), "the blaze kept its pitch");
        helper.setBlock(new BlockPos(3, 1, 4), Blocks.AIR.defaultBlockState());
        check(helper, helper.getBlockState(blaze).isAir(), "a blaze outlived its tree");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rocksPileIntoACairnThreeHigh(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE.defaultBlockState());
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("projectvibrantjourneys", "rocks")).defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("projectvibrantjourneys", "rocks")), 8));
        int most = Balance.get().trail().cairnHeight();
        for (int stone = 0; stone <= most; stone++) {
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(ground), hit(helper, ground, Direction.UP)));
        }
        for (int y = 1; y <= most; y++) {
            check(helper, helper.getBlockState(new BlockPos(3, y, 3)).is(Things.CAIRN.get()), "the cairn stopped below " + y);
        }
        check(helper, !helper.getBlockState(new BlockPos(3, most + 1, 3)).is(Things.CAIRN.get()), "the cairn grew past " + most);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aFishWheelNeedsAStream(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.WATER.defaultBlockState());
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.WATER.defaultBlockState());
        check(helper, !FishWheelBlock.inStream(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3))), "still water turned a fish wheel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSmoulderingFungusLightsAColdFire(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        BlockPos fire = new BlockPos(3, 1, 3);
        helper.setBlock(fire, Things.CAMPFIRE.get().defaultBlockState());
        player.getInventory().setItem(1, new ItemStack(Things.TINDER_BUNDLE.get()));
        player.getInventory().setItem(2, new ItemStack(Items.STICK, Balance.get().fire().kindlingSticks()));
        player.setItemInHand(InteractionHand.MAIN_HAND, SmoulderingFungusItem.lit(helper.getLevel()));
        helper.getBlockState(fire).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, fire, Direction.UP));
        check(helper, !FireBlock.cold(helper.getBlockState(fire)), "the smouldering fungus left the fire cold");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theYearShowsBearsDenningAndLongSummerDays(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Biome biome = helper.getLevel().getBiome(pos).value();
        List<Year.Row> rows = Year.events(biome, pos);
        Year.Row bears = rows.stream().filter(row -> row.id().equals("bears")).findFirst().orElseThrow();
        check(helper, bears.at(JANUARY) == Year.QUIET && bears.at(JUNE) == Year.FULL, "bears were not in their dens in January and about in June");
        List<Year.Month> climate = Year.climate(biome, pos);
        check(helper, climate.get(JUNE - 1).daylightHours() > climate.get(DECEMBER - 1).daylightHours(), "June was no lighter than December");
        check(helper, climate.get(JUNE - 1).highC() > climate.get(JANUARY - 1).highC(), "June was no warmer than January");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void caribouMigrateTwiceAYear(GameTestHelper helper) {
        int migrations = 0;
        for (int day = 0; day < Balance.get().year().days(); day++) {
            if (Migration.caribouBegin(day)) {
                migrations++;
            }
        }
        check(helper, migrations == 2, "caribou migrated " + migrations + " times in a year");
        int secondYear = Balance.get().year().days() + 1;
        double hares = Migration.share(EntityType.RABBIT, secondYear);
        check(helper, hares == Balance.get().director().hareCycle().get(Calendar.year(secondYear) - 1), "the second year's hares were " + hares);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theHomesteadHasItsTasks(GameTestHelper helper) {
        check(helper, Chapter.HOMESTEAD.tasks().size() == HOMESTEAD_TASKS, "the homestead has " + Chapter.HOMESTEAD.tasks().size() + " tasks");
        helper.succeed();
    }
}
