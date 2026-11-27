package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.gear.PackRows;
import io.github.roman16.backcountry.survivor.Summary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

public final class Body implements INBTSerializable<CompoundTag> {
    public final EnumMap<Alert, Long> alerts = new EnumMap<>(Alert.class);
    public int alertsDay;
    public boolean arrived;
    public int arrivedDay;
    public double arrivedKg;
    public long awakeTicks;
    public int blackoutTicks;
    public boolean spent;
    public final double[] burned = new double[Burn.values().length];
    public double carbsToday;
    public final EnumMap<Condition, Affliction> conditions = new EnumMap<>(Condition.class);
    public double coreC;
    public double energy;
    @Nullable
    public Summary ended;
    public double fatToday;
    public double frostHours;
    public int gameModeBeforeEnd;
    public double gutOnsetDays;
    public double healHours;
    public double kcalBurnedToday;
    public double kcalEaten;
    public double kcalEatenToday;
    public final Map<String, Integer> animals = new TreeMap<>();
    public int lastDay;
    public long lastHurt;
    public double metresToday;
    public final PackRows packRows = new PackRows();
    public double proteinToday;
    public int leanDays;
    public int fattyDays;
    public double numbHours;
    public double spirits = 0.5;
    public int spiritCauses;
    public final List<String> mealItems = new ArrayList<>();
    public final List<Long> mealTicks = new ArrayList<>();
    public final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
    public double stomachCarbs;
    public double stomachFat;
    public double stomachKcal;
    public double stomachProtein;
    public double stomachVitaminC;
    public int stumbleTicks;
    public double thirstSeconds;
    public boolean rationsTold;
    public boolean unread;
    public double vitaminCMg;
    public double warmHours;
    public double waterL;
    public double weightKg;
    public double weightMarkKg;
    public final List<Float> weights = new ArrayList<>();
    public double wetBlocks;
    public double wetness;
    public int yearSeen;

    public transient double clo;
    public transient Rooms.Kind room = Rooms.Kind.OPEN;
    public transient double roomC;
    public transient boolean smoky;
    public transient double effectiveC;
    public transient double exertedKcal;
    public transient boolean fire;
    public transient double lastCoreC;
    @Nullable
    public transient Vec3 lastPosition;
    public transient double loadKg;
    public transient int loadWarnedAt;
    public transient boolean sheltered;
    public transient double snowBlocks;
    public final transient Exertion.Strides strides = new Exertion.Strides();
    public transient Vec3 snowFrom;
    public transient boolean windy;
    public transient double windKmh;
    public transient double workKcal;

    public static final class Affliction {
        public boolean bandaged;
        public double days;
        public boolean told;

        public Affliction(double days, boolean bandaged, boolean told) {
            this.days = days;
            this.bandaged = bandaged;
            this.told = told;
        }
    }

    public void reset(int day, int year) {
        Balance balance = Balance.get();
        alerts.clear();
        alertsDay = day;
        arrived = true;
        arrivedDay = day;
        arrivedKg = balance.body().arrivalKg();
        spirits = balance.spirits().arrivalShare();
        spiritCauses = 0;
        leanDays = 0;
        fattyDays = 0;
        numbHours = 0;
        mealItems.clear();
        mealTicks.clear();
        awakeTicks = 0;
        blackoutTicks = 0;
        conditions.clear();
        coreC = balance.warmth().coreC();
        lastCoreC = coreC;
        energy = 1;
        ended = null;
        frostHours = 0;
        gutOnsetDays = 0;
        healHours = 0;
        kcalEaten = 0;
        animals.clear();
        lastDay = day;
        skills.clear();
        stomachCarbs = 0;
        stomachFat = 0;
        stomachKcal = 0;
        stomachProtein = 0;
        stomachVitaminC = 0;
        stumbleTicks = 0;
        thirstSeconds = 0;
        rationsTold = false;
        unread = false;
        vitaminCMg = balance.body().vitaminCMgDay() * balance.body().vitaminCStoreDays();
        warmHours = 0;
        waterL = balance.water().needLDay();
        weightKg = arrivedKg;
        weightMarkKg = arrivedKg;
        weights.clear();
        weights.add((float) weightKg);
        wetBlocks = 0;
        wetness = 0;
        yearSeen = year;
        newDay();
    }

