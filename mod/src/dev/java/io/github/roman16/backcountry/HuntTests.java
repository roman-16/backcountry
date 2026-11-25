package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.DeepSnow;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.hunt.CarcassBlock;
import io.github.roman16.backcountry.hunt.CarcassEntity;
import io.github.roman16.backcountry.hunt.CarcassItem;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.hunt.Wound;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
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
        carcass.hold(BuiltInRegistries.ENTITY_TYPE.get(MOOSE), List.of(new ItemStack(Things.BROADHEAD_ARROW.get())));
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
        ((CarcassEntity) helper.getLevel().getBlockEntity(at)).hold(EntityType.RABBIT, List.of());
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
}
