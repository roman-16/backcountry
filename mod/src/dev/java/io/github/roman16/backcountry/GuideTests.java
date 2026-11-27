package io.github.roman16.backcountry;

import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.food.CacheBlock;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.guide.Task;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class GuideTests {
    private static final int MAY = 5;
    private static final int JUNE = 6;

    private GuideTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static GuideView view(GameTestHelper helper, ServerPlayer player) {
        return Guide.get(helper.getLevel().getServer()).view(player);
    }

    @GameTest(template = "empty")
    public static void aNewSurvivorFollowsTheFirstTask(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        GuideView view = view(helper, player);
        check(helper, Task.values()[0] == Task.AXE && !view.done(Task.AXE), "a new survivor has made a stone axe");
        check(helper, view.following(JUNE).equals(Optional.of(Task.AXE)), "a new survivor follows " + view.following(JUNE));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aTasksLastStepDoneTicksTheWholeTask(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Metabolism.drink(player, 0.25, Water.boiled(0.25F));
        ItemStack fish = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riverfishing", "northern_pike")));
        CompoundTag data = new CompoundTag();
        data.putString("Species", "riverfishing:northern_pike");
        data.putInt("WeightG", 2048);
        fish.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        Fish.land(player, fish);
        check(helper, !view(helper, player).done(Task.BOIL_WATER), "the kit's boiled water finished Boil water without a pot over a fire");
        check(helper, view(helper, player).stepsDone(Task.FILL_BOTTLE) == 1, "drinking did not tick its step of the Water Bottle");
        Guide.did(player, Deed.FILLED_POT);
        Guide.did(player, Deed.HUNG_POT);
        Metabolism.drink(player, 0.25, Water.boiled(0.25F));
        GuideView view = view(helper, player);
        check(helper, view.done(Task.BOIL_WATER), "drinking water boiled in the hung pot did not finish Boil water");
        check(helper, view.done(Task.LAND_FISH), "landing a fish did not finish Land a fish");
        check(helper, !view.done(Task.LIGHT_FIRE), "Light a fire is done without a fire");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void whatTheSurvivorHoldsTicksItsStep(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        player.getInventory().setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("projectvibrantjourneys", "rocks"))));
        Guide.sense(player);
        GuideView view = view(helper, player);
        check(helper, view.stepsDone(Task.AXE) == 1 && !view.done(Task.AXE), "a rock held ticked " + view.stepsDone(Task.AXE) + " steps of the stone axe");
        player.getInventory().setItem(1, new ItemStack(Items.STONE_AXE));
        Guide.sense(player);
        check(helper, view(helper, player).done(Task.AXE), "a stone axe held did not finish Make a stone axe");
        player.getInventory().clearContent();
        Guide.sense(player);
        check(helper, view(helper, player).done(Task.AXE), "Make a stone axe came undone when the axe was gone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ticksStayWithThePlayerThroughANewSurvivor(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        Guide.did(player, Deed.LIT_FIRE);
        int day = Calendar.day(helper.getLevel());
        Metabolism.body(player).reset(day, Calendar.year(day));
        GuideView view = view(helper, player);
        check(helper, view.stepsDone(Task.LIGHT_FIRE) == 1, "a new survivor lost the player's lit fire");
        check(helper, view.following(JUNE).equals(Optional.of(Task.AXE)), "a half-lit fire jumped the queue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theTrackerPassesOverWhatIsOutOfSeason(GameTestHelper helper) {
        List<Integer> steps = Arrays.stream(Task.values()).map(task -> task.ordinal() < Task.EAT_BERRIES.ordinal() ? task.full() : 0).toList();
        GuideView view = new GuideView(steps, 0, 0, Optional.empty());
        check(helper, view.following(JUNE).equals(Optional.of(Task.EAT_BERRIES)), "in June the tracker follows " + view.following(JUNE));
        check(helper, view.following(MAY).equals(Optional.of(Task.SPRUCE_TEA)), "in May the tracker follows " + view.following(MAY));
        GuideView tracked = new GuideView(steps, 0, 0, Optional.of(Task.SMOKE_MEAT));
        check(helper, tracked.following(JUNE).equals(Optional.of(Task.SMOKE_MEAT)), "the tracked task was not followed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryFirewoodAndHideClothingReachTheirGoals(GameTestHelper helper) {
        ServerPlayer player = BackcountryTests.survivor(helper);
        MinecraftServer server = helper.getLevel().getServer();
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
        check(helper, view(helper, player).done(Task.HIDE_CLOTHING), "the team's clothing goal does not show done");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aCacheBesideTheFireIsInCamp(GameTestHelper helper) {
        FireTests.campfire(helper, new BlockPos(1, 1, 1));
        check(helper, CacheBlock.inCamp(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 4))), "a cache 3 blocks from the campfire stood out of camp");
        helper.succeed();
    }
}
