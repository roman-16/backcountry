package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.Alerts;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.client.Words;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.Worn;
import io.github.roman16.backcountry.survivor.Team;
import io.github.roman16.backcountry.trap.TrapBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Rations {
    private static final long TICKS_A_DAY = 24000;
    private static final long TICKS_AN_HOUR = 1000;
    private static final int FIRST_HOUR = 6;
    private static final int STEP_TICKS = 20;
    private static final long MOST_DAYS = 2;
    private static long last = Long.MIN_VALUE;

    private Rations() {}

    private record Source(ItemStack stack, Runnable changed, double airC) {}

    @SubscribeEvent
    static void started(ServerStartedEvent event) {
        last = Long.MIN_VALUE;
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long now = level.getDayTime();
        if (last == Long.MIN_VALUE || now < last) {
            last = now;
            return;
        }
        if (level.getGameTime() % STEP_TICKS != 0) {
            return;
        }
        long from = Math.max(last, now - MOST_DAYS * TICKS_A_DAY);
        Balance.Rations rations = Balance.get().rations();
        for (long day = Math.floorDiv(from, TICKS_A_DAY); day <= Math.floorDiv(now, TICKS_A_DAY); day++) {
            for (boolean dawn : new boolean[] {true, false}) {
                long meal = day * TICKS_A_DAY + ((dawn ? rations.dawnHour() : rations.duskHour()) - FIRST_HOUR) * TICKS_AN_HOUR;
                if (meal > from && meal <= now) {
                    serve(level, dawn);
                    if (!dawn) {
                        raid(level);
                    }
                }
            }
        }
        last = now;
    }

    public static void serve(ServerLevel level, boolean dawn) {
        Team team = Team.get(level.getServer());
        for (ServerPlayer player : level.players()) {
            if (Metabolism.lives(player)) {
                eat(player, team.ration().mealKcal(), dawn);
            }
        }
    }

    public static double eat(ServerPlayer player, double meal, boolean dawn) {
        List<Source> food = cached(player);
        if (food.isEmpty()) {
            food = carried(player);
        }
        food.sort(Comparator.comparingDouble(source -> Spoilage.daysLeft(source.stack(), source.airC()).orElse(Double.MAX_VALUE)));
        double eaten = 0;
        boolean full = false;
        Map<Item, Integer> bites = new LinkedHashMap<>();
        for (Source source : food) {
            while (eaten < meal && !source.stack().isEmpty()) {
                ItemStack bite = source.stack().copyWithCount(1);
                if (!Metabolism.eat(player, bite)) {
                    full = true;
                    break;
                }
                eaten += Pantry.kcal(bite);
                bites.merge(bite.getItem(), 1, Integer::sum);
                source.stack().shrink(1);
                source.changed().run();
                FoodProperties properties = bite.get(DataComponents.FOOD);
                if (properties != null && properties.usingConvertsTo().isPresent() && !player.getInventory().add(properties.usingConvertsTo().get().copy())) {
                    player.drop(properties.usingConvertsTo().get().copy(), false);
                }
            }
            if (full || eaten >= meal) {
                break;
            }
        }
        String when = dawn ? "dawn" : "dusk";
        Component told;
        if (eaten <= 0) {
            told = Component.translatable("ration.backcountry." + when + "_none", Balance.get().rations().reachBlocks());
        } else if (full) {
            told = Component.translatable("ration.backcountry." + when + "_full", eatenList(bites), Words.kcal(eaten));
        } else if (eaten < meal) {
            told = Component.translatable("ration.backcountry." + when + "_short", eatenList(bites), Words.kcal(eaten), Words.kcal(meal));
        } else {
            told = Component.translatable("ration.backcountry." + when, eatenList(bites), Words.kcal(eaten));
        }
        player.sendSystemMessage(told);
        Body body = Metabolism.body(player);
        if (!body.rationsTold) {
            body.rationsTold = true;
            player.sendSystemMessage(Component.translatable("ration.backcountry.how"));
        }
        return eaten;
    }

    private static Component eatenList(Map<Item, Integer> bites) {
        MutableComponent list = Component.empty();
        bites.forEach((item, count) -> {
            if (!list.getSiblings().isEmpty()) {
                list.append(", ");
            }
            list.append(Component.translatable("ration.backcountry.bites", count, item.getDescription()));
        });
        return list;
    }

    private static List<Source> cached(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double reach = Balance.get().rations().reachBlocks();
        List<Source> food = new ArrayList<>();
        for (BlockPos pos : Team.get(level.getServer()).caches()) {
            if (pos.distToCenterSqr(player.position()) <= reach * reach && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CacheEntity cache) {
                Spoilage.ageContainer(cache);
                double air = cache.airC();
                for (ItemStack stack : cache.stacks()) {
                    if (Pantry.usable(stack)) {
                        food.add(new Source(stack, cache::setChanged, air));
                    }
                }
            }
        }
        return food;
    }

    private static List<Source> carried(ServerPlayer player) {
        double air = Spoilage.airC(player.level(), player.blockPosition());
        List<Source> food = new ArrayList<>();
        for (ItemStack stack : Spoilage.carried(player)) {
            if (Pantry.usable(stack)) {
                food.add(new Source(stack, () -> {}, air));
            }
        }
        ItemStack pack = Worn.BACK.get(player);
        if (!pack.isEmpty()) {
            NonNullList<ItemStack> packed = Hotbar.contents(pack);
            for (ItemStack stack : packed) {
                if (Pantry.usable(stack)) {
                    food.add(new Source(stack, () -> Hotbar.store(pack, packed), air));
                }
            }
        }
        return food;
    }

    public static void raid(ServerLevel level) {
        Team team = Team.get(level.getServer());
        boolean raided = false;
        for (BlockPos pos : team.caches()) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CacheEntity cache && cache.reach() != CacheEntity.Reach.TALL
                    && level.random.nextDouble() < Balance.get().mice().chance()
                    && !TrapBlock.guards(level, pos, Balance.get().traps().miceBlocks())) {
                raided |= cache.raid(level.random);
                cache.report();
            }
        }
        if (raided) {
            for (ServerPlayer player : level.players()) {
                if (Metabolism.lives(player)) {
                    Alerts.announce(player, Alert.MICE_IN_CACHE);
                }
            }
        }
    }
}
