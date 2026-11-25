package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.food.BirchTapBlock;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.food.Spoilage;
import io.github.roman16.backcountry.food.StewRecipe;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class ForageTests {
    private static final int TICKS_A_SECOND = 20;
    private static final double CLOSE = 0.01;
    private static final double COLD_C = -20;

    private ForageTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static CampPotEntity pot(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 2);
        FireTests.burning(helper, fire);
        helper.setBlock(fire.above(), Things.CAMP_POT_BLOCK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(fire.above())) instanceof CampPotEntity pot)) {
            throw new IllegalStateException("the camp pot has no block entity");
        }
        return pot;
    }

    private static void boil(GameTestHelper helper, CampPotEntity pot, int seconds) {
        BlockPos at = pot.getBlockPos();
        for (int tick = 0; tick < seconds * TICKS_A_SECOND; tick++) {
            CampPotEntity.tick(helper.getLevel(), at, helper.getLevel().getBlockState(at), pot);
        }
    }

    @GameTest(template = "empty")
    public static void spruceNeedlesBoilIntoTeaWithVitaminC(GameTestHelper helper) {
        CampPotEntity pot = pot(helper);
        pot.hold(Water.of(false, 2));
        check(helper, pot.steep(), "a pot of water took no needles");
        boil(helper, pot, Balance.get().water().boilSeconds());
        check(helper, pot.water().drink() == Water.Drink.SPRUCE_TEA && pot.water().boiled(), "boiling needles made " + pot.water());
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.vitaminCMg = 0;
        Metabolism.drink(player, 1, pot.water());
        check(helper, Math.abs(body.vitaminCMg - Balance.get().forage().teaVitaminCMgPerL()) < CLOSE, "a litre of spruce tea gave " + body.vitaminCMg + " mg of vitamin C");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void birchSapBoilsDownToSyrup(GameTestHelper helper) {
        CampPotEntity pot = pot(helper);
        pot.hold(new Water(Water.Drink.BIRCH_SAP, false, (float) Balance.get().forage().syrupL()));
        boil(helper, pot, Balance.get().forage().syrupSeconds());
        check(helper, pot.water().empty(), "the sap did not boil down");
        boolean syrup = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pot.getBlockPos()).inflate(2))
                .stream().filter(item -> item.getItem().is(Things.BIRCH_SYRUP.get())).toList().isEmpty();
        check(helper, syrup, "2 L of sap left no Birch Syrup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBirchTapFillsTheBottleWithSap(GameTestHelper helper) {
        BlockPos log = new BlockPos(2, 1, 2);
        helper.setBlock(log, Blocks.BIRCH_LOG.defaultBlockState());
        BlockPos tap = log.north();
        helper.setBlock(tap, Things.BIRCH_TAP_BLOCK.get().defaultBlockState().setValue(BirchTapBlock.FACING, Direction.NORTH).setValue(BirchTapBlock.SAP, BirchTapBlock.FULL));
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        BlockPos at = helper.absolutePos(tap);
        helper.getLevel().getBlockState(at).useItemOn(bottle, helper.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
        Water held = WaterVesselItem.water(player.getMainHandItem());
        check(helper, held.drink() == Water.Drink.BIRCH_SAP && held.litres() > 0, "the bottle took " + held + " from a full tap");
        helper.setBlock(log, Blocks.AIR.defaultBlockState());
        check(helper, helper.getBlockState(tap).isAir(), "the tap outlived its birch");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void berriesRipenInTheirMonths(GameTestHelper helper) {
        check(helper, Seasons.within(1, 9, 3) && Seasons.within(9, 9, 3) && !Seasons.within(6, 9, 3), "highbush cranberries' months do not run from September into March");
        check(helper, Nutrition.of(new ItemStack(Things.BANEBERRIES.get())).map(Nutrition::poisoning).orElse(0.0) >= 1, "baneberries do not poison");
        check(helper, Nutrition.of(new ItemStack(Things.SALMONBERRIES.get())).map(Nutrition::vitaminC).orElse(0.0) > 0, "salmonberries carry no vitamin C");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aRedMushroomPoisonsTheStew(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(Things.STEWING.get());
        StewRecipe.Pot safe = new StewRecipe.Pot(List.of(new ItemStack(Items.BROWN_MUSHROOM), new ItemStack(Items.BROWN_MUSHROOM)));
        StewRecipe.Pot mixed = new StewRecipe.Pot(List.of(new ItemStack(Items.BROWN_MUSHROOM), new ItemStack(Items.RED_MUSHROOM)));
        ItemStack good = recipes.stream().map(RecipeHolder::value).filter(recipe -> recipe.matches(safe, helper.getLevel())).findFirst().orElseThrow()
                .assemble(safe, helper.getLevel().registryAccess());
        ItemStack bad = recipes.stream().map(RecipeHolder::value).filter(recipe -> recipe.matches(mixed, helper.getLevel())).findFirst().orElseThrow()
                .assemble(mixed, helper.getLevel().registryAccess());
        check(helper, Nutrition.of(good).map(Nutrition::poisoning).orElse(1.0) == 0, "two brown mushrooms made a poisoned stew");
        check(helper, Nutrition.of(bad).map(Nutrition::poisoning).orElse(0.0) >= 1, "a red mushroom left the stew safe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishFrozenThroughCarriesNoParasites(GameTestHelper helper) {
        ItemStack fish = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riverfishing", "northern_pike")));
        CompoundTag data = new CompoundTag();
        data.putString("Species", "riverfishing:northern_pike");
        data.putInt("WeightG", 2048);
        fish.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        check(helper, Nutrition.of(fish).orElseThrow().raw(), "a fresh raw pike is safe raw");
        Spoilage.age(fish, Balance.get().fish().frozenDays(), COLD_C);
        check(helper, Fish.frozenThrough(fish) && !Nutrition.of(fish).orElseThrow().raw(), "a pike a day at -20 °C is not frozen through");
        check(helper, Nutrition.of(fish).orElseThrow().vitaminC() > 0, "freezing took the pike's vitamin C");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deadSpruceSplitsIntoDryFirewood(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(Backcountry.id("softwood_from_dead_spruce")).orElseThrow();
        ItemStack split = recipe.value().getResultItem(helper.getLevel().registryAccess());
        check(helper, FirewoodItem.catches(split) && FirewoodItem.seasoning(split) >= 1, "dead spruce split into " + split + " at " + FirewoodItem.seasoning(split));
        helper.succeed();
    }
}
