package io.github.roman16.backcountry.weather;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomSupport;

public final class Weather {
    public static final int HOURS = 24;
    public static final int EVENING = 12;
    public static final int SHARED_BEFORE = 1;
    public static final int SHARED_AFTER = 2;
    private static final int COMPASS = 8;
    private static final int ANY_YEAR = 2001;
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final double RAYLEIGH_MEAN = Math.sqrt(Math.PI) / 2;
    private static final List<Double> PRECIPITATION_SHARES = IntStream.range(0, Balance.get().weather().precipitationDays().size())
            .mapToObj(month -> Balance.get().weather().precipitationDays().get(month) / YearMonth.of(ANY_YEAR, month + 1).lengthOfMonth()).toList();
    private static final Map<Long, List<Day>> DRAWN = new HashMap<>();
    private static final Map<Integer, Day> SET = new ConcurrentHashMap<>();
    private static volatile Map<Integer, Day> shared = Map.of();

    private Weather() {}

    public static Day day(Level level, int day) {
        if (level instanceof ServerLevel server) {
            Day set = SET.get(day);
            return set != null ? set : drawn(server.getSeed(), day);
        }
        return shared.getOrDefault(day, Day.calm(day));
    }

    public static synchronized Day drawn(long seed, int day) {
        if (day < 0) {
            return draw(seed, day, 0);
        }
        List<Day> days = DRAWN.computeIfAbsent(seed, key -> new ArrayList<>());
        while (days.size() <= day) {
            days.add(draw(seed, days.size(), days.isEmpty() ? 0 : days.getLast().driftC()));
        }
        return days.get(day);
    }

    private static Day draw(long seed, int day, double previousDriftC) {
        Balance.Weather rules = Balance.get().weather();
        RandomGenerator random = random(seed, day);
        LocalDate date = Calendar.date(day);
        double carried = rules.driftCarried();
        double drift = carried * previousDriftC + Math.sqrt(1 - carried * carried) * driftSpreadC(date) * random.nextGaussian();
        boolean wet = random.nextDouble() < usualChance(date);
        int hours = rules.shortestSpellHours() + (int) (random.nextDouble() * (rules.longestSpellHours() - rules.shortestSpellHours() + 1));
        int from = (int) (random.nextDouble() * (HOURS - hours + 1));
        int intensity = pick(rules.intensityShares(), random.nextDouble()) + Day.LIGHT;
        int month = date.getMonthValue();
        boolean thunder = random.nextDouble() < rules.stormShare() && wet && month >= rules.stormFirstMonth() && month <= rules.stormLastMonth();
        double cloud = wet ? rules.overcast() + random.nextDouble() * (1 - rules.overcast()) : random.nextDouble() * rules.overcast();
        double mean = wet ? rules.wetWindKmh() : rules.dryWindKmh();
        double wind = Math.min(rules.highestWindKmh(), mean / RAYLEIGH_MEAN * Math.sqrt(-Math.log(1 - random.nextDouble())));
        int windFrom = random.nextInt(COMPASS);
        return new Day(day, (float) drift, (float) cloud, (float) wind, windFrom, wet ? intensity : Day.DRY, wet ? from : 0, wet ? from + hours : 0, thunder);
    }

    private static int pick(List<Double> shares, double roll) {
        double sum = 0;
        for (int i = 0; i < shares.size(); i++) {
            sum += shares.get(i);
            if (roll < sum) {
                return i;
            }
        }
        return shares.size() - 1;
    }

    private static double driftSpreadC(LocalDate date) {
        Balance.Weather rules = Balance.get().weather();
        List<Double> lows = Balance.get().climate().lowsC();
        double warmest = Collections.max(lows);
        double coldest = Collections.min(lows);
        double cold = (warmest - Climate.monthly(lows, date)) / (warmest - coldest);
        return rules.driftSummerC() + (rules.driftWinterC() - rules.driftSummerC()) * cold;
    }

    public static double usualChance(LocalDate date) {
        return Climate.monthly(PRECIPITATION_SHARES, date);
    }

    public static RandomGenerator random(long seed, long index) {
        return new SplittableRandom(RandomSupport.mixStafford13(seed ^ index * GOLDEN));
    }

    public static double driftC(Level level, int day, double hour) {
        return blend(level, day, hour, Day::driftC);
    }

    public static double cloud(Level level, int day, double hour) {
        return blend(level, day, hour, Day::cloud);
    }

    private static double blend(Level level, int day, double hour, ToDoubleFunction<Day> value) {
        boolean morning = hour < EVENING;
        int from = morning ? day - 1 : day;
        double share = (morning ? hour + EVENING : hour - EVENING) / HOURS;
        double before = value.applyAsDouble(day(level, from));
        return before + (value.applyAsDouble(day(level, from + 1)) - before) * share;
    }

    public static double wetShare(Day day) {
        return day.wet() ? Balance.get().weather().wetShares().get(day.intensity() - Day.LIGHT) : 0;
    }

    public static void set(Day day) {
        SET.put(day.day(), day);
    }

    public static void unset(int day) {
        SET.remove(day);
    }

    public static synchronized void forget() {
        SET.clear();
        DRAWN.clear();
    }

    public static List<Day> around(Level level, int day) {
        return IntStream.rangeClosed(day - SHARED_BEFORE, day + SHARED_AFTER).mapToObj(each -> day(level, each)).toList();
    }

    public static void share(List<Day> days) {
        Map<Integer, Day> byDay = new HashMap<>();
        days.forEach(day -> byDay.put(day.day(), day));
        shared = Map.copyOf(byDay);
    }

    public static void unshare() {
        shared = Map.of();
    }
}
