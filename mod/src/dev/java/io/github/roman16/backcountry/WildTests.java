package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Burn;
import io.github.roman16.backcountry.body.Exertion;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.haul.Sled;
import io.github.roman16.backcountry.hunt.Bears;
import io.github.roman16.backcountry.hunt.CarcassEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class WildTests {
    private static final int STRIDES = 1000;
    private static final double STRIDE_BLOCKS = 0.2;

    private WildTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static Mob bear(GameTestHelper helper, ResourceLocation id, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return (Mob) helper.spawn(type, pos);
    }

    @GameTest(template = "empty")
    public static void bearsTakeThePacksTemper(GameTestHelper helper) {
        Mob grizzly = bear(helper, Bears.GRIZZLY, new BlockPos(2, 1, 2));
        boolean attacksAnyone = grizzly.targetSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).anyMatch(goal -> goal.getClass().getSimpleName().equals("AttackPlayerGoal"));
        boolean tempered = grizzly.targetSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).anyMatch(goal -> goal.getClass().getSimpleName().equals("Temper"));
        check(helper, !attacksAnyone && tempered, "the grizzly kept Alex's Mobs' 5-block attack or lacks the pack's temper");
        Mob black = bear(helper, Bears.BLACK_BEAR, new BlockPos(6, 1, 6));
        check(helper, black.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).anyMatch(goal -> goal instanceof AvoidEntityGoal<?>), "the black bear does not flee");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSowChargesNearHerCub(GameTestHelper helper) {
        Mob sow = bear(helper, Bears.GRIZZLY, new BlockPos(2, 1, 2));
        Mob cub = bear(helper, Bears.GRIZZLY, new BlockPos(4, 1, 2));
        ((AgeableMob) cub).setAge(AgeableMob.BABY_START_AGE);
        ServerPlayer player = BackcountryTests.survivor(helper);
        Vec3 near = helper.absoluteVec(new Vec3(7.5, 1, 2.5));
        player.moveTo(near.x, near.y, near.z);
        check(helper, Bears.reason(sow, player).filter("cubs"::equals).isPresent(), "a survivor 3 blocks from a cub gave the sow no reason: " + Bears.reason(sow, player));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aPulledSledAddsItsLoadAndTakesTheMoose(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        Sled sled = helper.spawn(Things.SLED_ENTITY.get(), new BlockPos(3, 1, 1));
        sled.setLeashedTo(player, true);
        for (int slot = 0; slot < 8; slot++) {
            sled.setItem(slot, new ItemStack(Items.COBBLESTONE, 8));
        }
        double ground = Sled.pulledKg(player);
        check(helper, ground > 64, "a sled of 64 cobblestone pulled as " + ground + " kg");
        Vec3 from = player.position();
        Vec3 to = from.add(STRIDE_BLOCKS, 0, 0);
        for (int stride = 0; stride < STRIDES; stride++) {
            Exertion.stride(player, body, from, to);
        }
        body.burned[Burn.LOAD.ordinal()] = 0;
        Exertion.burn(player, body, 1, false, 0);
        check(helper, body.burned[Burn.LOAD.ordinal()] > 0, "pulling the sled 200 m burned nothing for the load");
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.SNOW.defaultBlockState());
        sled.moveTo(sled.getX(), sled.getY() + 0.2, sled.getZ());
        check(helper, Sled.pulledKg(player) < ground, "the sled pulled as heavy on snow as on ground");
        BlockPos at = new BlockPos(5, 1, 5);
        helper.setBlock(at, Things.CARCASS.get().defaultBlockState());
        var carcass = (CarcassEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
        carcass.hold(BuiltInRegistries.ENTITY_TYPE.get(HuntTests.MOOSE), List.of(), 0);
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(at);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        check(helper, helper.getBlockState(at).isAir(), "the moose did not go onto the sled");
        helper.succeed();
    }
}
