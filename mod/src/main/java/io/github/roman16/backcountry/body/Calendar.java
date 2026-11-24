package io.github.roman16.backcountry.body;

import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import com.teamtea.eclipticseasons.common.core.solar.SolarDataManager;
import io.github.roman16.backcountry.Balance;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Calendar {
    private static final int DAYS_A_YEAR = 365;
    private static final int ANY_YEAR = 2001;

    private Calendar() {}

    public static int firstSolarDay() {
        Balance.Year year = Balance.get().year();
        return (year.firstTerm() - 1) * year.termDays() + year.firstDayInTerm() - 1;
    }

    public static void begin(ServerLevel level) {
        SolarDataManager solar = SolarDataManager.get(level);
        solar.setSolarTermsDay(firstSolarDay());
        solar.sendAndUpdate(level);
    }

    public static int day(Level level) {
        return EclipticSeasonsApi.getInstance().getSolarDays(level) - firstSolarDay();
    }

    public static int year(int day) {
        return Math.floorDiv(day, Balance.get().year().days()) + 1;
    }

    public static int dayOfYear(int day) {
        return Math.floorMod(day, Balance.get().year().days()) + 1;
    }

    public static LocalDate date(int day) {
        Balance.Year year = Balance.get().year();
        LocalDate start = LocalDate.of(ANY_YEAR, year.startMonth(), year.startDay());
        return start.plusDays((long) Math.floor((dayOfYear(day) - 1) * (double) DAYS_A_YEAR / year.days()));
    }

    public static String dateText(int day) {
        LocalDate date = date(day);
        return date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public static Component label(int day) {
        Component dated = Component.translatable("calendar.backcountry.day", dayOfYear(day), dateText(day));
        return year(day) > 1 ? Component.translatable("calendar.backcountry.year_day", year(day), dated) : dated;
    }
}
