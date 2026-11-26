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

public record Balance(Alerts alerts, Arrows arrows, Bait bait, BearSpray bearSpray, Bears bears, Berries berries, Body body, BowDrill bowDrill, Cache cache, Climate climate,
        Conditions conditions, Cooking cooking, Director director, DropOff dropOff, Effort effort, Energy energy, Farming farming, Fire fire, Firewood firewood, Fish fish, Forage forage,
        Headlamp headlamp, Healing healing, Map<String, Double> heat, Hunting hunting, Ice ice, Map<String, Double> insulation, Kit kit, Lights lights, Load load, Map<String, Double> met, Mice mice,
        Mining mining, Mosquitoes mosquitoes, PlantFibre plantFibre, Raft raft, Rations rations, Seasons seasons, Skills skills, Sky sky, Sled sled, Smoker smoker, Snow snow, Spoilage spoilage,
        Survivor survivor, Tasks tasks, Traps traps, Warmth warmth, Walking walking, Water water, Weather weather, World world, Year year) {
    private static final Pattern WORD_START = Pattern.compile("([A-Z])");
    private static final Balance LOADED = read();

    public record Alerts(double chance, double wastingBmi, double weightStepKg) {}

    public record Arrows(double bluntAnimals, double bluntBlocks, double bluntDamage, double boneAnimals, double boneBlocks, double boneDamage, double broadheadAnimals,
            double broadheadBlocks, double broadheadDamage) {}

    public record Bait(double cricketChance, List<Double> cricketMonths, double wormChance, double wormHours) {}

    public record BearSpray(int blindSeconds, int bursts, double coneDegrees, int fleeSeconds, double pointBlankBlocks, double reachBlocks) {}

    public record Bears(double bluffShare, double bluffStopBlocks, double chargeBlocks, double chargeChance, double cubBlocks, double cubShare, double defendBlocks,
            List<Double> fallMonths, double fleeBlocks, int leftoverGrams, double leftoverHours, int mostCubs, List<Double> salmonMonths, List<Double> springMonths,
            double surpriseBlocks) {}

    public record Berries(List<Double> baneberryMonths, List<Double> blueberryMonths, List<Double> highbushCranberryMonths, List<Double> lingonberryMonths,
            List<Double> salmonberryMonths) {}

    public record Body(double arrivalKg, double bmrKcalPerKgHour, double digestKcalPerHour, double extractionBmi, double heightM, double kcalPerKg, double sleepKcalPerKgHour,
            double stomachKcal, double thermicShare, double vitaminCMgDay, double vitaminCStoreDays) {}

    public record BowDrill(double blowGlow, double coolingPerSecond, double flamePerSecond, double glowBand, double glowLossPerSecond, double strokeHeat,
            int uses, double wetCooling) {}

    public record Cache(double scentKcal) {}

    public record Climate(double biomeCPerTemperature, double biomeReferenceTemperature, double coldestHour, List<Double> highsC, double lapseCPerBlock, List<Double> lowsC,
            int seaLevel) {}

    public record Conditions(double bareSkinC, double blistersWetBlocks, double blistersWetness, double burnsDays, double dryWetness, double exhaustedAwakeDays,
            double exhaustedEnergy, double feverBurn, double frostCoreC, double frostStageHours, double gutAbsorb, double gutDays, double gutInfectionChance, double gutWater,
            double infectionChance, double infectionDays, double parasitesAbsorb, double parasitesChance, double poisoningDays, double scurvyBurn, double vomitL) {}

    public record Cooking(double potL, double servingL, double stewHours) {}

    public record Director(int checkSeconds, int countBlocks, double eventShare, int farBlocks, int nearBlocks, double rainShare, double scentShare, int tries,
            List<Double> whiteMonths) {}

    public record DropOff(int highestLakeY, int inlandBlocks, int lakeCells, int searchBlocks, int shoreRiseBlocks, int stepBlocks) {}

    public record Effort(double attackMinutes, double buildMinutes, double dressMinutes, double scrapeMinutes, double splitMinutes, double strokeMinutes) {}

    public record Energy(double awakeHoursToEmpty, int blackoutSeconds, double digestKcalToFull, double sleepHoursToFull, double slowBelow, double slowSpeed, double sprintBelow,
            double stumbleBelow, int stumbleSeconds, double stumbleShare, double wakeEnergy, double workKcalPerKgToEmpty) {}

    public record Farming(int firstMonth, double germination, double growthShare, int lastMonth, double warmC) {}

    public record Fire(double bankedHours, double coalsC, double hardwoodHeat, double hardwoodHours, double hotBurn, double hotC, double kindlingHeat,
            double kindlingHours, int kindlingSticks, double lowBurn, double lowC, double mostCoalsHours, double rainBurn, double softwoodHours, int sparkBlocks,
            double sparkChance, double steadyC) {}

    public record Firewood(double catches, double dryPerDay, double freshHardwood, double freshSoftwood, double frozenDrying, int pilePieces) {}

    public record Fish(double cookedDays, double fatGPerKg, double frozenC, double frozenDays, double kcalPerKg, double proteinGPerKg, double rawDays,
            List<Double> spawnedOutMonths, double vitaminCMgPerKg) {}

    public record Forage(int needlesALeaf, double sapChance, double sapKcalPerL, double sapLPerStep, List<Double> sapMonths, double syrupL, int syrupSeconds,
            double teaVitaminCMgPerL) {}

    public record Headlamp(double chargeHours, double crankHoursPerSecond, double dimFromC, double dimmestC, double dimmestShare, int light) {}

    public record Healing(double above, double asleepFactor, double hoursPerHeart, double scurvyFactor) {}

    public record Hunting(double bleedHours, double bloodChance, double bloodDays, double carryKg, double dressClicksPerKg, double heavyPower,
            double organKg, double strongBmi, double strongEnergy, double woundShare) {}

    public record Ice(double freezeBelowC, double thawAboveC) {}

    public record Kit(int pilotBread) {}

    public record Lights(double lanternHours, double torchHours) {}

    public record Load(double blockKg, double capacityKg, double clothingKg, double firewoodKg, double foodKgPerKcal, double itemKg, double logKg, int packRows, double stoneKg, double toolKg,
            int warnSeconds) {}

    public record Mice(double chance, double kcal) {}

    public record Mining(int openBlocks, double speed) {}

    public record Mosquitoes(double calmKmh, double energyPerHour, int firstHour, int lastHour, List<Double> months, double sleepShare, int waterBlocks) {}

    public record PlantFibre(double chance) {}

    public record Raft(double calmKmh, double fallChance) {}

    public record Rations(int dawnHour, int duskHour, double feastKcal, double leanKcal, double normalKcal, int reachBlocks) {}

    public record Seasons(List<Double> feedingMonths, List<Double> rutMonths) {}

    public record Skills(double levelShare, List<Double> usesToLevel) {}

    public record Sky(int cloudHeight, double moonFull, double moonNew, double moonPartial) {}

    public record Sled(double groundDrag, double groundFriction, double selfKg, double snowDrag, double snowFriction) {}

    public record Smoker(double batchHours, double burnRate, double risingHours) {}

    public record Snow(int deepLayers, double frostbiteBlocks, double slowShare) {}

    public record Spoilage(double doublingC, double frozenBelowC, double frozenShare, double referenceC, double smokedLife) {}

    public record Survivor(int quietSeconds) {}

    public record Tasks(double dryFirewood, double foodDays, int shown) {}

    public record Traps(double baitShare, double fishPerDay, double fishShare, int miceBlocks, double share) {}

    public record Warmth(double baseClo, double coldShare, double comfortC, double comfortSpanC, double coolingShare, double coreC, double coreCPerHour, double dryPerHour,
            double fireDryPerHour, int heatReachBlocks, double rainWetPerHour, int roofBlocks, double shelterC, double shelterDryPerHour, double shiverCoreC,
            double shiverKcalPerHourC, double shiverMostBmr, double sweatCoreC, double sweatLPerHour, double sweatWetPerHour, double veryColdShare, double warmCPerClo,
            double wetInsulationLoss) {}

    public record Walking(double feetShare, double iceEta, double loadWPerKg, double motionWPerKg, double sneakBlocksPerTick, double sneakMS, double snowDeepestCm,
            double snowEta, double snowEtaPerCm, double snowshoeSink, double sprintBlocksPerTick, double standingWPerKg, double steepestGrade, double swimBlocksPerTick,
            double walkBlocksPerTick, double walkMS, double windKmhDoubled) {}

    public record Water(int boilSeconds, double bottleL, int knockoutSeconds, double needLDay, double potL, double sipL, float thirstDamage) {}

    public record Weather(int blizzardFogBlocks, double blizzardWhiteout, double blizzardWindKmh, double breathBelowC, double clearBelow, double cloudyAbove,
            double cloudySwing, double driftCarried, double driftSummerC, double driftWinterC, double dryWindKmh, double forecastChanceNoise, double forecastErrorCPerDay,
            double forecastSkillLostPerDay, double forecastWindErrorKmhPerDay, double highestWindKmh, List<Double> intensityShares, int longestSpellHours,
            double overcast, List<Double> precipitationDays, int shortestSpellHours, double snowBelowC, int stormFirstMonth, int stormLastMonth, double stormShare,
            List<Double> wetShares, double wetWindKmh) {}

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
