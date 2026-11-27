package io.github.roman16.backcountry.guide;

import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.hunt.Migration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class Year {
    public static final int MONTHS = 12;
    public static final int NONE = 0;
    public static final int QUIET = 1;
    public static final int FULL = 2;
    private static final double TICKS_AN_HOUR = 1000;
    private static final ResourceLocation GRIZZLY = ResourceLocation.fromNamespaceAndPath("alexsmobs", "grizzly_bear");

    private Year() {}

    public record Month(int month, double highC, double lowC, double daylightHours, double wetDays) {}

    public record Row(String id, int[] months) {
        public int at(int month) {
            return months[month - 1];
        }
    }

    public static List<Month> climate(Biome biome, BlockPos pos) {
        Balance.Climate climate = Balance.get().climate();
        double local = Climate.localC(biome, pos);
        List<Month> months = new ArrayList<>();
        for (int month = 1; month <= MONTHS; month++) {
            months.add(new Month(month, climate.highsC().get(month - 1) + local, climate.lowsC().get(month - 1) + local, daylightHours(month),
                    Balance.get().weather().precipitationDays().get(month - 1)));
        }
        return months;
    }

    public static double daylightHours(int month) {
        Balance.Year year = Balance.get().year();
        double ticks = 0;
        int days = 0;
        for (int day = 0; day < year.days(); day++) {
            if (Calendar.date(day).getMonthValue() == month) {
                int term = Math.floorMod(Math.floorDiv(day + Calendar.firstSolarDay(), year.termDays()), SolarTerm.collectValidValues().length);
                ticks += SolarTerm.collectValidValues()[term].getDayTime();
                days++;
            }
        }
        return days == 0 ? 0 : ticks / days / TICKS_AN_HOUR;
    }

    public static List<Row> events(Biome biome, BlockPos pos) {
        Balance balance = Balance.get();
        List<Month> climate = climate(biome, pos);
        List<Row> rows = new ArrayList<>();
        rows.add(row("berries", month -> berries(month)));
        rows.add(row("salmon_run", month -> within(month, balance.fish().runMonths()) ? FULL : NONE));
        List<Integer> bears = Animal.of(BuiltInRegistries.ENTITY_TYPE.get(GRIZZLY)).map(Animal::months).orElse(List.of());
        rows.add(row("bears", month -> bears.size() < 2 || Seasons.within(month, bears.get(0), bears.get(1)) ? FULL : QUIET));
        rows.add(row("moose_rut", month -> within(month, balance.seasons().rutMonths()) ? FULL : NONE));
        rows.add(row("calves", month -> within(month, balance.moose().calfMonths()) ? FULL : NONE));
        rows.add(row("mosquitoes", month -> within(month, balance.mosquitoes().months()) ? FULL : NONE));
        rows.add(row("lake_ice", month -> climate.get(month - 1).lowC() <= balance.ice().freezeBelowC() ? FULL
                : within(month, balance.ice().breakupMonths()) ? QUIET : NONE));
        Balance.Migration migration = balance.migration();
        rows.add(row("caribou", month -> month == migration.caribouSpringMonth() || month == migration.caribouAutumnMonth() ? FULL : NONE));
        List<Integer> geese = Animal.of(BuiltInRegistries.ENTITY_TYPE.get(Migration.GOOSE)).map(Animal::months).orElse(List.of());
        rows.add(row("geese", month -> month == migration.geeseSpringMonth() || month == migration.geeseAutumnMonth() ? FULL
                : geese.size() >= 2 && Seasons.within(month, geese.get(0), geese.get(1)) ? QUIET : NONE));
        rows.add(row("birch_sap", month -> within(month, balance.forage().sapMonths()) ? FULL : NONE));
        return rows;
    }

    private static int berries(int month) {
        int ripest = NONE;
        for (Block bush : List.<Block>of(Things.SALMONBERRY_BUSH.get(), Things.BLUEBERRY_BUSH.get(), Blocks.SWEET_BERRY_BUSH, Things.HIGHBUSH_CRANBERRY_BUSH.get())) {
            int state = Seasons.ripest(month, bush);
            ripest = Math.max(ripest, state == Seasons.RIPE ? FULL : state == Seasons.KEPT ? QUIET : NONE);
        }
        return ripest;
    }

    private static boolean within(int month, List<Double> range) {
        return Hunting.months(month, range);
    }

    private static Row row(String id, IntUnaryOperator at) {
        int[] months = new int[MONTHS];
        for (int month = 1; month <= MONTHS; month++) {
            months[month - 1] = at.applyAsInt(month);
        }
        return new Row(id, months);
    }
}
