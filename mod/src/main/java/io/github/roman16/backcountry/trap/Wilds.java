package io.github.roman16.backcountry.trap;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Wilds {
    private static final ResourceLocation WORM = ResourceLocation.fromNamespaceAndPath("riverfishing", "worm");
    private static final int TICKS_A_SECOND = 20;
    private static final double MOVING = 0.05;
    private static final double BASE_HPA = 1013;
    private static final double CLOUD_HPA = 24;
    private static final double FALLING_HPA = 8;
    private static final double WIND_HPA = 8;
    private static final double HIGHEST_WIND = 96;

    private Wilds() {}

    @SubscribeEvent
    static void dropped(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        Balance.Bait bait = Balance.get().bait();
        if ((state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) && Hunting.months(Seasons.month(level), bait.cricketMonths())
                && level.random.nextDouble() < bait.cricketChance()) {
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Things.CRICKET.get())));
        }
        if (state.is(BlockTags.DIRT) && event.getTool().getItem() instanceof ShovelItem && Climate.airC(level, pos, Calendar.day(level)) > 0
                && level.random.nextDouble() < (rainedLately(level, pos) ? bait.rainWormChance() : bait.dryWormChance())) {
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(BuiltInRegistries.ITEM.get(WORM))));
        }
    }

    private static boolean rainedLately(Level level, BlockPos pos) {
        int day = Calendar.day(level);
        double hour = Climate.hour(level);
        for (int back = 0; back <= Balance.get().bait().wormHours(); back++) {
            double at = hour - back;
            int which = at < 0 ? day - 1 : day;
            if (Weather.day(level, which).falling(at < 0 ? at + Weather.HOURS : at) && Climate.airC(level, pos, day) > Balance.get().weather().snowBelowC()) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    static void rafted(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % TICKS_A_SECOND != 0 || !(player.getVehicle() instanceof Boat boat)
                || boat.getVariant() != Boat.Type.BAMBOO || boat.getDeltaMovement().horizontalDistance() < MOVING || !Metabolism.lives(player)) {
            return;
        }
        Balance.Raft raft = Balance.get().raft();
        double wind = Weather.day(player.level(), Calendar.day(player.level())).windKmh();
        if (wind > raft.calmKmh() && player.getRandom().nextDouble() < raft.fallChance() * (wind - raft.calmKmh()) / (HIGHEST_WIND - raft.calmKmh())) {
            player.stopRiding();
            player.displayClientMessage(Component.translatable("message.backcountry.fell_off"), true);
        }
    }

    public static double pressure(Level level, int day, double hour) {
        Day weather = Weather.day(level, day);
        return BASE_HPA + CLOUD_HPA * (0.5 - Weather.cloud(level, day, hour)) - (weather.falling(hour) ? FALLING_HPA : 0) - WIND_HPA * weather.windKmh() / HIGHEST_WIND;
    }

    public static double pressureTrend(Level level) {
        int day = Calendar.day(level);
        double hour = Climate.hour(level);
        double before = hour - 3;
        return pressure(level, day, hour) - (before < 0 ? pressure(level, day - 1, before + Weather.HOURS) : pressure(level, day, before));
    }
}
