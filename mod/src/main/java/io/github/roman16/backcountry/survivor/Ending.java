package io.github.roman16.backcountry.survivor;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.LeftPackEntity;
import io.github.roman16.backcountry.gear.Worn;
import io.github.roman16.backcountry.net.Messages;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Ending {
    private static final int SEARCH_BLOCKS = 8;

    private Ending() {}

    public static void extract(ServerPlayer player) {
        String bmi = String.format(Locale.ROOT, "%.1f", Metabolism.body(player).bmi());
        leave(player, Summary.Kind.EXTRACTED, Component.translatable("message.backcountry.extracted", player.getDisplayName(), bmi));
    }

    public static void tapOut(ServerPlayer player) {
        Body body = Metabolism.body(player);
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (!body.arrived || body.ended != null || !player.isAlive() || mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
            return;
        }
        int day = Calendar.dayOfYear(Calendar.day(player.level()));
        leave(player, Summary.Kind.TAPPED_OUT, Component.translatable("message.backcountry.tapped_out", player.getDisplayName(), day));
    }

    private static void leave(ServerPlayer player, Summary.Kind kind, Component told) {
        Body body = Metabolism.body(player);
        body.gameModeBeforeEnd = player.gameMode.getGameModeForPlayer().getId();
        Summary summary = end(player, kind);
        player.setGameMode(GameType.SPECTATOR);
        player.server.getPlayerList().broadcastSystemMessage(told, false);
        Messages.summary(player, summary);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void died(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Body body = Metabolism.body(player);
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (body.arrived && body.ended == null && (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)) {
            Messages.summary(player, end(player, Summary.Kind.DIED));
        }
    }

    private static Summary end(ServerPlayer player, Summary.Kind kind) {
        Body body = Metabolism.body(player);
        int day = Calendar.day(player.level());
        List<Summary.Taken> taken = new ArrayList<>();
        body.animals.forEach((entity, count) -> taken.add(new Summary.Taken(entity, count)));
        taken.sort(Comparator.comparingInt(Summary.Taken::count).reversed());
        List<Float> weights = new ArrayList<>(body.weights);
        weights.add((float) body.weightKg);
        Summary summary = new Summary(kind, day, body.bmi(), body.weightKg, body.arrivedKg, day - body.arrivedDay + 1, body.kcalEaten, List.copyOf(taken), List.copyOf(weights),
                List.of());
        leavePack(player, body);
        body.ended = summary;
        Team.get(player.server).ended(player, body, kind, day);
        Metabolism.release(player);
        return summary;
    }

    private static void leavePack(ServerPlayer player, Body body) {
        List<ItemStack> carried = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                carried.add(stack);
            }
        }
        for (Worn worn : Worn.values()) {
            ItemStack taken = worn.take(player);
            if (!taken.isEmpty()) {
                carried.add(taken);
            }
        }
        if (carried.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos spot = standing(level, player.blockPosition()).orElseGet(() -> floor(level, player.blockPosition()));
        level.setBlockAndUpdate(spot, Things.LEFT_PACK.get().defaultBlockState());
        if (level.getBlockEntity(spot) instanceof LeftPackEntity pack) {
            pack.fill(carried, Component.translatable("container.backcountry.left_pack", player.getDisplayName()));
        }
    }

    private static Optional<BlockPos> standing(ServerLevel level, BlockPos from) {
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos at : BlockPos.betweenClosed(from.offset(-SEARCH_BLOCKS, -SEARCH_BLOCKS, -SEARCH_BLOCKS), from.offset(SEARCH_BLOCKS, SEARCH_BLOCKS, SEARCH_BLOCKS))) {
            if (level.getBlockState(at).canBeReplaced() && level.getFluidState(at).isEmpty() && !level.getBlockState(at.below()).canBeReplaced()
                    && level.getFluidState(at.below()).isEmpty() && at.distSqr(from) < nearest) {
                nearest = at.distSqr(from);
                best = at.immutable();
            }
        }
        return Optional.ofNullable(best);
    }

    private static BlockPos floor(ServerLevel level, BlockPos from) {
        BlockPos floor = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, from);
        return floor.getY() > level.getMinBuildHeight() ? floor : from;
    }
}
