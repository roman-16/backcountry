package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.DeepSnow;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.hunt.CarcassBlock;
import io.github.roman16.backcountry.hunt.CarcassEntity;
import io.github.roman16.backcountry.hunt.CarcassItem;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.hunt.Track;
import io.github.roman16.backcountry.hunt.TrackBlock;
import io.github.roman16.backcountry.hunt.TrackEntity;
import io.github.roman16.backcountry.hunt.Tracks;
import io.github.roman16.backcountry.hunt.Wolves;
import io.github.roman16.backcountry.hunt.Wound;
import io.github.roman16.backcountry.mixin.TargetTypeAccessor;
import io.github.roman16.backcountry.weather.Day;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class HuntTests {
    static final ResourceLocation MOOSE = ResourceLocation.fromNamespaceAndPath("alexsmobs", "moose");
    private static final int ANIMALS = 16;
    private static final float ARROW_DAMAGE = 2;
    private static final int SCAVENGE_STEP = 40;

    private HuntTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static LivingEntity moose(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        if (!(BuiltInRegistries.ENTITY_TYPE.get(MOOSE).spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), MobSpawnType.EVENT) instanceof Mob moose)) {
            throw new IllegalStateException("no moose");
        }
        moose.setNoAi(true);
        return moose;
    }

    private static void shoot(GameTestHelper helper, ServerPlayer hunter, LivingEntity animal, double heightShare) {
        Arrow arrow = new Arrow(helper.getLevel(), hunter, new ItemStack(Things.BROADHEAD_ARROW.get()), new ItemStack(Things.LIGHT_HUNTING_BOW.get()));
        arrow.setPos(animal.getX() - 1, animal.getY() + animal.getBbHeight() * heightShare, animal.getZ());
        animal.hurt(helper.getLevel().damageSources().arrow(arrow, hunter), ARROW_DAMAGE);
    }

    private static Optional<CarcassEntity> carcass(GameTestHelper helper) {
        for (BlockPos near : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(6, 4, 6)))) {
            if (helper.getLevel().getBlockEntity(near) instanceof CarcassEntity carcass) {
                return Optional.of(carcass);
            }
        }
        return Optional.empty();
    }

    @GameTest(template = "empty")
    public static void theDirectorKnowsItsAnimals(GameTestHelper helper) {
        check(helper, BuiltInRegistries.ENTITY_TYPE.getDataMap(Things.ANIMAL).size() >= ANIMALS, "the director knows too few animals");
        Animal moose = Animal.of(BuiltInRegistries.ENTITY_TYPE.get(MOOSE)).orElseThrow();
        check(helper, moose.kg() > Balance.get().hunting().organKg() && !moose.cuts().isEmpty(), "the moose has no weight or cuts");
        check(helper, Animal.of(EntityType.ZOMBIE).isEmpty(), "zombies are on the list");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aChestShotKillsAMoose(GameTestHelper helper) {
        LivingEntity moose = moose(helper);
        ServerPlayer hunter = BackcountryTests.survivor(helper);
        shoot(helper, hunter, moose, 0.6);
        helper.runAfterDelay(2, () -> {
            check(helper, !moose.isAlive(), "a chest shot left the moose alive");
            check(helper, carcass(helper).isPresent(), "the moose left no carcass");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aLowShotWoundsAndBleedsOut(GameTestHelper helper) {
        LivingEntity moose = moose(helper);
        ServerPlayer hunter = BackcountryTests.survivor(helper);
        shoot(helper, hunter, moose, 0.1);
        check(helper, moose.isAlive() && moose.getData(Things.WOUND).bleeding(), "a low shot did not wound the moose");
        moose.setData(Things.WOUND, new Wound(helper.getLevel().getGameTime(), Optional.of(hunter.getUUID()), List.of()));
        helper.runAfterDelay(40, () -> {
            check(helper, !moose.isAlive(), "the wounded moose never bled out");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aCarcassIsDressedWithAKnife(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Things.CARCASS.get().defaultBlockState());
        BlockPos at = helper.absolutePos(pos);
        CarcassEntity carcass = (CarcassEntity) helper.getLevel().getBlockEntity(at);
        carcass.hold(BuiltInRegistries.ENTITY_TYPE.get(MOOSE), List.of(new ItemStack(Things.BROADHEAD_ARROW.get())), 0);
        ServerPlayer player = BackcountryTests.survivor(helper);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        ItemStack stick = new ItemStack(Items.STICK);
        helper.getLevel().getBlockState(at).useItemOn(stick, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        check(helper, helper.getBlockState(pos).is(Things.CARCASS.get()), "a stick dressed the carcass");
        ItemStack knife = new ItemStack(Things.KNIFE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, knife);
        int clicks = CarcassBlock.clicks(carcass, Metabolism.body(player));
        for (int click = 0; click < clicks; click++) {
            helper.getLevel().getBlockState(at).useItemOn(knife, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        }
        check(helper, helper.getBlockState(pos).isAir(), "the carcass outlasted " + clicks + " clicks of the knife");
        List<ItemEntity> cuts = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        check(helper, cuts.stream().anyMatch(item -> BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath().equals("moose_ribs")), "the moose gave no meat");
        check(helper, cuts.stream().anyMatch(item -> item.getItem().is(Things.HEART.get())), "the moose gave no heart");
        check(helper, cuts.stream().anyMatch(item -> item.getItem().is(Things.BROADHEAD_ARROW.get())), "the embedded arrow stayed in");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSmallCarcassIsCarried(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Things.CARCASS.get().defaultBlockState());
        BlockPos at = helper.absolutePos(pos);
        ((CarcassEntity) helper.getLevel().getBlockEntity(at)).hold(EntityType.RABBIT, List.of(), 0);
        ServerPlayer player = BackcountryTests.survivor(helper);
        player.setShiftKeyDown(true);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        ItemStack carried = player.getMainHandItem();
        check(helper, carried.is(Things.CARCASS_ITEM.get()) && CarcassItem.animal(carried) == EntityType.RABBIT, "the hare was not carried: " + carried);
        check(helper, helper.getBlockState(pos).isAir(), "the carried carcass stayed on the ground");
        BlockPos ground = at.east(2);
        helper.getLevel().setBlockAndUpdate(ground.below(), Blocks.STONE.defaultBlockState());
        carried.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground.below()), Direction.UP, ground.below(), false)));
        check(helper, helper.getLevel().getBlockState(ground).is(Things.CARCASS.get()), "the carcass was not set down");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strengthFollowsWeightAndEnergy(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.energy = 1;
        check(helper, Hunting.strength(player) >= 1, "a rested survivor of BMI " + body.bmi() + " was weak");
        body.energy = 0;
        check(helper, Hunting.strength(player) <= 0, "an exhausted survivor had strength");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void peltsBecomeHides(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingInput scraping = CraftingInput.of(2, 1, List.of(new ItemStack(Things.PELT.get()), new ItemStack(Things.KNIFE.get())));
        ItemStack scraped = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, scraping, level).orElseThrow().value().assemble(scraping, level.registryAccess());
        check(helper, scraped.is(Things.SCRAPED_HIDE.get()), "a scraped pelt became " + scraped);
        CraftingInput curing = CraftingInput.of(2, 1, List.of(scraped, new ItemStack(Things.RENDERED_FAT.get())));
        ItemStack cured = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, curing, level).orElseThrow().value().assemble(curing, level.registryAccess());
        check(helper, cured.is(Things.CURED_HIDE.get()), "a cured hide came out as " + cured);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deepSnowNeedsSnowshoes(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.SNOW_BLOCK);
        check(helper, DeepSnow.deep(helper.getLevel(), helper.absolutePos(pos)), "a snow block underfoot was not deep snow");
        helper.setBlock(pos.below(), Blocks.STONE);
        check(helper, !DeepSnow.deep(helper.getLevel(), helper.absolutePos(pos)), "bare stone was deep snow");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wolvesNeverHuntSurvivors(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        if (!(BuiltInRegistries.ENTITY_TYPE.get(Wolves.TIMBER_WOLF).spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), MobSpawnType.EVENT) instanceof Mob wolf)) {
            throw new IllegalStateException("no timber wolf");
        }
        boolean hunts = wolf.targetSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> hunt
                && Player.class.isAssignableFrom(((TargetTypeAccessor) hunt).backcountry$targetType()));
        boolean keepsOff = wolf.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof AvoidEntityGoal<?>);
        wolf.discard();
        check(helper, !hunts && keepsOff, "a timber wolf hunts survivors (" + hunts + ") or keeps no distance (" + !keepsOff + ")");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bigGameLeavesSignThatFades(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.MUD);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(2, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 4), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3));
        long now = level.getDayTime();
        Tracks.mark(level, helper.absolutePos(new BlockPos(2, 1, 2)), Track.MOOSE, Direction.NORTH, now, false);
        Tracks.mark(level, helper.absolutePos(new BlockPos(4, 1, 2)), Track.MOOSE, Direction.NORTH, now, false);
        Tracks.mark(level, helper.absolutePos(new BlockPos(2, 1, 4)), Track.HARE, Direction.NORTH, now, false);
        check(helper, helper.getBlockState(new BlockPos(2, 1, 2)).is(Things.TRACKS.get()), "a moose left no print in mud");
        check(helper, helper.getBlockState(new BlockPos(4, 1, 2)).isAir(), "a moose printed on grass");
        BlockState hare = helper.getBlockState(new BlockPos(2, 1, 4));
        check(helper, hare.is(Things.TRACKS.get()) && hare.getValue(TrackBlock.SNOW) == 3, "a hare's print in snow is " + hare);
        Tracks.mark(level, helper.absolutePos(new BlockPos(2, 1, 2)), Track.MOOSE, Direction.NORTH, now, true);
        check(helper, helper.getBlockState(new BlockPos(2, 1, 2)).is(Things.DROPPINGS.get()), "the moose dropped nothing");
        if (!(level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 4))) instanceof TrackEntity print)) {
            throw new IllegalStateException("the print has no block entity");
        }
        check(helper, Tracks.Age.of(print.age()) == Tracks.Age.FRESH, "a print just laid reads " + Tracks.Age.of(print.age()));
        print.laid(now - (long) (Balance.get().tracking().signDays() * 24000));
        Tracks.weather(level, helper.absolutePos(new BlockPos(2, 1, 4)), hare);
        BlockState after = helper.getBlockState(new BlockPos(2, 1, 4));
        check(helper, after.is(Blocks.SNOW) && after.getValue(SnowLayerBlock.LAYERS) == 3, "a faded print in snow left " + after);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scentCarriesDownwind(GameTestHelper helper) {
        Day northerly = new Day(1, 0, 0, 20, 0, Day.DRY, 0, 0, false);
        Day calm = new Day(1, 0, 0, 0, 0, Day.DRY, 0, 0, false);
        Vec3 survivor = Vec3.ZERO;
        check(helper, Tracks.carries(northerly, survivor, new Vec3(0, 0, 48)), "a moose downwind did not smell the survivor");
        check(helper, !Tracks.carries(northerly, survivor, new Vec3(0, 0, -48)), "a moose upwind smelled the survivor");
        check(helper, !Tracks.carries(northerly, survivor, new Vec3(0, 0, 96)), "the scent carried past its reach");
        check(helper, Tracks.carries(calm, survivor, new Vec3(8, 0, 0)) && !Tracks.carries(calm, survivor, new Vec3(32, 0, 0)), "calm air carried the scent wrong");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scavengersTakeAnUndressedCarcass(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Things.CARCASS.get().defaultBlockState());
        CarcassEntity carcass = (CarcassEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        carcass.hold(EntityType.RABBIT, List.of(), 0);
        check(helper, carcass.ticksLeft() > 0, "a fresh carcass was already scavenged");
        carcass.hold(EntityType.RABBIT, List.of(), (long) (Balance.get().hunting().scavengedDays() * 24000));
        ItemStack carried = carcass.item();
        check(helper, carried.getOrDefault(Things.LAIN.get(), 0L) >= (long) (Balance.get().hunting().scavengedDays() * 24000), "a carried carcass forgot how long it lay");
        helper.runAfterDelay(SCAVENGE_STEP, () -> {
            check(helper, helper.getBlockState(pos).isAir(), "an old carcass was never scavenged");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aWolfGivesItsShareOfMeat(GameTestHelper helper) {
        Animal wolf = Animal.of(BuiltInRegistries.ENTITY_TYPE.get(Wolves.TIMBER_WOLF)).orElseThrow();
        int pieces = wolf.cuts().stream().filter(cut -> cut.item() == BuiltInRegistries.ITEM.get(ResourceLocation.parse("cnc:raw_game"))).mapToInt(Animal.Cut::count).sum();
        check(helper, pieces == 12, "a timber wolf gives " + pieces + " pieces of Raw Game");
        helper.succeed();
    }
}
