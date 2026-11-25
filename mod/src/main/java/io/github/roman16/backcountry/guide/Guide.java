package io.github.roman16.backcountry.guide;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.Alerts;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.survivor.Team;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Guide extends SavedData {
    private static final String NAME = "backcountry_guide";
    private static final int CHECK_TICKS = 100;
    private static final int APRIL = 4;
    private static final int NOVEMBER = 11;

    private final Map<UUID, EnumSet<Task>> own = new HashMap<>();
    private final Map<UUID, String> shown = new HashMap<>();
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
            guide.own.put(UUID.fromString(key), tasks(players.getList(key, Tag.TAG_STRING)));
        }
        CompoundTag seen = tag.getCompound("shown");
        for (String key : seen.getAllKeys()) {
            guide.shown.put(UUID.fromString(key), seen.getString(key));
        }
        guide.goals.addAll(tasks(tag.getList("goals", Tag.TAG_STRING)));
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
        own.forEach((player, tasks) -> players.put(player.toString(), list(tasks)));
        tag.put("own", players);
        CompoundTag seen = new CompoundTag();
        shown.forEach((player, key) -> seen.putString(player.toString(), key));
        tag.put("shown", seen);
        tag.put("goals", list(goals));
        CompoundTag piles = new CompoundTag();
        woodpiles.forEach((pos, dry) -> piles.putInt(Long.toString(pos), dry));
        tag.put("woodpiles", piles);
        ListTag crafted = new ListTag();
        made.forEach(item -> crafted.add(StringTag.valueOf(item)));
        tag.put("made", crafted);
        tag.putInt("year", year);
        return tag;
    }

    private static EnumSet<Task> tasks(ListTag list) {
        EnumSet<Task> tasks = EnumSet.noneOf(Task.class);
        for (Tag entry : list) {
            for (Task task : Task.values()) {
                if (task.id().equals(entry.getAsString())) {
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    private static ListTag list(Set<Task> tasks) {
        ListTag list = new ListTag();
        tasks.forEach(task -> list.add(StringTag.valueOf(task.id())));
        return list;
    }

    public static void done(ServerPlayer player, Task task) {
        if (task.stage().team() || !Metabolism.lives(player)) {
            return;
        }
        Guide guide = get(player.server);
        if (guide.own.computeIfAbsent(player.getUUID(), uuid -> EnumSet.noneOf(Task.class)).add(task)) {
            guide.setDirty();
        }
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

    public GuideView view(ServerPlayer player) {
        return view(player.getUUID(), Calendar.date(Calendar.day(player.level())).getMonthValue());
    }

    public GuideView view(UUID player, int month) {
        EnumSet<Task> mine = own.getOrDefault(player, EnumSet.noneOf(Task.class));
        List<Task> first = Task.Stage.FIRST_DAYS.tasks();
        if (!mine.containsAll(first)) {
            return new GuideView(Task.Stage.FIRST_DAYS, first, first.stream().filter(mine::contains).toList());
        }
        if (month == APRIL) {
            return new GuideView(Task.Stage.YEAR, Task.Stage.YEAR.tasks(), List.of());
        }
        List<Task> preparing = Task.Stage.BEFORE_SNOW.tasks();
        List<Task> done = preparing.stream().filter(goals::contains).toList();
        if (done.size() == preparing.size() || month >= NOVEMBER || month < APRIL) {
            List<Task> open = new ArrayList<>(preparing.stream().filter(task -> !goals.contains(task)).toList());
            open.addAll(Task.Stage.WINTER.tasks());
            return new GuideView(Task.Stage.WINTER, List.copyOf(open), List.of());
        }
        return new GuideView(Task.Stage.BEFORE_SNOW, preparing, done);
    }

    @SubscribeEvent
    static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack result = event.getCrafting();
        if (result.getItem() instanceof FirewoodItem) {
            done(player, Task.SPLIT_LOG);
        }
        if (result.is(Things.HIDE_HAT.get()) || result.is(Things.HIDE_COAT.get()) || result.is(Things.SNOWSHOES.get())) {
            Guide guide = get(player.server);
            if (guide.made.add(key(result.getItem()))) {
                guide.setDirty();
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
        if (team.storesDays(Team.living(server)) >= rules.foodDays()) {
            reach(Task.FOOD_STORES);
        }
        if (woodpiles.values().stream().mapToInt(Integer::intValue).sum() >= rules.dryFirewood()) {
            reach(Task.DRY_FIREWOOD);
        }
        if (made.containsAll(List.of(key(Things.HIDE_HAT.get()), key(Things.HIDE_COAT.get()), key(Things.SNOWSHOES.get())))) {
            reach(Task.HIDE_CLOTHING);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            notice(player);
        }
    }

    public void notice(ServerPlayer player) {
        if (!Metabolism.lives(player)) {
            return;
        }
        String key = year + ":" + view(player).stage().name();
        if (!key.equals(shown.get(player.getUUID()))) {
            shown.put(player.getUUID(), key);
            setDirty();
            Alerts.announce(player, Alert.NEW_TASKS);
        }
    }

    public boolean reached(Task goal) {
        return goals.contains(goal);
    }

    private static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private void reach(Task goal) {
        if (goals.add(goal)) {
            setDirty();
        }
    }
}
