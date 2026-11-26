package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Burn;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Exertion;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class BodyTests {
    private static final double KCAL_PER_WATT_HOUR = 0.8598;
    private static final double STRIDE_BLOCKS = 0.2;
    private static final int STRIDES_A_KM = 5000;
    private static final double METRES_A_KM = 1000;
    private static final double SECONDS_AN_HOUR = 3600;
    private static final double WALK_HOURS = 1;
    private static final double CLOSE = 1;
    private static final int DEEP_LAYERS = 4;
    private static final double UPHILL_BLOCKS_A_STRIDE = 0.1;
    private static final double CARRIED_KG = 20;
    private static final double EATEN = 1024;

    private BodyTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static double walkKilometre(ServerPlayer player, Body body, double risePerStride) {
        Vec3 from = player.position();
        Vec3 to = from.add(STRIDE_BLOCKS, risePerStride, 0);
        for (int stride = 0; stride < STRIDES_A_KM; stride++) {
            Exertion.stride(player, body, from, to);
        }
        body.burned[Burn.MOVING.ordinal()] = 0;
        body.burned[Burn.LOAD.ordinal()] = 0;
        Exertion.burn(player, body, WALK_HOURS, false, 0);
        return body.burned[Burn.MOVING.ordinal()];
    }

    private static double flatKilometre(Body body) {
        Balance.Walking walking = Balance.get().walking();
        double hours = METRES_A_KM / walking.walkMS() / SECONDS_AN_HOUR;
        return KCAL_PER_WATT_HOUR * walking.motionWPerKg() * walking.walkMS() * walking.walkMS() * hours * body.weightKg;
    }

    @GameTest(template = "empty")
    public static void aKilometreWalkedCostsWhatItCostsAWalker(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        double flat = walkKilometre(player, body, 0);
        check(helper, Math.abs(flat - flatKilometre(body)) < CLOSE, "a kilometre walked burned " + flat + " kcal above standing, not " + flatKilometre(body));
        double uphill = walkKilometre(player, body, UPHILL_BLOCKS_A_STRIDE);
        check(helper, uphill > 2 * flat, "a kilometre uphill burned " + uphill + " kcal against " + flat + " on the flat");
        Vec3 from = player.position();
        body.metresToday = 0;
        Exertion.stride(player, body, from, from.add(METRES_A_KM, 0, 0));
        check(helper, body.metresToday <= Balance.get().walking().sprintBlocksPerTick() + 1e-9, "a teleport counted " + body.metresToday + " m walked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deepSnowCostsMoreAndSnowshoesLess(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, DEEP_LAYERS));
        double flat = flatKilometre(body);
        double snow = walkKilometre(player, body, 0);
        check(helper, snow > 4 * flat, "50 cm of snow burned " + snow + " kcal against " + flat + " on bare ground");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Things.SNOWSHOES.get()));
        double shoes = walkKilometre(player, body, 0);
        check(helper, shoes < snow / 2 && shoes > flat, "snowshoes in 50 cm of snow burned " + shoes + " kcal against " + snow + " without them");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loadAndDigestionBurnOnTop(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        body.loadKg = CARRIED_KG;
        walkKilometre(player, body, 0);
        double carried = body.burned[Burn.LOAD.ordinal()];
        double expected = flatKilometre(body) * CARRIED_KG / body.weightKg;
        check(helper, carried > expected, "carrying 20 kg for a kilometre burned " + carried + " kcal for the load, under " + expected);
        body.burned[Burn.DIGESTION.ordinal()] = 0;
        Exertion.burn(player, body, 0, false, EATEN);
        double digestion = body.burned[Burn.DIGESTION.ordinal()];
        check(helper, Math.abs(digestion - EATEN * Balance.get().body().thermicShare()) < CLOSE, "digesting 1,024 kcal burned " + digestion);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void practiceRaisesASkillThatDiesWithTheSurvivor(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Body body = Metabolism.body(player);
        int first = Balance.get().skills().usesToLevel().getFirst().intValue();
        Skill.WOODCUTTING.use(player, first);
        check(helper, Skill.WOODCUTTING.level(body) == 2, first + " logs left Woodcutting at " + Skill.WOODCUTTING.level(body));
        check(helper, Math.abs(Skill.WOODCUTTING.effort(body) - (1 - Balance.get().skills().levelShare())) < 1e-9, "Woodcutting 2 works at " + Skill.WOODCUTTING.effort(body));
        int day = Calendar.day(helper.getLevel());
        body.reset(day, Calendar.year(day));
        check(helper, Skill.WOODCUTTING.level(body) == 1, "a new survivor chops at Woodcutting " + Skill.WOODCUTTING.level(body));
        helper.succeed();
    }
}
