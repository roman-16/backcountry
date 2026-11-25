package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.guide.Task;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class GuideTests {
    private static final int JUNE = 6;
    private static final int DECEMBER = 12;
    private static final int APRIL = 4;

    private GuideTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static void finishFirstDays(ServerPlayer player) {
        Task.Stage.FIRST_DAYS.tasks().forEach(task -> Guide.done(player, task));
    }

    @GameTest(template = "empty")
    public static void aNewSurvivorStartsWithTheFirstDays(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        GuideView view = Guide.get(helper.getLevel().getServer()).view(player.getUUID(), JUNE);
        check(helper, view.stage() == Task.Stage.FIRST_DAYS, "a new survivor is at " + view.stage());
        check(helper, view.tasks().equals(Task.Stage.FIRST_DAYS.tasks()) && view.done().isEmpty(), "a new survivor sees " + view);
        Guide.get(helper.getLevel().getServer()).notice(player);
        check(helper, Metabolism.body(player).alerts.containsKey(Alert.NEW_TASKS), "the first days raised no New tasks alert");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tasksTickWhenTheSurvivorDoesThem(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Metabolism.drink(player, 0.25, Water.of(true, 0.25F));
        ItemStack fish = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riverfishing", "northern_pike")));
        CompoundTag data = new CompoundTag();
        data.putString("Species", "riverfishing:northern_pike");
        data.putInt("WeightG", 2048);
        fish.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        Fish.land(player, fish);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, new ItemStack(Things.SOFTWOOD.get()), new SimpleContainer(1)));
        GuideView view = Guide.get(helper.getLevel().getServer()).view(player.getUUID(), JUNE);
        check(helper, view.done().equals(List.of(Task.BOIL_WATER, Task.SPLIT_LOG, Task.LAND_FISH)), "drinking boiled water, landing a fish and splitting a log ticked " + view.done());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ticksStayWithThePlayerThroughANewSurvivor(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Guide.done(player, Task.LIGHT_FIRE);
        int day = Calendar.day(helper.getLevel());
        Metabolism.body(player).reset(day, Calendar.year(day));
        check(helper, Guide.get(helper.getLevel().getServer()).view(player.getUUID(), JUNE).done(Task.LIGHT_FIRE), "a new survivor lost the player's Light a fire");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theTeamsGoalsFollowTheYear(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        finishFirstDays(player);
        Guide guide = Guide.get(helper.getLevel().getServer());
        GuideView june = guide.view(player.getUUID(), JUNE);
        boolean ready = Task.Stage.BEFORE_SNOW.tasks().stream().allMatch(guide::reached);
        check(helper, ready ? june.stage() == Task.Stage.WINTER : june.stage() == Task.Stage.BEFORE_SNOW && june.tasks().equals(Task.Stage.BEFORE_SNOW.tasks()),
                "in June the team sees " + june);
        GuideView december = guide.view(player.getUUID(), DECEMBER);
        check(helper, december.stage() == Task.Stage.WINTER && december.tasks().getLast() == Task.REACH_APRIL, "in December the team sees " + december);
        GuideView april = guide.view(player.getUUID(), APRIL);
        check(helper, april.stage() == Task.Stage.YEAR && april.tasks().equals(List.of(Task.REACH_MAY)), "in April the team sees " + april);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryFirewoodAndHideClothingReachTheirGoals(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        var server = helper.getLevel().getServer();
        BlockPos pile = helper.absolutePos(new BlockPos(1, 1, 1));
        Guide.woodpile(server, pile, (int) Balance.get().tasks().dryFirewood());
        for (var item : List.of(Things.HIDE_HAT.get(), Things.HIDE_COAT.get(), Things.SNOWSHOES.get())) {
            NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, new ItemStack(item), new SimpleContainer(1)));
        }
        Guide guide = Guide.get(server);
        guide.check(server);
        Guide.woodpileGone(server, pile);
        check(helper, guide.reached(Task.DRY_FIREWOOD), "16 pieces of dry firewood on a woodpile did not reach the goal");
        check(helper, guide.reached(Task.HIDE_CLOTHING), "a hide hat, coat and snowshoes made did not reach the goal");
        helper.succeed();
    }
}
