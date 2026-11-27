package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.food.Age;
import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.CookingPotEntity;
import io.github.roman16.backcountry.food.DryingRackEntity;
import io.github.roman16.backcountry.food.Pantry;
import io.github.roman16.backcountry.food.Rations;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.food.SmokerEntity;
import io.github.roman16.backcountry.food.Spoilage;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import io.github.roman16.backcountry.guide.FieldNotes;
import io.github.roman16.backcountry.survivor.Team;
import io.github.roman16.backcountry.trap.Kind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class FoodTests {
    private static final double CLOSE = 1e-3;
    private static final double FAR_BLOCKS = 4096;

    private FoodTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static CacheEntity cache(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Things.FOOD_CACHE.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof CacheEntity cache)) {
            throw new IllegalStateException("the food cache has no block entity");
        }
        cache.report();
        return cache;
    }

    @GameTest(template = "empty")
    public static void warmthSpeedsSpoiling(GameTestHelper helper) {
        Balance.Spoilage spoilage = Balance.get().spoilage();
        check(helper, Math.abs(Spoilage.rate(spoilage.referenceC()) - 1) < CLOSE, "food at the reference warmth did not age a day a day");
        check(helper, Math.abs(Spoilage.rate(spoilage.referenceC() + spoilage.doublingC()) - 2) < CLOSE, "10 °C warmer did not double the spoiling");
        check(helper, Spoilage.rate(spoilage.frozenBelowC() - 1) < Spoilage.rate(spoilage.frozenBelowC()) / 16, "frozen food kept spoiling");
        ItemStack beef = new ItemStack(Items.BEEF);
        Spoilage.age(beef, 1, spoilage.referenceC());
        check(helper, Math.abs(Spoilage.age(beef) - 1) < CLOSE, "a day at the reference warmth aged beef " + Spoilage.age(beef));
        Spoilage.age(beef, 64, spoilage.referenceC());
        check(helper, Spoilage.spoiled(beef), "beef kept 65 days at 10 °C");
        check(helper, Spoilage.life(new ItemStack(Things.PILOT_BREAD.get())).isEmpty(), "pilot bread spoils");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spoiledFoodPoisons(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack rabbit = new ItemStack(Items.COOKED_RABBIT);
        Spoilage.age(rabbit, 1024, Balance.get().spoilage().referenceC());
        Metabolism.eat(player, rabbit);
        check(helper, Metabolism.body(player).has(Condition.FOOD_POISONING), "spoiled rabbit did not poison");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stewQuenchesThirst(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.waterL = 1;
        Metabolism.eat(player, new ItemStack(Items.MUSHROOM_STEW));
        check(helper, body.waterL > 1, "mushroom stew gave no water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aCacheFeedsTheRation(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        CacheEntity cache = cache(helper, new BlockPos(3, 1, 3));
        cache.setItem(0, new ItemStack(Items.BEEF, 4));
        cache.setItem(1, new ItemStack(Items.COOKED_RABBIT, 4));
        double eaten = Rations.eat(player, Balance.get().rations().normalKcal(), true);
        check(helper, eaten >= Balance.get().rations().normalKcal(), "the ration ate " + eaten + " kcal");
        check(helper, cache.getItem(0).getCount() == 4, "the ration ate raw beef");
        check(helper, cache.getItem(1).getCount() < 4, "the ration left the cooked rabbit");
        Team.get(helper.getLevel().getServer()).emptied(helper.absolutePos(new BlockPos(3, 1, 3)));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void carriedFoodFeedsTheRationInTheField(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        player.moveTo(player.getX() + FAR_BLOCKS, player.getY(), player.getZ());
        player.getInventory().setItem(4, new ItemStack(Things.PILOT_BREAD.get(), 16));
        double eaten = Rations.eat(player, Balance.get().rations().leanKcal(), false);
        check(helper, eaten >= Balance.get().rations().leanKcal() && player.getInventory().getItem(4).getCount() < 16, "the field ration ate " + eaten + " kcal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void miceRaidACache(GameTestHelper helper) {
        CacheEntity cache = cache(helper, new BlockPos(3, 1, 3));
        cache.setItem(0, new ItemStack(Items.COOKED_RABBIT, 4));
        double before = Pantry.usableKcal(cache.stacks());
        check(helper, cache.raid(helper.getLevel().random), "the mice found no food");
        check(helper, Pantry.usableKcal(cache.stacks()) < before, "the mice ate nothing");
        check(helper, cache.stacks().stream().anyMatch(stack -> stack.is(Things.MOUSE_DROPPINGS.get())), "the mice left no droppings");
        check(helper, cache.stacks().stream().anyMatch(stack -> stack.has(Things.CONTAMINATED.get())), "the mice contaminated nothing");
        Team.get(helper.getLevel().getServer()).emptied(helper.absolutePos(new BlockPos(3, 1, 3)));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theSmokerSmokesMeat(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Things.SMOKER.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof SmokerEntity smoker)) {
            throw new IllegalStateException("the smoker has no block entity");
        }
        smoker.setItem(0, new ItemStack(Items.BEEF, 2));
        smoker.setItem(SmokerEntity.HANGING, FirewoodItem.seasoned(new ItemStack(Things.HARDWOOD.get(), 4), 1));
        Balance.Smoker balance = Balance.get().smoker();
        smoker.advance(balance.risingHours() + balance.batchHours() - 1);
        check(helper, smoker.getItem(0).is(Items.BEEF), "the beef was done before its batch");
        smoker.advance(2);
        ItemStack smoked = smoker.getItem(0);
        check(helper, smoked.is(Items.COOKED_BEEF) && smoked.getCount() == 2 && smoked.has(Things.SMOKED.get()), "the smoker gave " + smoked);
        double cooked = Spoilage.life(new ItemStack(Items.COOKED_BEEF)).orElseThrow();
        check(helper, Spoilage.life(smoked).orElseThrow() > cooked * 16, "smoked beef keeps no longer than cooked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCookingPotCooksStew(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 2);
        FireTests.burning(helper, fire);
        helper.setBlock(fire.above(), Things.COOKING_POT_BLOCK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(fire.above())) instanceof CookingPotEntity pot)) {
            throw new IllegalStateException("the cooking pot has no block entity");
        }
        pot.pour(1);
        check(helper, pot.add(new ItemStack(Items.BROWN_MUSHROOM)) && pot.add(new ItemStack(Items.BROWN_MUSHROOM)), "the pot refused mushrooms");
        check(helper, !pot.add(new ItemStack(Items.STONE)), "the pot took a stone");
        pot.advance(Balance.get().cooking().stewHours() + CLOSE, true);
        check(helper, pot.stew().is(Items.MUSHROOM_STEW), "the pot cooked " + pot.stew());
        check(helper, pot.waterL() < 1, "the stew used no water");
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack bowl = new ItemStack(Items.BOWL);
        player.setItemInHand(InteractionHand.MAIN_HAND, bowl);
        BlockPos at = helper.absolutePos(fire.above());
        helper.getLevel().getBlockState(at).useItemOn(bowl, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        check(helper, player.getInventory().contains(new ItemStack(Items.MUSHROOM_STEW)), "the bowl took no stew");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aMooseHeadMakesSoupForTheWaterThereIs(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 2);
        FireTests.burning(helper, fire);
        helper.setBlock(fire.above(), Things.COOKING_POT_BLOCK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(fire.above())) instanceof CookingPotEntity pot)) {
            throw new IllegalStateException("the cooking pot has no block entity");
        }
        Balance.Cooking cooking = Balance.get().cooking();
        pot.pour(cooking.servingL() * 3);
        check(helper, pot.add(new ItemStack(Things.MOOSE_HEAD.get())), "the pot refused the moose head");
        pot.advance(cooking.stewHours() + CLOSE, true);
        check(helper, pot.stew().is(Things.MOOSE_HEAD_SOUP.get()) && pot.stew().getCount() == 3 && pot.waterL() < CLOSE,
                "three servings of water made " + pot.stew() + " and left " + pot.waterL() + " L");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void offalCooksOnTheFire(GameTestHelper helper) {
        for (Item raw : List.of(Things.HEART.get(), Things.LIVER.get(), Things.TONGUE.get(), Things.KIDNEY.get())) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(raw));
            ItemStack cooked = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, helper.getLevel())
                    .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
            check(helper, Nutrition.of(cooked).map(food -> !food.raw()).orElse(false), raw + " cooked into " + cooked);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aVesselPoursOut(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        bottle.set(Things.WATER.get(), Water.drawn(Water.Source.LAKE, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        player.setShiftKeyDown(true);
        bottle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(helper, WaterVesselItem.water(player.getMainHandItem()).empty(), "pouring left " + WaterVesselItem.water(player.getMainHandItem()));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueberriesArePicked(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(pos, Things.BLUEBERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        ServerPlayer player = BackcountryTests.survivor(helper);
        BlockPos at = helper.absolutePos(pos);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        check(helper, dropped.stream().anyMatch(item -> item.getItem().is(Things.BLUEBERRIES.get())), "picking the bush gave no blueberries");
        check(helper, helper.getBlockState(pos).getValue(SweetBerryBushBlock.AGE) == 1, "the picked bush kept its berries");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foodPickedWithinAQuarterDayStacks(GameTestHelper helper) {
        double window = Balance.get().spoilage().stackDays();
        ItemStack early = new ItemStack(Items.SWEET_BERRIES);
        ItemStack later = new ItemStack(Items.SWEET_BERRIES);
        early.set(Things.AGE.get(), new Age((float) (window * 0.75)));
        later.set(Things.AGE.get(), new Age((float) (window * 0.25)));
        check(helper, ItemStack.isSameItemSameComponents(early, later), "berries picked within a quarter day did not stack");
        later.set(Things.AGE.get(), new Age((float) (window * 1.25)));
        check(helper, !ItemStack.isSameItemSameComponents(early, later), "berries picked a quarter day apart stacked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bushesBearAsTheMonthStands(GameTestHelper helper) {
        int may = 5;
        check(helper, Seasons.ripest(may, Blocks.SWEET_BERRY_BUSH) == 2, "lingonberries kept none of last year's berries into May");
        check(helper, Seasons.ripest(may, Things.SALMONBERRY_BUSH.get()) == 1, "salmonberries were ripe in May");
        check(helper, Seasons.ripest(7, Blocks.SWEET_BERRY_BUSH) == 1, "lingonberries kept berries into July");
        check(helper, Seasons.ripest(8, Blocks.SWEET_BERRY_BUSH) == 3, "lingonberries were not ripe in August");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void sixAnimalFatRenderThree(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 2);
        FireTests.burning(helper, fire);
        helper.setBlock(fire.above(), Things.COOKING_POT_BLOCK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(fire.above())) instanceof CookingPotEntity pot)) {
            throw new IllegalStateException("the cooking pot has no block entity");
        }
        pot.pour(Balance.get().cooking().potL());
        for (int i = 0; i < 5; i++) {
            pot.add(new ItemStack(Things.ANIMAL_FAT.get()));
        }
        pot.advance(Balance.get().cooking().stewHours() + CLOSE, true);
        check(helper, pot.stew().isEmpty(), "five fat rendered " + pot.stew());
        pot.add(new ItemStack(Things.ANIMAL_FAT.get()));
        pot.advance(Balance.get().cooking().stewHours() + CLOSE, true);
        check(helper, pot.stew().is(Things.RENDERED_FAT.get()) && pot.stew().getCount() == 3, "six fat rendered " + pot.stew());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingSlowsSpoiling(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, Things.DRYING_RACK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(at)) instanceof DryingRackEntity rack)) {
            throw new IllegalStateException("the drying rack has no block entity");
        }
        ItemStack hung = new ItemStack(Items.BEEF);
        ItemStack lying = hung.copy();
        rack.setItem(0, hung);
        double air = Spoilage.airC(helper.getLevel(), helper.absolutePos(at));
        for (int hour = 0; hour < 24; hour++) {
            rack.advance(1);
            Spoilage.age(lying, 1 / 24.0, air);
        }
        check(helper, rack.driedShare(0) > 0 && Spoilage.age(rack.getItem(0)) < Spoilage.age(lying), "drying did not slow the spoiling: " + Spoilage.age(rack.getItem(0))
                + " against " + Spoilage.age(lying));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bearMeatIsMeatAndFatIsNot(GameTestHelper helper) {
        check(helper, Pantry.meaty(new ItemStack(Items.COOKED_BEEF)) && SmokerEntity.hangs(new ItemStack(Items.COOKED_BEEF)), "cooked bear meat is no meat");
        check(helper, !Pantry.meaty(new ItemStack(Things.ANIMAL_FAT.get())), "animal fat counts as meat");
        check(helper, !SmokerEntity.hangs(new ItemStack(Things.MOOSE_HEAD.get())), "the moose head hangs in the smoker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aMooseHeadSpoils(GameTestHelper helper) {
        ItemStack head = new ItemStack(Things.MOOSE_HEAD.get());
        check(helper, Spoilage.life(head).isPresent(), "the moose head keeps forever");
        Spoilage.age(head, Spoilage.life(head).orElseThrow() + 1, Balance.get().spoilage().referenceC());
        check(helper, Spoilage.spoiled(head) && !CookingPotEntity.cooks(helper.getLevel(), head), "the pot takes a spoiled moose head");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foodThatPoisonsIsRemembered(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        check(helper, Metabolism.eat(player, new ItemStack(Things.BANEBERRIES.get())), "the survivor ate no baneberries");
        check(helper, Metabolism.body(player).has(Condition.FOOD_POISONING), "baneberries did not poison");
        check(helper, FieldNotes.get(player.server).sickening(player).contains("backcountry:baneberries"), "the poisoning was not remembered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meatAndFishStackToSixteen(GameTestHelper helper) {
        for (Item food : List.of(Items.COOKED_BEEF, Items.BEEF, Items.RABBIT, Things.ANIMAL_FAT.get(), Things.HEART.get())) {
            check(helper, new ItemStack(food).getMaxStackSize() == 16, food + " stacks to " + new ItemStack(food).getMaxStackSize());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gutsBaitTheTipUp(GameTestHelper helper) {
        check(helper, Kind.TIP_UP.takes(new ItemStack(Things.GAME_GUTS.get())), "the tip-up takes no guts");
        check(helper, !Kind.SMALL_SNARE.takes(new ItemStack(Things.GAME_GUTS.get())), "a snare takes guts");
        helper.succeed();
    }
}
