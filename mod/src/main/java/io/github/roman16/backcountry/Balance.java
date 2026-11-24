package io.github.roman16.backcountry;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Balance(BearSpray bearSpray, Body body, Climate climate, Conditions conditions, DropOff dropOff, Energy energy, Healing healing, Map<String, Double> heat,
        Map<String, Double> insulation, Kit kit, Load load, Mining mining, PlantFibre plantFibre, Survivor survivor, Warmth warmth, Water water, Work work, World world, Year year) {
    private static final Pattern WORD_START = Pattern.compile("([A-Z])");
    private static final Balance LOADED = read();

    public record BearSpray(int blindSeconds, int bursts, double coneDegrees, int fleeSeconds, double pointBlankBlocks, double reachBlocks) {}

    public record Body(double arrivalKg, double digestKcalPerHour, double extractionBmi, double heightM, double kcalPerKg, double restingKcalPerKgDay, double stomachKcal,
            double vitaminCMgDay, double vitaminCStoreDays) {}

    public record Climate(double biomeCPerTemperature, double biomeReferenceTemperature, double coldestHour, List<Double> highsC, double lapseCPerBlock, List<Double> lowsC,
            int seaLevel) {}

    public record Conditions(double bareSkinC, double blistersWetBlocks, double blistersWetness, double burnsDays, double dryWetness, double exhaustedAwakeDays,
            double exhaustedEnergy, double feverBurn, double frostCoreC, double frostStageHours, double gutAbsorb, double gutDays, double gutInfectionChance, double gutWater,
            double infectionChance, double infectionDays, double parasitesAbsorb, double parasitesChance, double poisoningDays, double scurvyBurn, double vomitL) {}

    public record DropOff(int highestLakeY, int inlandBlocks, int lakeCells, int searchBlocks, int shoreRiseBlocks, int stepBlocks) {}

    public record Energy(double awakeHoursToEmpty, int blackoutSeconds, double digestKcalToFull, double sleepHoursToFull, double slowBelow, double slowSpeed, double sprintBelow,
            double stumbleBelow, int stumbleSeconds, double stumbleShare, double wakeEnergy, double workKcalToEmpty) {}

    public record Healing(double above, double asleepFactor, double hoursPerHeart, double scurvyFactor) {}

    public record Kit(int pilotBread) {}

    public record Load(double blockKg, double capacityKg, double clothingKg, double foodKgPerKcal, double itemKg, double logKg, int packRows, double stoneKg, double toolKg,
            int warnSeconds) {}

    public record Mining(int openBlocks, double speed) {}

    public record PlantFibre(double chance) {}

    public record Survivor(int quietSeconds) {}

    public record Warmth(double baseClo, double comfortC, double comfortSpanC, double coolingShare, double coreC, double coreCPerHour, double dryPerHour, double fireDryPerHour,
            int heatReachBlocks, double rainWetPerHour, int roofBlocks, double shelterC, double shelterDryPerHour, double shiverCoreC, double shiverKcalPerHourC,
            double sweatCoreC, double sweatLPerHour, double sweatWetPerHour, double warmCPerClo, double wetInsulationLoss) {}

    public record Water(int boilSeconds, double bottleL, int knockoutSeconds, double needLDay, double potL, double sipL, float thirstDamage) {}

    public record Work(double attackKcal, double chopKcal, double digKcal, double jumpKcal, double mineKcal, double overloadKcalPerKgHour, double placeKcal,
            double sprintKcalPerBlock, double swimKcalPerBlock, double walkKcalPerBlock) {}

    public record World(int acrossBlocks, double detailShare, int detailWavelengthBlocks, double dryShare, int edgeBlocks, double highest, double innerShare,
            double landContinentalness, double lift, double outerShare, double seaFloor, double sink, double warpShare, int warpWavelengthBlocks) {}

    public record Year(int days, int firstDayInTerm, int firstTerm, int startDay, int startMonth, int termDays) {}

    public static Balance get() {
        return LOADED;
    }

    private static Balance read() {
        try (InputStream stream = Balance.class.getResourceAsStream("/backcountry/balance.toml")) {
            if (stream == null) {
                throw new IllegalStateException("backcountry/balance.toml is missing from the mod jar");
            }
            return (Balance) record(Balance.class, new TomlParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)), "");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Object record(Class<?> type, Config table, String path) {
        RecordComponent[] components = type.getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            String key = snake(components[i].getName());
            String at = path.isEmpty() ? key : path + "." + key;
            Object raw = table.get(List.of(key));
            if (raw == null) {
                throw new IllegalStateException("balance.toml has no " + at);
            }
            types[i] = components[i].getType();
            values[i] = value(components[i].getType(), components[i].getGenericType(), raw, at);
        }
        try {
            return type.getDeclaredConstructor(types).newInstance(values);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("balance.toml: " + path + " does not fit " + type.getSimpleName(), exception);
        }
    }

    private static Object value(Class<?> type, Type generic, Object raw, String at) {
        if (type == double.class) {
            return ((Number) raw).doubleValue();
        }
        if (type == float.class) {
            return ((Number) raw).floatValue();
        }
        if (type == int.class) {
            return ((Number) raw).intValue();
        }
        if (type == List.class) {
            return ((List<?>) raw).stream().map(item -> ((Number) item).doubleValue()).toList();
        }
        if (type == Map.class && generic instanceof ParameterizedType) {
            Map<String, Double> map = new TreeMap<>();
            ((Config) raw).entrySet().forEach(entry -> map.put(entry.getKey(), ((Number) entry.getValue()).doubleValue()));
            return Map.copyOf(map);
        }
        if (type.isRecord()) {
            return record(type, (Config) raw, at);
        }
        throw new IllegalStateException("balance.toml: " + at + " has a type Balance cannot read");
    }

    private static String snake(String name) {
        Matcher matcher = WORD_START.matcher(name);
        return matcher.replaceAll(result -> "_" + result.group(1).toLowerCase(java.util.Locale.ROOT));
    }
}
