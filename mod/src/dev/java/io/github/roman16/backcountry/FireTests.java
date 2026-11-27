package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Exertion;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Warmth;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.fire.FireMenu;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.fire.Lighting;
import io.github.roman16.backcountry.fire.WoodpileBlock;
import io.github.roman16.backcountry.fire.WoodpileEntity;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.HeadlampItem;
import io.github.roman16.backcountry.gear.Water;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class FireTests {
    private static final double CLOSE = 1e-6;
    private static final double MOMENT = 0.01;
    private static final int COOK_TICKS = 600;
    private static final int NIGHT_TICKS = 8000;

    private FireTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    static ItemStack wood(Item kind, double seasoning) {
        return FirewoodItem.seasoned(new ItemStack(kind), seasoning);
    }

    private static ItemStack softwood(double seasoning) {
        return wood(Things.SOFTWOOD.get(), seasoning);
    }

    static CampfireBlockEntity campfire(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Things.CAMPFIRE.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof CampfireBlockEntity campfire)) {
            throw new IllegalStateException("the campfire has no block entity");
        }
        return campfire;
    }

    static CampfireBlockEntity burning(GameTestHelper helper, BlockPos pos) {
        CampfireBlockEntity campfire = campfire(helper, pos);
        Fire fire = Fire.of(campfire);
        for (int slot = 0; slot < Fire.QUEUE; slot++) {
            fire.queue.set(slot, softwood(1));
        }
        fire.light();
        fire.advance(Balance.get().fire().kindlingHours() + MOMENT, false);
        Fire.show(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)), fire);
        return campfire;
    }

    @GameTest(template = "empty")
    public static void aFireBurnsItsQueueThenSettlesToCoals(GameTestHelper helper) {
        Fire fire = new Fire();
        fire.queue.set(0, softwood(1));
        fire.queue.set(1, softwood(1));
        fire.light();
        Balance.Fire balance = Balance.get().fire();
        double burnt = balance.kindlingHours() + 2 * balance.softwoodHours();
        fire.advance(burnt - MOMENT, false);
        check(helper, fire.lit() && !fire.queued(), "two pieces and the kindling did not burn in turn");
        fire.advance(2 * MOMENT, false);
        check(helper, fire.coals() && Math.abs(fire.coalHours() - burnt + MOMENT) < 2 * MOMENT, "the burnt-out fire banked " + fire.coalHours() + " h of coals");
        check(helper, Math.abs(fire.heatC() - balance.coalsC()) < CLOSE, "coals gave " + fire.heatC() + " °C");
        fire.advance(burnt, false);
        check(helper, !fire.lit() && !fire.coals(), "the coals never went cold");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firewoodCatchesFromTheCoals(GameTestHelper helper) {
        Fire fire = new Fire();
        fire.light();
        fire.advance(Balance.get().fire().kindlingHours() + MOMENT, false);
        check(helper, fire.coals(), "the kindling left no coals");
        fire.queue.set(0, softwood(1));
        fire.advance(MOMENT, false);
        check(helper, fire.lit() && !fire.queued(), "the queued piece did not catch from the coals");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rainBurnsAnOpenFireFaster(GameTestHelper helper) {
        Fire dry = new Fire();
        Fire wet = new Fire();
        for (Fire fire : List.of(dry, wet)) {
            fire.queue.set(0, softwood(1));
            fire.light();
        }
        dry.advance(1, false);
        wet.advance(1, true);
        check(helper, wet.burningHours(false) + wet.queuedHours(false) < dry.burningHours(false) + dry.queuedHours(false), "rain did not burn the wood faster");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theSettingSetsTheBurnAndTheHeat(GameTestHelper helper) {
        Fire low = new Fire();
        Fire hot = new Fire();
        low.setting = Fire.Setting.LOW;
        hot.setting = Fire.Setting.HOT;
        for (Fire fire : List.of(low, hot)) {
            fire.queue.set(0, softwood(1));
            fire.light();
            fire.advance(Balance.get().fire().kindlingHours() / fire.setting.burn() + MOMENT, false);
        }
        check(helper, hot.heatC() > low.heatC(), "Hot gave no more heat than Low");
        check(helper, hot.burningHours(false) < low.burningHours(false), "Hot burned no faster than Low");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void greenWoodIsTooGreenToCatch(GameTestHelper helper) {
        check(helper, !FirewoodItem.catches(new ItemStack(Things.HARDWOOD.get())), "fresh hardwood caught");
        check(helper, FirewoodItem.catches(new ItemStack(Things.SOFTWOOD.get())), "fresh softwood did not catch");
        ServerPlayer player = BackcountryTests.survivor(helper);
        FireMenu menu = new FireMenu(0, player.getInventory(), campfire(helper, new BlockPos(2, 1, 2)));
        check(helper, !menu.slots.get(0).mayPlace(new ItemStack(Things.HARDWOOD.get())), "the queue took green wood");
        check(helper, menu.slots.get(0).mayPlace(wood(Things.HARDWOOD.get(), 1)), "the queue refused seasoned hardwood");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 480)
    public static void anUntendedFireBurnsDownAndItsCoalsCatchFedWood(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CampfireBlockEntity campfire = campfire(helper, pos);
        BlockPos at = helper.absolutePos(pos);
        FireBlock.light(helper.getLevel(), at, helper.getLevel().getBlockState(at), campfire);
        int kindling = (int) Math.round(Balance.get().fire().kindlingHours() * Exertion.TICKS_AN_HOUR);
        int step = 2 * 20;
        Fire fire = Fire.of(campfire);
        helper.runAfterDelay(kindling + step, () -> {
            check(helper, fire.coals() && helper.getLevel().getBlockState(at).getValue(FireBlock.COALS), "the fire with nothing cooking never burnt its kindling down");
            fire.queue.set(0, softwood(1));
            campfire.setChanged();
        });
        helper.runAfterDelay(kindling + 2 * step, () -> {
            check(helper, fire.lit() && !fire.queued() && helper.getLevel().getBlockState(at).getValue(CampfireBlock.LIT), "the coals did not catch the queued piece");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void potsGoOnTheFire(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        burning(helper, pos);
        ServerPlayer player = BackcountryTests.survivor(helper);
        BlockPos at = helper.absolutePos(pos);
        for (ItemStack pot : List.of(new ItemStack(Things.CAMP_POT.get()), new ItemStack(Things.COOKING_POT.get()))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, pot);
            player.gameMode.useItemOn(player, helper.getLevel(), pot, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.4, 0), Direction.UP, at, false));
            check(helper, !helper.getBlockState(pos.above()).isAir(), BuiltInRegistries.ITEM.getKey(pot.getItem()) + " did not go on the fire");
            helper.setBlock(pos.above(), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aShovelPutsTheFireOut(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CampfireBlockEntity campfire = burning(helper, pos);
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        BlockPos at = helper.absolutePos(pos);
        helper.getLevel().getBlockState(at).useItemOn(shovel, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        Fire fire = Fire.of(campfire);
        check(helper, !fire.lit() && !fire.coals() && FireBlock.cold(helper.getLevel().getBlockState(at)), "the shovel left the fire burning");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aPackedFireSetsTheShelterAlight(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        CampfireBlockEntity campfire = burning(helper, pos);
        Fire fire = Fire.of(campfire);
        fire.setting = Fire.Setting.HOT;
        fire.queue.set(Fire.QUEUE - 1, softwood(1));
        check(helper, fire.packed(), "a Hot fire with a full queue was not packed too full");
        for (BlockPos roof : BlockPos.betweenClosed(new BlockPos(1, 3, 1), new BlockPos(5, 3, 5))) {
            helper.setBlock(roof, Blocks.SPRUCE_PLANKS);
        }
        BlockPos at = helper.absolutePos(pos);
        for (int hour = 0; hour < 64 && !alight(helper); hour++) {
            Fire.spark(helper.getLevel(), at, 1);
        }
        check(helper, alight(helper), "64 hours of sparks set nothing alight");
        helper.succeed();
    }

    private static boolean alight(GameTestHelper helper) {
        for (BlockPos near : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(5, 3, 5))) {
            if (helper.getBlockState(near).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = "empty")
    public static void theBowDrillLightsAColdFire(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CampfireBlockEntity campfire = campfire(helper, pos);
        ServerPlayer player = BackcountryTests.survivor(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Things.BOW_DRILL.get()));
        BlockPos at = helper.absolutePos(pos);
        Lighting.drilled(player, at, 16, true);
        check(helper, FireBlock.cold(helper.getLevel().getBlockState(at)), "the fire caught without tinder");
        player.getInventory().setItem(1, new ItemStack(Things.TINDER_BUNDLE.get()));
        player.getInventory().setItem(2, new ItemStack(Items.STICK, Balance.get().fire().kindlingSticks()));
        Lighting.drilled(player, at, 16, false);
        check(helper, FireBlock.cold(helper.getLevel().getBlockState(at)), "an unfinished drilling lit the fire");
        Lighting.drilled(player, at, 16, true);
        BlockState state = helper.getLevel().getBlockState(at);
        check(helper, state.getValue(CampfireBlock.LIT) && Fire.of(campfire).lit(), "the bow drill did not light the fire");
        check(helper, player.getInventory().getItem(1).isEmpty() && player.getInventory().getItem(2).isEmpty(), "the tinder and kindling were not used");
        check(helper, player.getMainHandItem().getDamageValue() == 1, "the bow drill did not wear");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theFireWarmsByItsSetting(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Fire fire = Fire.of(burning(helper, new BlockPos(3, 1, 1)));
        Body body = Metabolism.body(player);
        int day = Calendar.day(helper.getLevel());
        fire.setting = Fire.Setting.LOW;
        Warmth.sense(player, body, day);
        double low = body.effectiveC;
        fire.setting = Fire.Setting.HOT;
        Warmth.sense(player, body, day);
        check(helper, body.fire && body.effectiveC > low, "a Hot fire felt no warmer than a Low one");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void splittingALogWearsTheAxe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.SPRUCE_LOG), new ItemStack(Items.IRON_AXE)));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        check(helper, recipe.isPresent(), "a spruce log and an axe split into nothing");
        ItemStack pieces = recipe.get().value().assemble(input, level.registryAccess());
        check(helper, pieces.is(Things.SOFTWOOD.get()) && pieces.getCount() == 2, "a spruce log split into " + pieces);
        ItemStack axe = recipe.get().value().getRemainingItems(input).get(1);
        check(helper, axe.is(Items.IRON_AXE) && axe.getDamageValue() == 1, "the axe came back as " + axe);
        CraftingInput birch = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BIRCH_LOG), new ItemStack(Items.STONE_AXE)));
        ItemStack hardwood = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, birch, level).orElseThrow().value().assemble(birch, level.registryAccess());
        check(helper, hardwood.is(Things.HARDWOOD.get()) && !FirewoodItem.catches(hardwood), "a birch log split into " + hardwood);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strippingABirchPeelsItsBark(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.BIRCH_LOG);
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        BlockPos at = helper.absolutePos(pos);
        axe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false)));
        check(helper, helper.getBlockState(pos).is(Blocks.STRIPPED_BIRCH_LOG), "the birch log was not stripped");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        check(helper, dropped.stream().anyMatch(item -> item.getItem().is(Things.BIRCH_BARK.get())), "stripping the birch peeled no bark");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aWoodpileDriesItsWood(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos.above(2), Blocks.STONE);
        helper.setBlock(pos, Things.WOODPILE.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof WoodpileEntity woodpile)) {
            throw new IllegalStateException("the woodpile has no block entity");
        }
        woodpile.add(new ItemStack(Things.HARDWOOD.get()));
        woodpile.add(new ItemStack(Things.SOFTWOOD.get()));
        woodpile.dry(24);
        ItemStack softwood = woodpile.removeItemNoUpdate(1);
        ItemStack hardwood = woodpile.removeItemNoUpdate(0);
        Balance.Firewood firewood = Balance.get().firewood();
        check(helper, FirewoodItem.seasoning(softwood) > firewood.freshSoftwood() && FirewoodItem.seasoning(hardwood) > firewood.freshHardwood(),
                "a day laid out dried nothing: " + softwood + ", " + hardwood);
        check(helper, helper.getBlockState(pos).isAir(), "the emptied woodpile stayed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theHeadlampDimsInTheColdAndRunsDown(GameTestHelper helper) {
        Balance.Headlamp lamp = Balance.get().headlamp();
        check(helper, HeadlampItem.light(lamp.chargeHours(), 0) == lamp.light(), "a charged lamp did not shine fully");
        check(helper, HeadlampItem.light(lamp.chargeHours(), lamp.dimmestC()) < lamp.light() / 2, "the lamp did not dim in extreme cold");
        check(helper, HeadlampItem.light(0, 0) == 0, "an empty lamp shone");
        ItemStack dark = new ItemStack(Things.HEADLAMP.get());
        HeadlampItem.shine(dark, 1000, true, 0);
        check(helper, Math.abs(HeadlampItem.charge(dark) - (lamp.chargeHours() - 1)) < MOMENT, "an hour worn in the dark left " + HeadlampItem.charge(dark) + " h");
        ItemStack lit = new ItemStack(Things.HEADLAMP.get());
        HeadlampItem.shine(lit, 1000, false, 0);
        check(helper, Math.abs(HeadlampItem.charge(lit) - lamp.chargeHours()) < MOMENT && lit.getOrDefault(Things.LIGHT.get(), 0) == 0,
                "an hour worn in daylight ran the lamp down to " + HeadlampItem.charge(lit) + " h");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aNightSleptCooksAndBoils(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CampfireBlockEntity campfire = burning(helper, pos);
        BlockPos at = helper.absolutePos(pos);
        campfire.placeFood(null, new ItemStack(Items.RABBIT), COOK_TICKS);
        helper.setBlock(pos.above(), Things.CAMP_POT_BLOCK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(at.above()) instanceof CampPotEntity pot)) {
            throw new IllegalStateException("the camp pot has no block entity");
        }
        pot.hold(Water.drawn(Water.Source.LAKE, (float) Balance.get().water().potL()));
        Fire.slept(helper.getLevel(), at, campfire, Fire.of(campfire), NIGHT_TICKS);
        check(helper, pot.water().boiled(), "a night slept over the fire left the pot unboiled");
        check(helper, campfire.getItems().stream().allMatch(ItemStack::isEmpty), "a night slept left the rabbit on the fire");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        check(helper, dropped.stream().anyMatch(item -> item.getItem().is(Items.COOKED_RABBIT)), "the rabbit did not come off cooked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aHotFireBoilsSooner(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CampfireBlockEntity campfire = burning(helper, pos);
        BlockPos above = helper.absolutePos(pos.above());
        helper.setBlock(pos.above(), Things.CAMP_POT_BLOCK.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(above) instanceof CampPotEntity pot)) {
            throw new IllegalStateException("the camp pot has no block entity");
        }
        pot.hold(Water.drawn(Water.Source.LAKE, 1));
        int steady = pot.ticksLeft(CampPotEntity.heat(helper.getLevel(), above));
        Fire.of(campfire).setting = Fire.Setting.HOT;
        int hot = pot.ticksLeft(CampPotEntity.heat(helper.getLevel(), above));
        Fire.of(campfire).setting = Fire.Setting.LOW;
        int low = pot.ticksLeft(CampPotEntity.heat(helper.getLevel(), above));
        check(helper, hot * 2 == steady && low == steady * 2, "boiling took " + low + ", " + steady + " and " + hot + " ticks on Low, Steady and Hot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullWoodpilesStack(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        int full = Balance.get().firewood().pilePieces();
        helper.setBlock(pos, Things.WOODPILE.get().defaultBlockState().setValue(WoodpileBlock.PIECES, full));
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof WoodpileEntity woodpile)) {
            throw new IllegalStateException("the woodpile has no block entity");
        }
        for (int piece = 0; piece < full; piece++) {
            woodpile.add(softwood(1));
        }
        ServerPlayer player = BackcountryTests.survivor(helper);
        ItemStack wood = softwood(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, wood);
        BlockPos at = helper.absolutePos(pos);
        wood.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)));
        check(helper, helper.getBlockState(pos.above()).is(Things.WOODPILE.get()), "a full woodpile took no pile on top");
        if (!(helper.getLevel().getBlockEntity(at.above()) instanceof WoodpileEntity top)) {
            throw new IllegalStateException("the pile on top has no block entity");
        }
        top.removeItemNoUpdate(0);
        check(helper, helper.getBlockState(pos.above()).isAir() && woodpile.pieces().size() == full, "taking the top pile's last piece left it standing or emptied the lower one");
        helper.succeed();
    }
}
