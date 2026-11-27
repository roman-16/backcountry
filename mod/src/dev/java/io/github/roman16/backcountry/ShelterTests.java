package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Afflictions;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Rooms;
import io.github.roman16.backcountry.body.Spirits;
import io.github.roman16.backcountry.body.Warmth;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.Spoilage;
import io.github.roman16.backcountry.gear.TreatmentItem;
import io.github.roman16.backcountry.survivor.Team;
import io.github.roman16.backcountry.weather.Ice;
import io.github.roman16.backcountry.weather.ThinIceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class ShelterTests {
    private static final int SIDE = 5;
    private static final int HEIGHT = 4;
    private static final int LIGHT_TICKS = 8;

    private ShelterTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static void hut(GameTestHelper helper) {
        hut(helper, Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    private static void hut(GameTestHelper helper, BlockState wall) {
        for (int x = 0; x < SIDE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIDE; z++) {
                    boolean shell = x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1 || y == 0 || y == HEIGHT - 1;
                    helper.setBlock(new BlockPos(x + 1, y, z + 1), shell ? wall : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static Rooms.Room room(GameTestHelper helper, BlockPos inside) {
        return Rooms.at(helper.getLevel(), helper.absolutePos(inside));
    }

    @GameTest(template = "empty")
    public static void aSealedHutIsACabin(GameTestHelper helper) {
        hut(helper);
        helper.runAfterDelay(LIGHT_TICKS, () -> {
            Rooms.Kind kind = room(helper, new BlockPos(3, 1, 3)).kind();
            check(helper, kind == Rooms.Kind.CABIN, "a sealed plank hut was " + kind);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aRoomInTheRockIsACellar(GameTestHelper helper) {
        hut(helper, Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(LIGHT_TICKS, () -> {
            Rooms.Kind kind = room(helper, new BlockPos(3, 1, 3)).kind();
            check(helper, kind == Rooms.Kind.CELLAR, "a room in the rock was " + kind);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aHoleInTheRoofOpensTheRoom(GameTestHelper helper) {
        hut(helper);
        helper.setBlock(new BlockPos(2, HEIGHT - 1, 2), Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(LIGHT_TICKS, () -> {
            check(helper, !room(helper, new BlockPos(3, 1, 3)).sealed(), "a hut with a hole in its roof was sealed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aFireWithoutASmokeHoleFillsTheRoomWithSmoke(GameTestHelper helper) {
        hut(helper);
        BlockPos fire = new BlockPos(3, 1, 3);
        helper.setBlock(fire, Things.CAMPFIRE.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(fire)) instanceof CampfireBlockEntity campfire)) {
            throw new IllegalStateException("the campfire has no block entity");
        }
        helper.runAfterDelay(LIGHT_TICKS, () -> {
            FireBlock.light(helper.getLevel(), helper.absolutePos(fire), helper.getBlockState(fire), campfire);
            Rooms.Room room = room(helper, new BlockPos(2, 1, 2));
            check(helper, room.sealed() && room.smoky(), "a fire in a sealed hut left it clear of smoke");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aSmokeHoleKeepsTheRoomSealedAndClear(GameTestHelper helper) {
        hut(helper);
        BlockPos fire = new BlockPos(3, 1, 3);
        helper.setBlock(fire, Things.CAMPFIRE.get().defaultBlockState());
        helper.setBlock(new BlockPos(3, HEIGHT - 1, 3), Blocks.AIR.defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(fire)) instanceof CampfireBlockEntity campfire)) {
            throw new IllegalStateException("the campfire has no block entity");
        }
        helper.runAfterDelay(LIGHT_TICKS, () -> {
            FireBlock.light(helper.getLevel(), helper.absolutePos(fire), helper.getBlockState(fire), campfire);
            Rooms.Room room = room(helper, new BlockPos(2, 1, 2));
            check(helper, room.sealed() && !room.smoky(), "a smoke hole over the fire left the room " + room.kind() + (room.smoky() ? ", smoky" : ""));
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void thinIceBreaksUnderALoad(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        BlockPos under = new BlockPos(1, 0, 1);
        helper.setBlock(under, Things.THIN_ICE.get().defaultBlockState().setValue(ThinIceBlock.THICKNESS, 1));
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.moveTo(at.x, at.y, at.z);
        player.setOnGround(true);
        Body body = Metabolism.body(player);
        body.loadKg = 0;
        Ice.step(player, body);
        check(helper, helper.getBlockState(under).is(Things.THIN_ICE.get()), "thin ice broke under an empty-handed survivor");
        body.loadKg = Balance.get().ice().thinLoadKg();
        Ice.step(player, body);
        check(helper, helper.getBlockState(under).is(Blocks.WATER), "thin ice held a survivor with a full load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void threeStrippedPolesMakeATallCache(GameTestHelper helper) {
        for (int y = 0; y < Balance.get().raids().tallPoles(); y++) {
            helper.setBlock(new BlockPos(3, y, 3), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        }
        BlockPos top = new BlockPos(3, Balance.get().raids().tallPoles(), 3);
        helper.setBlock(top, Things.FOOD_CACHE.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(top)) instanceof CacheEntity cache)) {
            throw new IllegalStateException("the food cache has no block entity");
        }
        cache.report();
        check(helper, cache.reach() == CacheEntity.Reach.TALL, "a cache on 3 stripped poles was " + cache.reach());
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.LADDER.defaultBlockState());
        check(helper, cache.reach() == CacheEntity.Reach.LADDER, "a ladder beside the poles let nothing climb");
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.SPRUCE_LOG.defaultBlockState());
        check(helper, cache.reach() == CacheEntity.Reach.GROUND, "a pole with its bark counted as stripped");
        Team.get(helper.getLevel().getServer()).emptied(helper.absolutePos(top));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void leanMeatStartsRabbitStarvation(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        Balance.Conditions rules = Balance.get().conditions();
        for (int day = 0; day < rules.rabbitOnsetDays(); day++) {
            body.kcalEatenToday = 2048;
            body.proteinToday = rules.rabbitProteinG() + 1;
            body.fatToday = 8;
            Afflictions.day(body);
        }
        check(helper, body.has(Condition.RABBIT_STARVATION), "lean days did not bring rabbit starvation");
        for (int day = 0; day < rules.rabbitCureDays(); day++) {
            body.kcalEatenToday = 2048;
            body.proteinToday = 64;
            body.fatToday = 128;
            Afflictions.day(body);
        }
        Afflictions.advance(player, body, 0, 0);
        check(helper, !body.has(Condition.RABBIT_STARVATION), "fatty days did not end rabbit starvation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saltedMeatKeepsLonger(GameTestHelper helper) {
        ItemStack beef = new ItemStack(Items.BEEF);
        ItemStack salted = new ItemStack(Items.BEEF);
        salted.set(Things.SALTED.get(), Unit.INSTANCE);
        double fresh = Spoilage.life(beef).orElseThrow();
        double kept = Spoilage.life(salted).orElseThrow();
        check(helper, Math.abs(kept - fresh * Balance.get().drying().saltedLife()) < 1e-3, "salted beef keeps " + kept + " days against " + fresh);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aFireLiftsSpirits(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        double start = Balance.get().spirits().comfortCeiling() / 2;
        body.spirits = start;
        body.fire = false;
        Spirits.step(player, body, 1, false);
        double without = body.spirits;
        body.spirits = start;
        body.fire = true;
        Spirits.step(player, body, 1, false);
        check(helper, Math.abs(body.spirits - without - Balance.get().spirits().firePerHour()) < 1e-6, "an hour by a fire lifted spirits by " + (body.spirits - without));
        check(helper, (body.spiritCauses & Spirits.Cause.FIRE.bit()) != 0, "the fire was not named among the causes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pitchSalveHealsAWoundWithinADay(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.afflict(Condition.INFECTED_WOUND);
        check(helper, Afflictions.treat(body, TreatmentItem.Treatment.SALVE), "pitch salve treated nothing");
        Afflictions.advance(player, body, 1, 24);
        check(helper, !body.has(Condition.INFECTED_WOUND), "the wound outlasted a day under pitch salve");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shellfishCanPoison(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        for (int meal = 0; meal < 512 && !body.has(Condition.SHELLFISH_POISONING); meal++) {
            body.stomachKcal = 0;
            Metabolism.eat(player, new ItemStack(Things.MUSSELS.get()));
        }
        check(helper, body.has(Condition.SHELLFISH_POISONING), "512 meals of mussels poisoned no one");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void comfortsHoldSpiritsAtTheMiddle(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        Balance.Spirits rules = Balance.get().spirits();
        body.spirits = rules.comfortCeiling() - 1e-3;
        body.fire = true;
        Spirits.step(player, body, 4, false);
        check(helper, body.spirits <= rules.comfortCeiling() + 1e-9, "a fire lifted spirits past the middle to " + body.spirits);
        body.spirits = 0.99;
        body.fire = true;
        Spirits.step(player, body, 24, false);
        check(helper, body.spirits <= 0.99 - rules.settlePerDay() + 1e-6, "a day by a fire left high spirits at " + body.spirits);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void illnessWearsSpiritsDown(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.spirits = Balance.get().spirits().comfortCeiling();
        body.fire = false;
        Spirits.step(player, body, 1, false);
        double well = body.spirits;
        body.spirits = Balance.get().spirits().comfortCeiling();
        body.afflict(Condition.FOOD_POISONING);
        Spirits.step(player, body, 1, false);
        check(helper, Math.abs(well - body.spirits - Balance.get().spirits().illPerHour()) < 1e-6, "an hour of food poisoning took " + (well - body.spirits));
        check(helper, (body.spiritCauses & Spirits.Cause.ILL.bit()) != 0, "illness was not named among the causes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tooColdToSleepWithoutTheBag(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.wetness = 0;
        body.effectiveC = -10;
        check(helper, Warmth.tooColdToSleep(player, body), "a survivor without a bag slept at -10 °C");
        player.getInventory().add(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("sleeping_bags", "brown_sleeping_bag")).getDefaultInstance());
        check(helper, !Warmth.tooColdToSleep(player, body), "the Sleeping Bag did not keep out -10 °C");
        body.effectiveC = -40;
        check(helper, Warmth.tooColdToSleep(player, body), "the Sleeping Bag kept out -40 °C");
        helper.succeed();
    }
}
