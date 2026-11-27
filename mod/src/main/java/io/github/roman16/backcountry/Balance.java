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

public record Balance(Alerts alerts, Arrows arrows, Bait bait, BearSpray bearSpray, Bears bears, Berries berries, Binoculars binoculars, Body body, BowDrill bowDrill, Map<String, Double> bulk, Cache cache, Canoe canoe, Climate climate, Coast coast,
        Conditions conditions, Cooking cooking, Director director, DropOff dropOff, Drying drying, Effort effort, Energy energy, Fire fire, Firewood firewood, Fish fish, FishWheel fishWheel, Forage forage,
        Graphics graphics, Headlamp headlamp, Healing healing, Map<String, Double> heat, Hunting hunting, Ice ice, Map<String, Double> insulation, Kit kit, Lights lights, Load load, Locator locator, Map<String, Double> met, Mice mice,
        Migration migration, Mining mining, MooseCharges moose, Mosquitoes mosquitoes, PlantFibre plantFibre, Raft raft, Raids raids, Rations rations, Rooms rooms, Seasons seasons, Skills skills, Sky sky, Sled sled, Smoker smoker, Snow snow, Spirits spirits, Spoilage spoilage,
        Survey survey, Survivor survivor, Tasks tasks, Tracking tracking, Trail trail, Traps traps, Warmth warmth, Walking walking, Water water, Weather weather, Map<String, Double> weight, World world, Year year) {
    private static final Pattern WORD_START = Pattern.compile("([A-Z])");
    private static final Balance LOADED = read();

    public record Alerts(double chance, double wastingBmi, double weightStepKg) {}

    public record Arrows(double bluntAnimals, double bluntBlocks, double bluntDamage, double boneAnimals, double boneBlocks, double boneDamage, double broadheadAnimals,
            double broadheadBlocks, double broadheadDamage) {}

    public record Bait(double cricketChance, List<Double> cricketMonths, double dryWormChance, double rainWormChance, double wormHours) {}

    public record BearSpray(int blindSeconds, int bursts, double coneDegrees, int fleeSeconds, double pointBlankBlocks, double reachBlocks) {}

    public record Bears(double bluffShare, double bluffStopBlocks, double chargeBlocks, double chargeChance, double cubBlocks, double cubShare, double defendBlocks,
            List<Double> fallMonths, double fleeBlocks, int leftoverGrams, double leftoverHours, int mostCubs, List<Double> springMonths,
            double surpriseBlocks) {}

    public record Berries(List<Double> baneberryMonths, List<Double> blueberryMonths, List<Double> highbushCranberryMonths, int lingonberryKeptUntil, List<Double> lingonberryMonths,
            List<Double> salmonberryMonths) {}

    public record Binoculars(double magnification, double studyBlocks, int studyTicks) {}

    public record Body(double arrivalKg, double bmrKcalPerKgHour, double digestKcalPerHour, double extractionBmi, double heightM, double kcalPerKg, double sleepKcalPerKgHour,
            double stomachKcal, double thermicShare, double vitaminCMgDay, double vitaminCStoreDays) {}

    public record BowDrill(double blowGlow, double coolingPerSecond, double flamePerSecond, double glowBand, double glowLossPerSecond, double strokeHeat,
            int uses, double wetCooling) {}

    public record Cache(double scentKcal) {}

    public record Canoe(double calmKmh, double fallChance, double speedShare) {}

    public record Climate(double biomeCPerTemperature, double biomeReferenceTemperature, double coldestHour, List<Double> highsC, double lapseCPerBlock, List<Double> lowsC,
            int seaLevel) {}

    public record Coast(double clamChance, double driftwoodPerDay, double galeKmh, double galeShare, int most, double musselsPerDay, int nestMonth, double nestsPerDay,
            int reachBlocks, int tries, int unseenBlocks) {}

    public record Conditions(double bareSkinC, double blistersWetBlocks, double blistersWetness, double burnsDays, double dryWetness, double exhaustedAwakeDays, double exhaustedEnergy, double feverBurn, double frostStageHours, double gutAbsorb, double gutDays, double gutLakeChance, double gutOnsetDays, double gutStreamChance, double gutWater, double hypothermiaCoreC, double infectionChance, double infectionDays, double numbOnsetHours, double parasitesAbsorb, double parasitesChance, double poisoningDays, double rabbitAbsorb, int rabbitCureDays, double rabbitFatShare, int rabbitOnsetDays, double rabbitProteinG, double rabbitWater, double scurvyBurn, double severeHypothermiaCoreC, double shellfishChance, double shellfishHours, List<Double> shellfishMonths, double shellfishSummerChance, double vomitL) {}

    public record Cooking(double potL, double servingL, double stewHours) {}

    public record Director(int checkSeconds, int countBlocks, double eventShare, int farBlocks, List<Double> hareCycle, int nearBlocks, double rainShare, double scentShare, int tries, List<Double> whiteMonths) {}

    public record DropOff(int shoreRiseBlocks, int stepBlocks) {}

    public record Drying(double days, double life, double saltPerL, int saltSeconds, double saltedLife, double smokeFactor, double waterShare, double weightShare) {}

    public record Effort(double attackMinutes, double buildMinutes, double dressMinutes, double scrapeMinutes, double splitMinutes, double strokeMinutes) {}

    public record Energy(double awakeHoursToEmpty, int blackoutSeconds, double digestKcalToFull, double sleepHoursToFull, double slowBelow, double slowSpeed, double sprintBelow,
            double stumbleBelow, int stumbleSeconds, double stumbleShare, double wakeEnergy, double workKcalPerKgToEmpty) {}

    public record Fire(double bankedHours, double branchBlocks, double coalsC, double fireplaceBurn, double fungusHours, double hardwoodHeat, double hardwoodHours, double hotBurn, double hotC, double kindlingHeat, double kindlingHours, int kindlingSticks, double lowBurn, double lowC, double mostCoalsHours, double rainBurn, double smotherPerHour, double softwoodHours, int sparkBlocks, double sparkChance, double steadyC, double touchSeconds) {}

    public record Firewood(double catches, double dryPerDay, double freshHardwood, double freshSoftwood, double frozenDrying, int pilePieces) {}

    public record Fish(double cookedDays, double cookedWaterLPerKg, double fatGPerKg, double frozenC, double frozenDays, double kcalPerKg, double proteinGPerKg, double rawDays,
            double rawWaterLPerKg, List<Double> runMonths, List<Double> spawnedOutMonths, double vitaminCMgPerKg) {}

    public record FishWheel(int most, double othersPerDay, double runPerDay) {}

    public record Forage(int needlesALeaf, double rootsChance, double sapChance, double sapKcalPerL, double sapLPerStep, List<Double> sapMonths, double syrupL, int syrupSeconds, double teaVitaminCMgPerL) {}

    public record Graphics(List<Integer> farViewChunks, List<String> farViewDetail, List<String> farViewThreads, List<String> heldLight, List<String> profiles,
            List<Integer> renderChunks, List<Integer> simulationChunks) {}

    public record Headlamp(double chargeHours, double crankHoursPerSecond, int darkBelow, double dimFromC, double dimmestC, double dimmestShare, int light) {}

    public record Healing(double above, double asleepFactor, double hoursPerHeart, double scurvyFactor) {}

    public record Hunting(double bleedHours, double bloodChance, double bloodDays, double carryKg, double dressClicksPerKg, double fatalHours, double heavyChestHit, double heavyPower, double organKg, double quillDamage, double scavengedDays, double strongBmi, double strongEnergy, int wolfKeepOffBlocks, double woundShare) {}

    public record Ice(List<Double> breakupMonths, double crackChance, double freezeBelowC, double overflowChance, double overflowWetness, double rotChance,
            double thawAboveC, double thickSledKg, double thickenDays, double thinLoadKg) {}

    public record Kit(double bowSawSpeed, int broadheads, int ferroRodStrikes, int hooks, int monoLines, int pemmican, int picks, int pilotBread, int rope, double sharpenShare,
            int sharpeningStoneUses, double sleepingBagClo, double sleepingPadClo, int snareWire, int tarps) {}

    public record Lights(double lanternHours, double torchHours) {}

    public record Locator(double elevationBlocks, double faintBlocks, double faintShare, double fullBlocks, double nameDegrees, int sendTicks, double spanDegrees) {}

    public record Load(double blockKg, int canvasPackRows, double capacityKg, double clothingKg, double firewoodKg, double foodKgPerKcal, int hidePackRows, double itemKg, double logKg, double stoneKg, double toolKg,
            int warnSeconds) {}

    public record Mice(double chance, double kcal) {}

    public record Migration(int caribouAutumnMonth, int caribouSpringMonth, int fromDayOfMonth, int geeseAutumnMonth, double geeseShare, int geeseSpringMonth, double herdShare) {}

    public record Mining(double rawIronChance, double speed) {}

    public record MooseCharges(double bluffShare, double bluffStopBlocks, List<Double> calfMonths, double chargeBlocks, double chargeChance, double cowShare,
            double treeBlocks) {}

    public record Mosquitoes(double calmKmh, double energyPerHour, int firstHour, int lastHour, List<Double> months, double sleepShare, int waterBlocks) {}

    public record PlantFibre(double chance) {}

    public record Raft(double calmKmh, double fallChance) {}

    public record Raids(double bearKcal, List<String> fouling, double raidBlocks, double raidHours, List<String> raiders, double scareBlocks, int tallPoles,
            double wolverineKcal) {}

    public record Rations(int dawnHour, int duskHour, double feastKcal, double leanKcal, double normalKcal, int reachBlocks) {}

    public record Rooms(int acrossBlocks, double bodyGainC, double bodyVolume, double cellarShare, int chimneyBlocks, double fireGain, double fireVolume, double groundC,
            int highBlocks, Map<String, Double> insulation, double radiantShare, int recheckTicks, int remembered, double smokySleep, double snowShare,
            double warmHours) {}

    public record Seasons(List<Double> feedingMonths, List<Double> rutMonths) {}

    public record Skills(double levelShare, List<Double> usesToLevel) {}

    public record Sky(int cloudHeight, double moonFull, double moonNew, double moonPartial) {}

    public record Sled(double groundDrag, double groundFriction, double selfKg, double snowDrag, double snowFriction) {}

    public record Smoker(double batchHours, double burnRate, double risingHours) {}

    public record Snow(int deepLayers, double frostbiteBlocks, double slowShare) {}

    public record Spirits(double arrivalShare, List<Double> auroraMonths, double auroraPerHour, double bigCatch, double bigCatchKg, double bottomShare, double coldPerHour,
            double comfortCeiling, double companyBlocks, double companyPerHour, double darkLight, double darkPerHour, double firePerHour, double foodMemoryDays, double highHealing, double highShare,
            double highWork, double hotMeal, double hungerPerHour, double illPerHour, double lowShare, double lowSleep, double lowWorkSpeed, double newFood, double roomPerHour, double sameFood,
            int sameFoodTimes, double settlePerDay, double smokyPerHour, double sunPerHour, double taskDone, double warmRoomC, double wetPerHour) {}

    public record Spoilage(double doublingC, double frozenBelowC, double frozenShare, double referenceC, double smokedLife, double stackDays) {}

    public record Survey(double baseBlocks, int blocksAPixel, double blocksPerHeight, double minutes, double mostBlocks) {}

    public record Survivor(int quietSeconds) {}

    public record Tasks(int cacheFromCampBlocks, double dryFirewood, double foodDays) {}

    public record Tracking(List<Double> ageHours, double backTrailBlocks, double backTrailHours, double calmKmh, double calmScentBlocks, double droppingsBlocks, double fleeBlocks,
            double printBlocks, double rainFade, double readHours, double scentBlocks, double scentDegrees, double signDays, double stalkBlocks) {}

    public record Trail(int cairnHeight, double pitchDays) {}

    public record Traps(double baitShare, double fishPerDay, double fishShare, int miceBlocks, int runBlocks, double runShare, double share) {}

    public record Warmth(double baseClo, double coldShare, double comfortC, double comfortSpanC, double coolingShare, double coreC, double coreCPerHour, double dryPerHour,
            double fireDryPerHour, int heatReachBlocks, double rainWetPerHour, int roofBlocks, double shelterC, double shelterDryPerHour, double shiverCoreC,
            double shiverKcalPerHourC, double shiverMostBmr, double sweatCoreC, double sweatLPerHour, double sweatWetPerHour, double veryColdShare, double warmCPerClo,
            double wetInsulationLoss) {}

    public record Walking(double climbMS, double feetShare, double iceEta, double loadWPerKg, double motionWPerKg, double runMS, double sneakMS, double snowDeepestCm,
            double snowEta, double snowEtaPerCm, double snowshoeSink, double sprintBlocksPerTick, double standingWPerKg, double steepestGrade, double swimMS, double walkMS,
            double windKmhDoubled) {}

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
        if (type == List.class && generic instanceof ParameterizedType list) {
            Type item = list.getActualTypeArguments()[0];
            if (item == String.class) {
                return ((List<?>) raw).stream().map(String.class::cast).toList();
            }
            if (item == Integer.class) {
                return ((List<?>) raw).stream().map(number -> ((Number) number).intValue()).toList();
            }
            return ((List<?>) raw).stream().map(number -> ((Number) number).doubleValue()).toList();
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
