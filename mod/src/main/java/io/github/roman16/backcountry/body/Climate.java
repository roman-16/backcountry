package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import java.time.LocalDate;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class Climate {
    private static final double HOURS = 24;
    private static final int TICKS_A_DAY = 24000;
    private static final int TICKS_AN_HOUR = 1000;
    private static final int MIDNIGHT_HOUR_OFFSET = 6;

    private Climate() {}

    public static double airC(Level level, BlockPos pos, int day) {
        Balance.Climate climate = Balance.get().climate();
        LocalDate date = Calendar.date(day);
        double high = monthly(climate.highsC(), date);
        double low = monthly(climate.lowsC(), date);
        double hour = ((level.getDayTime() % TICKS_A_DAY) / (double) TICKS_AN_HOUR + MIDNIGHT_HOUR_OFFSET) % HOURS;
        double swing = -Math.cos(2 * Math.PI * (hour - climate.coldestHour()) / HOURS);
        double biome = (level.getBiome(pos).value().getBaseTemperature() - climate.biomeReferenceTemperature()) * climate.biomeCPerTemperature();
        double height = -(pos.getY() - climate.seaLevel()) * climate.lapseCPerBlock();
        return (high + low) / 2 + swing * (high - low) / 2 + biome + height;
    }

    private static double monthly(List<Double> normals, LocalDate date) {
        double position = date.getMonthValue() - 1 + (date.getDayOfMonth() - 1.0) / date.lengthOfMonth() - 0.5;
        int from = (int) Math.floor(position);
        double share = position - from;
        double before = normals.get(Math.floorMod(from, normals.size()));
        double after = normals.get(Math.floorMod(from + 1, normals.size()));
        return before + (after - before) * share;
    }
}
