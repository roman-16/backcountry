package io.github.roman16.backcountry.guide;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Rooms;
import io.github.roman16.backcountry.body.Spirits;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.Pantry;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.MapSheets;
import io.github.roman16.backcountry.gear.Worn;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.survivor.Team;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Guide extends SavedData {
    private static final String NAME = "backcountry_guide";
    private static final String SEPARATOR = "/";
    private static final int CHECK_TICKS = 100;
    private static final Thing BERRIES = Thing.any(Thing.of(Things.BLUEBERRIES), Thing.of(Things.HIGHBUSH_CRANBERRIES), Thing.of(Things.SALMONBERRIES),
            Thing.of("minecraft:sweet_berries"));

    private final Map<UUID, Set<String>> own = new HashMap<>();
    private final Map<UUID, Task> tracked = new HashMap<>();
    private final EnumSet<Task> goals = EnumSet.noneOf(Task.class);
    private final Map<Long, Integer> woodpiles = new TreeMap<>();
    private final Set<String> made = new TreeSet<>();
    private int year;

    public static Guide get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Guide::new, Guide::load, null), NAME);
    }

    private static Guide load(CompoundTag tag, HolderLookup.Provider provider) {
        Guide guide = new Guide();
        CompoundTag players = tag.getCompound("own");
        for (String key : players.getAllKeys()) {
            Set<String> ticks = new TreeSet<>();
            players.getList(key, Tag.TAG_STRING).forEach(entry -> ticks.add(entry.getAsString()));
            guide.own.put(UUID.fromString(key), ticks);
        }
        CompoundTag following = tag.getCompound("tracked");
        for (String key : following.getAllKeys()) {
            task(following.getString(key)).ifPresent(task -> guide.tracked.put(UUID.fromString(key), task));
        }
        tag.getList("goals", Tag.TAG_STRING).forEach(entry -> task(entry.getAsString()).ifPresent(guide.goals::add));
        CompoundTag piles = tag.getCompound("woodpiles");
        for (String key : piles.getAllKeys()) {
            guide.woodpiles.put(Long.parseLong(key), piles.getInt(key));
        }
        tag.getList("made", Tag.TAG_STRING).forEach(entry -> guide.made.add(entry.getAsString()));
        guide.year = tag.getInt("year");
        return guide;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag players = new CompoundTag();
        own.forEach((player, ticks) -> players.put(player.toString(), strings(ticks)));
        tag.put("own", players);
        CompoundTag following = new CompoundTag();
        tracked.forEach((player, task) -> following.putString(player.toString(), task.id()));
        tag.put("tracked", following);
        tag.put("goals", strings(goals.stream().map(Task::id).toList()));
        CompoundTag piles = new CompoundTag();
        woodpiles.forEach((pos, dry) -> piles.putInt(Long.toString(pos), dry));
        tag.put("woodpiles", piles);
        tag.put("made", strings(made));
        tag.putInt("year", year);
        return tag;
    }

    private static Optional<Task> task(String id) {
        for (Task task : Task.values()) {
            if (task.id().equals(id)) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    private static ListTag strings(Iterable<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }

    private static String key(Task task, Step step) {
        return task.id() + SEPARATOR + step.id();
    }

    public static void did(ServerPlayer player, Deed deed) {
        tick(player, check -> check instanceof Check.Did did && did.deed() == deed);
    }

    private static void tick(ServerPlayer player, Predicate<Check> done) {
        if (!Metabolism.lives(player)) {
            return;
        }
        Guide guide = get(player.server);
        Set<String> ticks = guide.own.computeIfAbsent(player.getUUID(), uuid -> new TreeSet<>());
        boolean changed = false;
        for (Task task : Task.values()) {
            if (task.team()) {
                continue;
            }
            for (Step step : task.steps()) {
                if (task.pace() == Task.Pace.IN_ORDER && !ticks.contains(key(task, step)) && !done.test(step.check())) {
                    break;
                }
                if (done.test(step.check()) && ticks.add(key(task, step))) {
                    changed = true;
                    if (step == task.steps().getLast()) {
                        task.steps().forEach(earlier -> ticks.add(key(task, earlier)));
                        Spirits.add(Metabolism.body(player), Balance.get().spirits().taskDone());
                    }
                }
            }
        }
        if (changed) {
            guide.setDirty();
        }
    }

    public static void track(ServerPlayer player, Optional<Task> task) {
        Guide guide = get(player.server);
        if (task.isPresent()) {
            guide.tracked.put(player.getUUID(), task.get());
        } else {
            guide.tracked.remove(player.getUUID());
        }
        guide.setDirty();
    }

    public static void woodpile(MinecraftServer server, BlockPos pos, int dry) {
        Guide guide = get(server);
        Integer before = guide.woodpiles.put(pos.asLong(), dry);
        if (before == null || before != dry) {
            guide.setDirty();
        }
    }

    public static void woodpileGone(MinecraftServer server, BlockPos pos) {
        Guide guide = get(server);
        if (guide.woodpiles.remove(pos.asLong()) != null) {
            guide.setDirty();
        }
    }

    public static int dry(List<ItemStack> pieces) {
        return (int) pieces.stream().filter(FirewoodItem::catches).count();
    }

    public int dryFirewood() {
        return woodpiles.values().stream().mapToInt(Integer::intValue).sum();
    }

    public GuideView view(ServerPlayer player) {
        Set<String> ticks = own.getOrDefault(player.getUUID(), Set.of());
        List<Integer> steps = new ArrayList<>();
        for (Task task : Task.values()) {
            int mask = 0;
            for (int i = 0; i < task.steps().size(); i++) {
                Step step = task.steps().get(i);
                boolean done = task.team() ? goals.contains(task) || teamStep(step) : ticks.contains(key(task, step));
                mask |= done ? 1 << i : 0;
            }
            steps.add(mask);
        }
        Team team = Team.get(player.server);
        return new GuideView(List.copyOf(steps), (float) team.storesDays(Team.living(player.server)), dryFirewood(),
                Optional.ofNullable(tracked.get(player.getUUID())));
    }

    private boolean teamStep(Step step) {
        return step.check() instanceof Check.Made item && made.contains(key(item.item().get()));
    }

    public boolean reached(Task goal) {
        return goals.contains(goal);
    }

    private static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @SubscribeEvent
    static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Item result = event.getCrafting().getItem();
        tick(player, check -> check instanceof Check.Made item && item.item().get() == result);
        boolean clothing = Task.HIDE_CLOTHING.steps().stream().anyMatch(step -> step.check() instanceof Check.Made item && item.item().get() == result);
        Guide guide = get(player.server);
        if (clothing && guide.made.add(key(result))) {
            guide.setDirty();
        }
    }

    @SubscribeEvent
    static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player, check -> check instanceof Check.Placed placed && event.getPlacedBlock().is(placed.block().get()));
        }
    }

    @SubscribeEvent
    static void ate(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack eaten = event.getItem();
        if (BERRIES.matches(eaten)) {
            did(player, Deed.ATE_BERRIES);
        }
        if (Pantry.cookedMeat(eaten)) {
            did(player, Deed.ATE_COOKED);
        }
    }

    @SubscribeEvent
    static void killed(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && Animal.of(event.getEntity().getType()).isPresent()) {
            did(player, Deed.KILLED_ANIMAL);
            FieldNotes.note(player, event.getEntity());
            if (Animal.of(event.getEntity().getType()).get().kg() >= Balance.get().spirits().bigCatchKg()) {
                Spirits.add(Metabolism.body(player), Balance.get().spirits().bigCatch());
            }
        }
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        get(server).check(server);
    }

    public void check(MinecraftServer server) {
        int day = Calendar.day(server.overworld());
        int thisYear = Calendar.year(day);
        if (thisYear != year) {
            year = thisYear;
            goals.clear();
            setDirty();
        }
        Balance.Tasks rules = Balance.get().tasks();
        Team team = Team.get(server);
        double stores = team.storesDays(Team.living(server));
        int month = Calendar.date(day).getMonthValue();
        for (Task task : Task.values()) {
            if (task.team() && task.steps().stream().allMatch(step -> switch (step.check()) {
                case Check.FoodStores ignored -> stores >= rules.foodDays();
                case Check.DryFirewood ignored -> dryFirewood() >= rules.dryFirewood();
                case Check.Made item -> made.contains(key(item.item().get()));
                case Check.Reach reach -> month == reach.month();
                default -> false;
            })) {
                reach(task);
            }
        }
        server.getPlayerList().getPlayers().forEach(Guide::sense);
    }

    public static void sense(ServerPlayer player) {
        List<ItemStack> carried = Hotbar.carried(player);
        double stores = Team.get(player.server).storesDays(Team.living(player.server));
        tick(player, check -> switch (check) {
            case Check.Held held -> carried.stream().filter(held.thing()::matches).mapToInt(ItemStack::getCount).sum() >= held.count();
            case Check.Sheltered ignored -> Metabolism.body(player).sheltered;
            case Check.WearsHidePack ignored -> Worn.BACK.get(player).is(Things.HIDE_PACK.get());
            case Check.FoodInACache ignored -> stores > 0;
            case Check.InCabin ignored -> Metabolism.body(player).room == Rooms.Kind.CABIN;
            case Check.WarmCabin ignored -> Metabolism.body(player).room == Rooms.Kind.CABIN && Metabolism.body(player).fire;
            case Check.TallCache ignored -> tallCache(player);
            case Check.Surveys surveys -> carried.stream().anyMatch(stack -> stack.is(Things.BARK_MAP.get()) && MapSheets.surveys(player.serverLevel(), stack) >= surveys.count());
            case Check.Noted noted -> FieldNotes.get(player.server).of(player).size() >= noted.count();
            default -> false;
        });
    }

    private static boolean tallCache(ServerPlayer player) {
        for (BlockPos pos : Team.get(player.server).caches()) {
            if (player.serverLevel().isLoaded(pos) && player.serverLevel().getBlockEntity(pos) instanceof CacheEntity cache && cache.reach() != CacheEntity.Reach.GROUND) {
                return true;
            }
        }
        return false;
    }

    private void reach(Task goal) {
        if (goals.add(goal)) {
            setDirty();
        }
    }
}
