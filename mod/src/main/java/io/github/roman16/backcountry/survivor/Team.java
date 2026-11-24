package io.github.roman16.backcountry.survivor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.food.Ration;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.net.Messages;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Team extends SavedData {
    private static final String NAME = "backcountry_team";
    private static final int TICKS_A_SECOND = 20;

    private final List<Run> runs = new ArrayList<>();
    private final Map<Integer, Summary> years = new TreeMap<>();
    private final Map<String, Integer> animalsThisYear = new TreeMap<>();
    private boolean begun;
    private Optional<BlockPos> crate = Optional.empty();
    private final Map<Long, Double> caches = new TreeMap<>();
    private final Map<String, Integer> lastTaken = new TreeMap<>();
    private Ration ration = Ration.NORMAL;
    private double kcalThisYear;
    private int year = 1;

    public static final class Run {
        static final Codec<Run> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("survivor").forGetter(run -> run.survivor),
                Codec.STRING.fieldOf("name").forGetter(run -> run.name),
                Codec.INT.fieldOf("arrived_day").forGetter(run -> run.arrivedDay),
                Codec.DOUBLE.fieldOf("from_kg").forGetter(run -> run.fromKg),
                Codec.DOUBLE.fieldOf("kg").forGetter(run -> run.kg),
                Codec.INT.fieldOf("ended_day").forGetter(run -> run.endedDay),
                Summary.Kind.CODEC.fieldOf("end").forGetter(run -> run.end))
                .apply(instance, Run::new));

        final UUID survivor;
        final String name;
        final int arrivedDay;
        double fromKg;
        double kg;
        int endedDay;
        Summary.Kind end;

        Run(UUID survivor, String name, int arrivedDay, double fromKg, double kg, int endedDay, Summary.Kind end) {
            this.survivor = survivor;
            this.name = name;
            this.arrivedDay = arrivedDay;
            this.fromKg = fromKg;
            this.kg = kg;
            this.endedDay = endedDay;
            this.end = end;
        }
    }

    public static Team get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Team::new, Team::load, null), NAME);
    }

    private static Team load(CompoundTag tag, HolderLookup.Provider provider) {
        Team team = new Team();
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        team.begun = tag.getBoolean("begun");
        team.year = Math.max(1, tag.getInt("year"));
        team.kcalThisYear = tag.getDouble("kcal_this_year");
        CompoundTag animals = tag.getCompound("animals_this_year");
        animals.getAllKeys().forEach(key -> team.animalsThisYear.put(key, animals.getInt(key)));
        Run.CODEC.listOf().parse(ops, tag.get("runs")).result().ifPresent(team.runs::addAll);
        CompoundTag years = tag.getCompound("years");
        years.getAllKeys().forEach(key -> Summary.CODEC.parse(ops, years.get(key)).result().ifPresent(summary -> team.years.put(Integer.parseInt(key), summary)));
        if (tag.contains("ration")) {
            team.ration = Ration.valueOf(tag.getString("ration"));
        }
        CompoundTag taken = tag.getCompound("last_taken");
        taken.getAllKeys().forEach(key -> team.lastTaken.put(key, taken.getInt(key)));
        CompoundTag caches = tag.getCompound("caches");
        caches.getAllKeys().forEach(key -> team.caches.put(Long.parseLong(key), caches.getDouble(key)));
        if (tag.contains("crate")) {
            team.crate = Optional.of(BlockPos.of(tag.getLong("crate")));
        }
        return team;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        tag.putBoolean("begun", begun);
        tag.putInt("year", year);
        tag.putDouble("kcal_this_year", kcalThisYear);
        CompoundTag animals = new CompoundTag();
        animalsThisYear.forEach(animals::putInt);
        tag.put("animals_this_year", animals);
        Run.CODEC.listOf().encodeStart(ops, runs).ifSuccess(encoded -> tag.put("runs", encoded));
        CompoundTag kept = new CompoundTag();
        years.forEach((number, summary) -> Summary.CODEC.encodeStart(ops, summary).ifSuccess(encoded -> kept.put(Integer.toString(number), encoded)));
        tag.put("years", kept);
        crate.ifPresent(pos -> tag.putLong("crate", pos.asLong()));
        tag.putString("ration", ration.name());
        CompoundTag stocked = new CompoundTag();
        caches.forEach((pos, kcal) -> stocked.putDouble(Long.toString(pos), kcal));
        tag.put("caches", stocked);
        CompoundTag taken = new CompoundTag();
        lastTaken.forEach(taken::putInt);
        tag.put("last_taken", taken);
        return tag;
    }

    public boolean begun() {
        return begun;
    }

    public void begin() {
        begun = true;
        setDirty();
    }

    public Optional<BlockPos> crate() {
        return crate;
    }

    public void crate(BlockPos pos) {
        crate = Optional.of(pos);
        setDirty();
    }

    public Ration ration() {
        return ration;
    }

    public void ration(Ration chosen) {
        ration = chosen;
        setDirty();
    }

    public void stocked(BlockPos pos, double kcal) {
        Double before = caches.put(pos.asLong(), kcal);
        if (before == null || before != kcal) {
            setDirty();
        }
    }

    public void emptied(BlockPos pos) {
        if (caches.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    public List<BlockPos> caches() {
        return caches.keySet().stream().map(BlockPos::of).toList();
    }

    public static int living(MinecraftServer server) {
        return (int) server.getPlayerList().getPlayers().stream().filter(Metabolism::lives).count();
    }

    public double storesDays(int survivors) {
        double stored = caches.values().stream().mapToDouble(Double::doubleValue).sum();
        return stored / (ration.dayKcal() * Math.max(1, survivors));
    }

    public void arrived(ServerPlayer player, Body body) {
        runs.add(new Run(player.getUUID(), player.getGameProfile().getName(), body.arrivedDay, body.weightKg, body.weightKg, -1, Summary.Kind.LIVING));
        setDirty();
    }

    public void note(ServerPlayer player, Body body) {
        current(player.getUUID()).ifPresent(run -> run.kg = body.weightKg);
        setDirty();
    }

    public void ended(ServerPlayer player, Body body, Summary.Kind end, int day) {
        current(player.getUUID()).ifPresent(run -> {
            run.kg = body.weightKg;
            run.endedDay = day;
            run.end = end;
        });
        setDirty();
    }

    public void ate(double kcal) {
        kcalThisYear += kcal;
        setDirty();
    }

    public void took(String entity, int day) {
        animalsThisYear.merge(entity, 1, Integer::sum);
        lastTaken.put(entity, day);
        setDirty();
    }

    public void caught(int fish) {
        animalsThisYear.merge(Fish.CAUGHT, fish, Integer::sum);
        setDirty();
    }

    public int lastTaken(String entity) {
        return lastTaken.getOrDefault(entity, Integer.MIN_VALUE / 2);
    }

    public Optional<Summary> year(int number) {
        return Optional.ofNullable(years.get(number));
    }

    private Optional<Run> current(UUID survivor) {
        for (int i = runs.size() - 1; i >= 0; i--) {
            if (runs.get(i).survivor.equals(survivor)) {
                return runs.get(i).end == Summary.Kind.LIVING ? Optional.of(runs.get(i)) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void turn(int lastDay) {
        int days = Balance.get().year().days();
        int firstDay = (year - 1) * days;
        List<Summary.Line> lines = new ArrayList<>();
        for (Run run : runs) {
            if (run.endedDay < 0 || run.endedDay >= firstDay) {
                lines.add(new Summary.Line(run.name, run.fromKg, run.kg, run.arrivedDay, run.endedDay, run.end));
            }
        }
        List<Summary.Taken> taken = new ArrayList<>();
        animalsThisYear.forEach((entity, count) -> taken.add(new Summary.Taken(entity, count)));
        taken.sort(Comparator.comparingInt(Summary.Taken::count).reversed());
        years.put(year, new Summary(Summary.Kind.YEAR, lastDay, 0, 0, 0, days, kcalThisYear, List.copyOf(taken), List.of(), List.copyOf(lines)));
        runs.removeIf(run -> run.end != Summary.Kind.LIVING);
        runs.forEach(run -> run.fromKg = run.kg);
        animalsThisYear.clear();
        kcalThisYear = 0;
        year++;
        setDirty();
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICKS_A_SECOND != 0) {
            return;
        }
        Team team = get(server);
        if (!team.begun) {
            return;
        }
        ServerLevel overworld = server.overworld();
        int day = Calendar.day(overworld);
        while (Calendar.year(day) > team.year) {
            team.turn(team.year * Balance.get().year().days() - 1);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Body body = Metabolism.body(player);
            if (Metabolism.lives(player) && body.yearSeen < team.year && quiet(player, body)) {
                team.year(team.year - 1).ifPresent(summary -> Messages.summary(player, summary));
                body.yearSeen = team.year;
            }
        }
    }

    public static boolean quiet(ServerPlayer player, Body body) {
        return player.onGround() && !player.isInWater() && !player.isInLava() && !player.isSleeping() && body.blackoutTicks == 0
                && player.level().getGameTime() - body.lastHurt >= (long) Balance.get().survivor().quietSeconds() * TICKS_A_SECOND;
    }
}
