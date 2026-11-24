package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.weather.Weather;
import java.time.LocalDate;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class Climate {
    public static final double CALM_KMH = 4.8;
    private static final double WIND_CHILL_HIGHEST_C = 10;
    private static final double WIND_CHILL_BASE = 13.12;
    private static final double WIND_CHILL_AIR = 0.6215;
    private static final double WIND_CHILL_WIND = 11.37;
    private static final double WIND_CHILL_BOTH = 0.3965;
    private static final double WIND_CHILL_POWER = 0.16;
    private static final int TICKS_A_DAY = 24000;
    private static final double TICKS_AN_HOUR = 1000;
    private static final int MIDNIGHT_HOUR_OFFSET = 6;

    private Climate() {}

    public static double hour(Level level) {
        return (level.getDayTime() % TICKS_A_DAY) / TICKS_AN_HOUR;
    }

    public static double airC(Level level, BlockPos pos, int day) {
        return airC(level, level.getBiome(pos).value(), pos, day, hour(level));
    }

    public static double airC(Level level, Biome biome, BlockPos pos, int day, double hour) {
        return airC(biome, pos, day, hour, Weather.driftC(level, day, hour), Weather.cloud(level, day, hour));
    }

    public static double airC(Biome biome, BlockPos pos, int day, double hour, double driftC, double cloud) {
        Balance.Climate climate = Balance.get().climate();
        LocalDate date = Calendar.date(day);
        double high = monthly(climate.highsC(), date);
        double low = monthly(climate.lowsC(), date);
        double clock = (hour + MIDNIGHT_HOUR_OFFSET) % Weather.HOURS;
        double swing = -Math.cos(2 * Math.PI * (clock - climate.coldestHour()) / Weather.HOURS) * (1 - cloud * (1 - Balance.get().weather().cloudySwing()));
        double shift = (biome.getBaseTemperature() - climate.biomeReferenceTemperature()) * climate.biomeCPerTemperature();
        double height = -(pos.getY() - climate.seaLevel()) * climate.lapseCPerBlock();
        return (high + low) / 2 + swing * (high - low) / 2 + driftC + shift + height;
    }

    public static boolean snows(Level level, Biome biome, BlockPos pos) {
        return airC(level, biome, pos, Calendar.day(level), hour(level)) <= Balance.get().weather().snowBelowC();
    }

    public static double feelsC(double airC, double windKmh) {
        if (airC > WIND_CHILL_HIGHEST_C || windKmh <= CALM_KMH) {
            return airC;
        }
        double wind = Math.pow(windKmh, WIND_CHILL_POWER);
        return Math.min(airC, WIND_CHILL_BASE + WIND_CHILL_AIR * airC - WIND_CHILL_WIND * wind + WIND_CHILL_BOTH * airC * wind);
    }

    public static double monthly(List<Double> normals, LocalDate date) {
        double position = date.getMonthValue() - 1 + (date.getDayOfMonth() - 1.0) / date.lengthOfMonth() - 0.5;
        int from = (int) Math.floor(position);
        double share = position - from;
        double before = normals.get(Math.floorMod(from, normals.size()));
        double after = normals.get(Math.floorMod(from + 1, normals.size()));
        return before + (after - before) * share;
    }
}
