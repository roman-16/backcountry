package io.github.roman16.backcountry.weather;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

public final class Forecast {
    public static final int DAYS_AHEAD = 3;
    private static final long FORECAST_STREAM = 0x0F0BECA57L;
    private static final int PERCENT = 100;
    private static final int PERCENT_STEP = 10;

    private Forecast() {}

    public static WeatherView view(ServerPlayer player, Body body) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        Biome biome = level.getBiome(pos).value();
        int day = Calendar.day(level);
        double hour = Climate.hour(level);
        Day today = Weather.day(level, day);
        double air = Climate.airC(level, biome, pos, day, hour);
        double feels = body.sheltered ? air : Climate.feelsC(air, today.windKmh());
        Optional<Kind> falling = Optional.empty();
        boolean now = false;
        if (today.wet() && hour < today.toHour()) {
            now = hour >= today.fromHour();
            double at = now ? air : Climate.airC(level, biome, pos, day, today.fromHour());
            falling = Optional.of(Kind.falling(at, today.windKmh(), today.thunder()));
        }
        DoubleUnaryOperator truth = each -> Climate.airC(level, biome, pos, day, each);
        List<Outlook> outlooks = IntStream.rangeClosed(1, DAYS_AHEAD).mapToObj(ahead -> outlook(level, day, ahead, pos)).toList();
        return new WeatherView((float) air, (float) feels, falling, today.intensity(), today.fromHour(), today.toHour(), now, today.windKmh(), today.windFrom(),
                today.cloud(), (float) highest(truth, 0, Weather.HOURS), (float) lowest(truth, 0, Weather.HOURS), outlooks);
    }

    public static Outlook outlook(ServerLevel level, int day, int ahead, BlockPos pos) {
        Balance.Weather rules = Balance.get().weather();
        int target = day + ahead;
        Day truth = Weather.day(level, target);
        RandomGenerator random = Weather.random(level.getSeed() ^ FORECAST_STREAM, (long) day * (DAYS_AHEAD + 1) + ahead);
        double skill = Math.max(0, 1 - rules.forecastSkillLostPerDay() * ahead);
        double drift = truth.driftC() + rules.forecastErrorCPerDay() * ahead * random.nextGaussian();
        double usual = Weather.usualChance(Calendar.date(target));
        double noise = (random.nextDouble() - 0.5) * rules.forecastChanceNoise() * (1 - skill);
        double chance = Math.clamp(skill * (truth.wet() ? 1 : 0) + (1 - skill) * usual + noise, 0, 1);
        double usualCloud = usual * (1 + rules.overcast()) / 2 + (1 - usual) * rules.overcast() / 2;
        double cloud = skill * truth.cloud() + (1 - skill) * usualCloud;
        double wind = Math.clamp(truth.windKmh() + rules.forecastWindErrorKmhPerDay() * ahead * random.nextGaussian(), 0, rules.highestWindKmh());
        Biome biome = level.getBiome(pos).value();
        DoubleUnaryOperator guess = hour -> Climate.airC(biome, pos, target, hour, drift, cloud);
        double high = highest(guess, 0, Weather.HOURS);
        double low = lowest(guess, 0, Weather.HOURS);
        int percent = (int) Math.round(chance * PERCENT / PERCENT_STEP) * PERCENT_STEP;
        return new Outlook(target, Kind.sky(cloud), Kind.falling((high + low) / 2, wind, truth.thunder()), percent, (float) high, (float) low, (float) wind, truth.windFrom());
    }

    public static double lowest(DoubleUnaryOperator air, int fromHour, int toHour) {
        return IntStream.rangeClosed(fromHour, toHour).mapToDouble(air::applyAsDouble).min().orElseThrow();
    }

    public static double highest(DoubleUnaryOperator air, int fromHour, int toHour) {
        return IntStream.rangeClosed(fromHour, toHour).mapToDouble(air::applyAsDouble).max().orElseThrow();
    }
}
