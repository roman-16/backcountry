package io.github.roman16.backcountry;

import com.mojang.authlib.GameProfile;
import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.body.Warmth;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.LeftPackEntity;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import io.github.roman16.backcountry.survivor.Arrival;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.survivor.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class BackcountryTests {
    private static final long DAY = 24000;
    private static final long HOUR = 1000;
    private static final double KG = 0.01;

    private BackcountryTests() {}

    static ServerPlayer survivor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "survivor"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        int day = Calendar.day(level);
        Metabolism.body(player).reset(day, Calendar.year(day));
        return player;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static void warm(GameTestHelper helper, ServerPlayer player) {
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Things.HIDE_HAT.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Things.HIDE_COAT.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Things.HIDE_PANTS.get()));
    }

    @GameTest(template = "empty")
    public static void everyFoodHasItsData(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.components().get(DataComponents.FOOD) != null && Nutrition.of(new ItemStack(item)).isEmpty()) {
                missing.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
        }
        check(helper, missing.isEmpty(), "foods without an entry in data/backcountry/data_maps/item/food.json: " + String.join(", ", missing.stream().sorted().toList()));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aRestingDayBurnsByWeight(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        warm(helper, player);
        Body body = Metabolism.body(player);
        Metabolism.step(player, body, DAY, false);
        double resting = Balance.get().body().restingKcalPerKgDay() * Balance.get().body().arrivalKg();
        check(helper, Math.abs(body.kcalBurnedToday - resting) < 1, "a warm resting day burned " + body.kcalBurnedToday + " kcal, not " + resting);
        double expected = Balance.get().body().arrivalKg() - resting / Balance.get().body().kcalPerKg();
        check(helper, Math.abs(body.weightKg - expected) < KG, "a day without food left " + body.weightKg + " kg, not " + expected);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pilotBreadFillsTheStomach(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        Nutrition bread = Nutrition.of(new ItemStack(Things.PILOT_BREAD.get())).orElseThrow();
        check(helper, bread.kcal() == 100 && bread.fat() == 2.5, "Pilot Bread is " + bread);
        int eaten = 0;
        while (Metabolism.eat(player, bread)) {
            eaten++;
        }
        check(helper, eaten == (int) (Balance.get().body().stomachKcal() / bread.kcal()), "a stomach took " + eaten + " Pilot Bread");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepRestoresEnergy(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        warm(helper, player);
        Body body = Metabolism.body(player);
        body.energy = 0.25;
        Metabolism.step(player, body, (long) (Balance.get().energy().sleepHoursToFull() * HOUR), true);
        check(helper, body.energy > 0.95, "a night's sleep left energy at " + body.energy);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noEnergyBlacksOut(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        body.energy = 0.0001;
        body.workKcal = Balance.get().energy().workKcalToEmpty();
        Metabolism.step(player, body, Metabolism.STEP_TICKS, false);
        check(helper, body.blackoutTicks > 0, "energy at " + body.energy + " did not black out");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyUnboiledWaterInfects(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        for (int i = 0; i < 64; i++) {
            Metabolism.drink(player, 0, true);
        }
        check(helper, !body.has(Condition.GUT_INFECTION), "boiled water gave a gut infection");
        for (int i = 0; i < 64 && !body.has(Condition.GUT_INFECTION); i++) {
            Metabolism.drink(player, 0, false);
        }
        check(helper, body.has(Condition.GUT_INFECTION), "64 drinks of lake water never infected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCampPotBoilsOnALitFire(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 2);
        helper.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(fire.above(), Things.CAMP_POT_BLOCK.get().defaultBlockState());
        BlockPos at = helper.absolutePos(fire.above());
        if (!(helper.getLevel().getBlockEntity(at) instanceof CampPotEntity pot)) {
            throw new IllegalStateException("the camp pot has no block entity");
        }
        pot.hold(new Water(false, 2));
        for (int tick = 0; tick < Balance.get().water().boilSeconds() * 20; tick++) {
            CampPotEntity.tick(helper.getLevel(), at, helper.getLevel().getBlockState(at), pot);
        }
        check(helper, pot.water().boiled(), "the pot's water did not boil");
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        ServerPlayer player = survivor(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        helper.getLevel().getBlockState(at).useItemOn(bottle, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        Water poured = WaterVesselItem.water(player.getMainHandItem());
        check(helper, poured.boiled() && Math.abs(poured.litres() - Balance.get().water().bottleL()) < KG, "the bottle took " + poured);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coldCoolsTheCore(GameTestHelper helper) {
        Body body = new Body();
        body.reset(0, 1);
        body.clo = Balance.get().warmth().baseClo();
        body.effectiveC = -20;
        for (int hour = 0; hour < 4; hour++) {
            Warmth.core(body, 1);
        }
        check(helper, body.coreC < Balance.get().conditions().frostCoreC(), "four hours at -20 °C left the core at " + body.coreC);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aRoofIsShelter(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        Warmth.sense(player, body, 0);
        check(helper, !body.sheltered, "open sky counted as shelter");
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.SPRUCE_PLANKS);
        Warmth.sense(player, body, 0);
        check(helper, body.sheltered, "a roof 3 blocks up was no shelter");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTheHotbarAndThePackHold(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.getInventory().clearContent();
        for (int i = 0; i < 12; i++) {
            player.getInventory().add(new ItemStack(Items.STICK, 64));
        }
        int used = 0;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            used += player.getInventory().items.get(slot).isEmpty() ? 0 : 1;
        }
        check(helper, used == Hotbar.ROW, "12 stacks took " + used + " slots");
        Slot row = player.inventoryMenu.getSlot(Hotbar.ROW);
        check(helper, !row.mayPlace(new ItemStack(Items.STICK)), "a locked row took an item");
        Metabolism.body(player).pack = new ItemStack(Things.HIDE_PACK.get());
        check(helper, row.mayPlace(new ItemStack(Items.STICK)), "the worn pack's row refused an item");
        row.set(new ItemStack(Items.FLINT, 3));
        ItemStack pack = Metabolism.body(player).pack.copy();
        check(helper, Hotbar.contents(pack).get(0).is(Items.FLINT), "the pack's row did not keep its item in the pack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bmi17Extracts(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.getInventory().setItem(0, new ItemStack(Items.FLINT, 5));
        Body body = Metabolism.body(player);
        body.weightKg = 55;
        BlockPos stood = player.blockPosition();
        Metabolism.step(player, body, Metabolism.STEP_TICKS, false);
        check(helper, body.ended != null && body.ended.kind() == Summary.Kind.EXTRACTED, "a survivor at 55 kg was not extracted");
        check(helper, player.isSpectator(), "the extracted survivor stayed in the world");
        boolean left = false;
        for (BlockPos at : BlockPos.betweenClosed(stood.offset(-8, -8, -8), stood.offset(8, 8, 8))) {
            if (helper.getLevel().getBlockEntity(at) instanceof LeftPackEntity pack && pack.getItem(0).is(Items.FLINT)) {
                left = true;
            }
        }
        check(helper, left, "no left pack holds what the survivor carried");
        Arrival.arrive(player);
        check(helper, !player.isSpectator() && body.ended == null && body.weightKg == Balance.get().body().arrivalKg(), "the new survivor did not arrive fresh");
        check(helper, player.getInventory().getItem(4).is(Things.PILOT_BREAD.get()) && player.getInventory().getItem(4).getCount() == Balance.get().kit().pilotBread(),
                "the kit has no " + Balance.get().kit().pilotBread() + " Pilot Bread");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCalendarStartsOn3May(GameTestHelper helper) {
        check(helper, Calendar.dateText(0).equals("3 May"), "day 1 is " + Calendar.dateText(0));
        check(helper, Calendar.dateText(8).equals("2 Jul"), "day 9 is " + Calendar.dateText(8));
        check(helper, Calendar.dateText(30).equals("17 Dec"), "day 31 is " + Calendar.dateText(30));
        check(helper, Calendar.dayOfYear(48) == 1 && Calendar.year(48) == 2, "day 49 is not day 1 of year 2");
        ServerLevel level = helper.getLevel();
        int day = Calendar.day(level);
        SolarTerm term = EclipticSeasonsApi.getInstance().getSolarTerm(level);
        int expected = (day + Calendar.firstSolarDay()) / Balance.get().year().termDays();
        check(helper, term.ordinal() == expected % 24, "solar day " + (day + Calendar.firstSolarDay()) + " is " + term);
        check(helper, EclipticSeasonsApi.getInstance().getLastingDaysOfEachTerm(level) == Balance.get().year().termDays(),
                "Ecliptic's terms last " + EclipticSeasonsApi.getInstance().getLastingDaysOfEachTerm(level) + " days");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theYearWaitsForAQuietMoment(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        body.lastHurt = helper.getLevel().getGameTime();
        check(helper, !Team.quiet(player, body), "a survivor hurt this moment counted as quiet");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCrateHandsOutOneMessenger(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.getInventory().clearContent();
        BlockPos crate = new BlockPos(3, 1, 3);
        helper.setBlock(crate, Things.SUPPLY_CRATE.get());
        BlockPos at = helper.absolutePos(crate);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
        int messengers = player.getInventory().countItem(Things.SATELLITE_MESSENGER.get());
        check(helper, messengers == 1, "the crate handed out " + messengers + " messengers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBedDoesNotMoveTheArrival(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.setRespawnPosition(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 1)), 0, false, true);
        check(helper, player.getRespawnPosition() == null, "a bed set where the survivor arrives");
        helper.succeed();
    }
}
