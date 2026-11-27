package io.github.roman16.backcountry.body;

import com.mojang.datafixers.util.Either;
import io.github.roman16.backcountry.Backcountry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Sleep {
    private static final String BAGS = "sleeping_bags";

    private Sleep() {}

    public static boolean bag(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(BAGS);
    }

    public static void lieDown(ServerPlayer player) {
        if (player.isSleeping() || !player.isAlive()) {
            return;
        }
        if (!player.onGround()) {
            player.displayClientMessage(Component.translatable("message.backcountry.sleep_on_ground"), true);
            return;
        }
        Level level = player.level();
        Either<Player.BedSleepingProblem, Unit> vanilla = !level.dimensionType().natural() ? Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE)
                : level.isDay() ? Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW) : Either.right(Unit.INSTANCE);
        Either<Player.BedSleepingProblem, Unit> result = EventHooks.canPlayerStartSleeping(player, player.blockPosition(), vanilla);
        if (result.left().isPresent()) {
            Component message = result.left().get().getMessage();
            if (message != null) {
                player.displayClientMessage(message, true);
            }
            return;
        }
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.setPose(Pose.SLEEPING);
        player.setSleepingPos(player.blockPosition());
        player.setDeltaMovement(Vec3.ZERO);
        player.awardStat(Stats.SLEEP_IN_BED);
        CriteriaTriggers.SLEPT_IN_BED.trigger(player);
        ((ServerLevel) level).updateSleepingPlayerList();
    }

    @SubscribeEvent
    static void ground(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()
                || event.getFace() != Direction.UP || event.getLevel().getBlockEntity(event.getPos()) != null
                || !event.getLevel().getBlockState(event.getPos()).isFaceSturdy(event.getLevel(), event.getPos(), Direction.UP)) {
            return;
        }
        if (player instanceof ServerPlayer survivor) {
            lieDown(survivor);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    @SubscribeEvent
    static void tooCold(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (event.getProblem() != null || !Metabolism.lives(player)) {
            return;
        }
        if (Warmth.tooColdToSleep(player, Metabolism.body(player))) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(Component.translatable("message.backcountry.too_cold_to_sleep"), true);
        }
    }

    @SubscribeEvent
    static void groundIsBed(CanContinueSleepingEvent event) {
        if (event.getEntity() instanceof Player && event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_HERE) {
            event.setContinueSleeping(true);
        }
    }
}