    public void newDay() {
        Arrays.fill(burned, 0);
        carbsToday = 0;
        fatToday = 0;
        kcalBurnedToday = 0;
        kcalEatenToday = 0;
        metresToday = 0;
        proteinToday = 0;
    }

    public double bmi() {
        double height = Balance.get().body().heightM();
        return weightKg / (height * height);
    }

    public boolean has(Condition condition) {
        return conditions.containsKey(condition);
    }

    public void afflict(Condition condition) {
        conditions.putIfAbsent(condition, new Affliction(0, false, false));
    }

    public double stomachShare() {
        return stomachKcal / Balance.get().body().stomachKcal();
    }

    public double waterShare() {
        return waterL / Balance.get().water().needLDay();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag raised = new CompoundTag();
        alerts.forEach((alert, at) -> raised.putLong(alert.id(), at));
        tag.put("alerts", raised);
        tag.putInt("alerts_day", alertsDay);
        tag.putBoolean("arrived", arrived);
        tag.putInt("arrived_day", arrivedDay);
        tag.putDouble("arrived_kg", arrivedKg);
        tag.putLong("awake_ticks", awakeTicks);
        tag.putInt("blackout_ticks", blackoutTicks);
        CompoundTag burns = new CompoundTag();
        for (Burn burn : Burn.values()) {
            burns.putDouble(burn.id(), burned[burn.ordinal()]);
        }
        tag.put("burned", burns);
        tag.putDouble("carbs_today", carbsToday);
        CompoundTag afflictions = new CompoundTag();
        conditions.forEach((condition, affliction) -> {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("days", affliction.days);
            entry.putBoolean("bandaged", affliction.bandaged);
            entry.putBoolean("told", affliction.told);
            afflictions.put(condition.id(), entry);
        });
        tag.put("conditions", afflictions);
        tag.putDouble("core_c", coreC);
        tag.putDouble("energy", energy);
        if (ended != null) {
            Summary.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), ended).ifSuccess(encoded -> tag.put("ended", encoded));
        }
        tag.putDouble("fat_today", fatToday);
        tag.putDouble("frost_hours", frostHours);
        tag.putInt("game_mode_before_end", gameModeBeforeEnd);
        tag.putDouble("gut_onset_days", gutOnsetDays);
        tag.putDouble("heal_hours", healHours);
        tag.putDouble("kcal_burned_today", kcalBurnedToday);
        tag.putDouble("kcal_eaten", kcalEaten);
        tag.putDouble("kcal_eaten_today", kcalEatenToday);
        CompoundTag taken = new CompoundTag();
        animals.forEach(taken::putInt);
        tag.put("animals", taken);
        tag.putInt("last_day", lastDay);
        tag.putLong("last_hurt", lastHurt);
        tag.putDouble("metres_today", metresToday);
        tag.putDouble("protein_today", proteinToday);
        tag.putDouble("spirits", spirits);
        tag.putInt("lean_days", leanDays);
        tag.putInt("fatty_days", fattyDays);
        tag.putDouble("numb_hours", numbHours);
        ListTag meals = new ListTag();
        for (int i = 0; i < mealItems.size(); i++) {
            CompoundTag meal = new CompoundTag();
            meal.putString("item", mealItems.get(i));
            meal.putLong("tick", mealTicks.get(i));
            meals.add(meal);
        }
        tag.put("meals", meals);
        CompoundTag practised = new CompoundTag();
        skills.forEach((skill, uses) -> practised.putInt(skill.id(), uses));
        tag.put("skills", practised);
        tag.putDouble("stomach_carbs", stomachCarbs);
        tag.putDouble("stomach_fat", stomachFat);
        tag.putDouble("stomach_kcal", stomachKcal);
        tag.putDouble("stomach_protein", stomachProtein);
        tag.putDouble("stomach_vitamin_c", stomachVitaminC);
        tag.putDouble("thirst_seconds", thirstSeconds);
        tag.putBoolean("rations_told", rationsTold);
        tag.putBoolean("unread", unread);
        tag.putDouble("vitamin_c_mg", vitaminCMg);
        tag.putDouble("warm_hours", warmHours);
        tag.putDouble("water_l", waterL);
        tag.putDouble("weight_kg", weightKg);
        tag.putDouble("weight_mark_kg", weightMarkKg);
        ListTag curve = new ListTag();
        weights.forEach(weight -> curve.add(FloatTag.valueOf(weight)));
        tag.put("weights", curve);
        tag.putDouble("wet_blocks", wetBlocks);
        tag.putDouble("wetness", wetness);
        tag.putInt("year_seen", yearSeen);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        alerts.clear();
        CompoundTag raised = tag.getCompound("alerts");
        for (Alert alert : Alert.values()) {
            if (raised.contains(alert.id())) {
                alerts.put(alert, raised.getLong(alert.id()));
            }
        }
        alertsDay = tag.getInt("alerts_day");
        arrived = tag.getBoolean("arrived");
        arrivedDay = tag.getInt("arrived_day");
        arrivedKg = tag.getDouble("arrived_kg");
        awakeTicks = tag.getLong("awake_ticks");
        blackoutTicks = tag.getInt("blackout_ticks");
        CompoundTag burns = tag.getCompound("burned");
        for (Burn burn : Burn.values()) {
            burned[burn.ordinal()] = burns.getDouble(burn.id());
        }
        carbsToday = tag.getDouble("carbs_today");
        conditions.clear();
        CompoundTag afflictions = tag.getCompound("conditions");
        for (Condition condition : Condition.values()) {
            if (afflictions.contains(condition.id())) {
                CompoundTag entry = afflictions.getCompound(condition.id());
                conditions.put(condition, new Affliction(entry.getDouble("days"), entry.getBoolean("bandaged"), entry.getBoolean("told")));
            }
        }
        coreC = tag.getDouble("core_c");
        lastCoreC = coreC;
        energy = tag.getDouble("energy");
        ended = tag.contains("ended") ? Summary.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.get("ended")).result().orElse(null) : null;
        fatToday = tag.getDouble("fat_today");
        frostHours = tag.getDouble("frost_hours");
        gameModeBeforeEnd = tag.getInt("game_mode_before_end");
        gutOnsetDays = tag.getDouble("gut_onset_days");
        healHours = tag.getDouble("heal_hours");
        kcalBurnedToday = tag.getDouble("kcal_burned_today");
        kcalEaten = tag.getDouble("kcal_eaten");
        kcalEatenToday = tag.getDouble("kcal_eaten_today");
        animals.clear();
        CompoundTag taken = tag.getCompound("animals");
        taken.getAllKeys().forEach(key -> animals.put(key, taken.getInt(key)));
        lastDay = tag.getInt("last_day");
        lastHurt = tag.getLong("last_hurt");
        metresToday = tag.getDouble("metres_today");
        proteinToday = tag.getDouble("protein_today");
        spirits = tag.getDouble("spirits");
        leanDays = tag.getInt("lean_days");
        fattyDays = tag.getInt("fatty_days");
        numbHours = tag.getDouble("numb_hours");
        mealItems.clear();
        mealTicks.clear();
        for (Tag meal : tag.getList("meals", Tag.TAG_COMPOUND)) {
            mealItems.add(((CompoundTag) meal).getString("item"));
            mealTicks.add(((CompoundTag) meal).getLong("tick"));
        }
        skills.clear();
        CompoundTag practised = tag.getCompound("skills");
        for (Skill skill : Skill.values()) {
            if (practised.contains(skill.id())) {
                skills.put(skill, practised.getInt(skill.id()));
            }
        }
        stomachCarbs = tag.getDouble("stomach_carbs");
        stomachFat = tag.getDouble("stomach_fat");
        stomachKcal = tag.getDouble("stomach_kcal");
        stomachProtein = tag.getDouble("stomach_protein");
        stomachVitaminC = tag.getDouble("stomach_vitamin_c");
        thirstSeconds = tag.getDouble("thirst_seconds");
        rationsTold = tag.getBoolean("rations_told");
        unread = tag.getBoolean("unread");
        vitaminCMg = tag.getDouble("vitamin_c_mg");
        warmHours = tag.getDouble("warm_hours");
        waterL = tag.getDouble("water_l");
        weightKg = tag.getDouble("weight_kg");
        weightMarkKg = tag.getDouble("weight_mark_kg");
        weights.clear();
        tag.getList("weights", Tag.TAG_FLOAT).forEach(entry -> weights.add(((FloatTag) entry).getAsFloat()));
        wetBlocks = tag.getDouble("wet_blocks");
        wetness = tag.getDouble("wetness");
        yearSeen = tag.getInt("year_seen");
    }
}
