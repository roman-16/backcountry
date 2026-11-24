package io.github.roman16.backcountry;

import com.mojang.authlib.GameProfile;
import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import io.github.roman16.backcountry.body.Alerts;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.body.Warmth;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.LeftPackEntity;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import io.github.roman16.backcountry.survivor.Arrival;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.survivor.Team;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Kind;
import io.github.roman16.backcountry.weather.Sky;
import io.github.roman16.backcountry.weather.Weather;
import com.teamtea.eclipticseasons.common.core.biome.WeatherManager;
import java.time.LocalDate;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec2;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Backcountry.MODID)
@PrefixGameTestTemplate(false)
public final class BackcountryTests {
    private static final long DAY = 24000;
    private static final long HOUR = 1000;
    private static final double KG = 0.01;

    private BackcountryTests() {}

    static ServerPlayer survivor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "survivor"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        int day = Calendar.day(level);
        Metabolism.body(player).reset(day, Calendar.year(day));
        return player;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static void warm(GameTestHelper helper, ServerPlayer player) {
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Things.HIDE_HAT.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Things.HIDE_COAT.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Things.HIDE_PANTS.get()));
    }

    @GameTest(template = "empty")
    public static void everyFoodHasItsData(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.components().get(DataComponents.FOOD) != null && Nutrition.of(new ItemStack(item)).isEmpty()) {
                missing.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
        }
        check(helper, missing.isEmpty(), "foods without an entry in data/backcountry/data_maps/item/food.json: " + String.join(", ", missing.stream().sorted().toList()));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aRestingDayBurnsByWeight(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        warm(helper, player);
        Body body = Metabolism.body(player);
        Metabolism.step(player, body, DAY, false);
        double resting = Balance.get().body().restingKcalPerKgDay() * Balance.get().body().arrivalKg();
        check(helper, Math.abs(body.kcalBurnedToday - resting) < 1, "a warm resting day burned " + body.kcalBurnedToday + " kcal, not " + resting);
        double expected = Balance.get().body().arrivalKg() - resting / Balance.get().body().kcalPerKg();
        check(helper, Math.abs(body.weightKg - expected) < KG, "a day without food left " + body.weightKg + " kg, not " + expected);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pilotBreadFillsTheStomach(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        Nutrition bread = Nutrition.of(new ItemStack(Things.PILOT_BREAD.get())).orElseThrow();
        check(helper, bread.kcal() == 100 && bread.fat() == 2.5, "Pilot Bread is " + bread);
        int eaten = 0;
        while (Metabolism.eat(player, bread)) {
            eaten++;
        }
        check(helper, eaten == (int) (Balance.get().body().stomachKcal() / bread.kcal()), "a stomach took " + eaten + " Pilot Bread");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepRestoresEnergy(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        warm(helper, player);
        Body body = Metabolism.body(player);
        body.energy = 0.25;
        Metabolism.step(player, body, (long) (Balance.get().energy().sleepHoursToFull() * HOUR), true);
        check(helper, body.energy > 0.95, "a night's sleep left energy at " + body.energy);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noEnergyBlacksOut(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        body.energy = 0.0001;
        body.workKcal = Balance.get().energy().workKcalToEmpty();
        Metabolism.step(player, body, Metabolism.STEP_TICKS, false);
        check(helper, body.blackoutTicks > 0, "energy at " + body.energy + " did not black out");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyUnboiledWaterInfects(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        for (int i = 0; i < 64; i++) {
            Metabolism.drink(player, 0, true);
        }
        check(helper, !body.has(Condition.GUT_INFECTION), "boiled water gave a gut infection");
        for (int i = 0; i < 64 && !body.has(Condition.GUT_INFECTION); i++) {
            Metabolism.drink(player, 0, false);
        }
        check(helper, body.has(Condition.GUT_INFECTION), "64 drinks of lake water never infected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCampPotBoilsOnALitFire(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 2);
        helper.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(fire.above(), Things.CAMP_POT_BLOCK.get().defaultBlockState());
        BlockPos at = helper.absolutePos(fire.above());
        if (!(helper.getLevel().getBlockEntity(at) instanceof CampPotEntity pot)) {
            throw new IllegalStateException("the camp pot has no block entity");
        }
        pot.hold(new Water(false, 2));
        for (int tick = 0; tick < Balance.get().water().boilSeconds() * 20; tick++) {
            CampPotEntity.tick(helper.getLevel(), at, helper.getLevel().getBlockState(at), pot);
        }
        check(helper, pot.water().boiled(), "the pot's water did not boil");
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        ServerPlayer player = survivor(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        helper.getLevel().getBlockState(at).useItemOn(bottle, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        Water poured = WaterVesselItem.water(player.getMainHandItem());
        check(helper, poured.boiled() && Math.abs(poured.litres() - Balance.get().water().bottleL()) < KG, "the bottle took " + poured);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coldCoolsTheCore(GameTestHelper helper) {
        Body body = new Body();
        body.reset(0, 1);
        body.clo = Balance.get().warmth().baseClo();
        body.effectiveC = -20;
        for (int hour = 0; hour < 4; hour++) {
            Warmth.core(body, 1);
        }
        check(helper, body.coreC < Balance.get().conditions().frostCoreC(), "four hours at -20 °C left the core at " + body.coreC);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aRoofIsShelter(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        Warmth.sense(player, body, 0);
        check(helper, !body.sheltered, "open sky counted as shelter");
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.SPRUCE_PLANKS);
        Warmth.sense(player, body, 0);
        check(helper, body.sheltered, "a roof 3 blocks up was no shelter");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTheHotbarAndThePackHold(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.getInventory().clearContent();
        for (int i = 0; i < 12; i++) {
            player.getInventory().add(new ItemStack(Items.STICK, 64));
        }
        int used = 0;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            used += player.getInventory().items.get(slot).isEmpty() ? 0 : 1;
        }
        check(helper, used == Hotbar.ROW, "12 stacks took " + used + " slots");
        Slot row = player.inventoryMenu.getSlot(Hotbar.ROW);
        check(helper, !row.mayPlace(new ItemStack(Items.STICK)), "a locked row took an item");
        Metabolism.body(player).pack = new ItemStack(Things.HIDE_PACK.get());
        check(helper, row.mayPlace(new ItemStack(Items.STICK)), "the worn pack's row refused an item");
        row.set(new ItemStack(Items.FLINT, 3));
        ItemStack pack = Metabolism.body(player).pack.copy();
        check(helper, Hotbar.contents(pack).get(0).is(Items.FLINT), "the pack's row did not keep its item in the pack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bmi17Extracts(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.getInventory().setItem(0, new ItemStack(Items.FLINT, 5));
        Body body = Metabolism.body(player);
        body.weightKg = 55;
        BlockPos stood = player.blockPosition();
        Metabolism.step(player, body, Metabolism.STEP_TICKS, false);
        check(helper, body.ended != null && body.ended.kind() == Summary.Kind.EXTRACTED, "a survivor at 55 kg was not extracted");
        check(helper, player.isSpectator(), "the extracted survivor stayed in the world");
        boolean left = false;
        for (BlockPos at : BlockPos.betweenClosed(stood.offset(-8, -8, -8), stood.offset(8, 8, 8))) {
            if (helper.getLevel().getBlockEntity(at) instanceof LeftPackEntity pack && pack.getItem(0).is(Items.FLINT)) {
                left = true;
            }
        }
        check(helper, left, "no left pack holds what the survivor carried");
        Arrival.arrive(player);
        check(helper, !player.isSpectator() && body.ended == null && body.weightKg == Balance.get().body().arrivalKg(), "the new survivor did not arrive fresh");
        check(helper, player.getInventory().getItem(4).is(Things.PILOT_BREAD.get()) && player.getInventory().getItem(4).getCount() == Balance.get().kit().pilotBread(),
                "the kit has no " + Balance.get().kit().pilotBread() + " Pilot Bread");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCalendarStartsOn3May(GameTestHelper helper) {
        check(helper, Calendar.dateText(0).equals("3 May"), "day 1 is " + Calendar.dateText(0));
        check(helper, Calendar.dateText(8).equals("2 Jul"), "day 9 is " + Calendar.dateText(8));
        check(helper, Calendar.dateText(30).equals("17 Dec"), "day 31 is " + Calendar.dateText(30));
        check(helper, Calendar.dayOfYear(48) == 1 && Calendar.year(48) == 2, "day 49 is not day 1 of year 2");
        ServerLevel level = helper.getLevel();
        int day = Calendar.day(level);
        SolarTerm term = EclipticSeasonsApi.getInstance().getSolarTerm(level);
        int expected = (day + Calendar.firstSolarDay()) / Balance.get().year().termDays();
        check(helper, term.ordinal() == expected % 24, "solar day " + (day + Calendar.firstSolarDay()) + " is " + term);
        check(helper, EclipticSeasonsApi.getInstance().getLastingDaysOfEachTerm(level) == Balance.get().year().termDays(),
                "Ecliptic's terms last " + EclipticSeasonsApi.getInstance().getLastingDaysOfEachTerm(level) + " days");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theYearWaitsForAQuietMoment(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        body.lastHurt = helper.getLevel().getGameTime();
        check(helper, !Team.quiet(player, body), "a survivor hurt this moment counted as quiet");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCrateHandsOutOneMessenger(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.getInventory().clearContent();
        BlockPos crate = new BlockPos(3, 1, 3);
        helper.setBlock(crate, Things.SUPPLY_CRATE.get());
        BlockPos at = helper.absolutePos(crate);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, hit);
        int messengers = player.getInventory().countItem(Things.SATELLITE_MESSENGER.get());
        check(helper, messengers == 1, "the crate handed out " + messengers + " messengers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBedDoesNotMoveTheArrival(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.setRespawnPosition(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 1)), 0, false, true);
        check(helper, player.getRespawnPosition() == null, "a bed set where the survivor arrives");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theWeatherIsTheSameForTheSameDay(GameTestHelper helper) {
        check(helper, Weather.drawn(1, 100).equals(Weather.drawn(1, 100)), "seed 1 drew day 100 twice differently");
        check(helper, !Weather.drawn(1, 100).equals(Weather.drawn(2, 100)), "seeds 1 and 2 drew the same day 100");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aYearOfWeatherIsAnchorages(GameTestHelper helper) {
        Balance.Weather rules = Balance.get().weather();
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Biome taiga = biomes.getOrThrow(Biomes.TAIGA);
        BlockPos sea = new BlockPos(0, Balance.get().climate().seaLevel(), 0);
        int years = 64;
        int[] days = new int[12];
        int[] wet = new int[12];
        int blizzards = 0;
        int thunderstorms = 0;
        double coldest = Double.MAX_VALUE;
        for (int day = 0; day < years * Balance.get().year().days(); day++) {
            Day weather = Weather.drawn(1, day);
            LocalDate date = Calendar.date(day);
            int month = date.getMonthValue() - 1;
            days[month]++;
            wet[month] += weather.wet() ? 1 : 0;
            for (int hour = 0; hour <= Weather.HOURS; hour++) {
                coldest = Math.min(coldest, Climate.airC(taiga, sea, day, hour, weather.driftC(), weather.cloud()));
            }
            if (weather.thunder()) {
                thunderstorms++;
                check(helper, date.getMonthValue() >= rules.stormFirstMonth() && date.getMonthValue() <= rules.stormLastMonth(), "a thunderstorm on " + date);
            }
            double middle = (weather.fromHour() + weather.toHour()) / 2.0;
            if (weather.wet() && weather.windKmh() >= rules.blizzardWindKmh()
                    && Climate.airC(taiga, sea, day, middle, weather.driftC(), weather.cloud()) <= rules.snowBelowC()) {
                blizzards++;
            }
        }
        for (int month = 0; month < 12; month++) {
            double share = wet[month] / (double) days[month];
            double anchorage = rules.precipitationDays().get(month) / LocalDate.of(2001, month + 1, 1).lengthOfMonth();
            check(helper, Math.abs(share - anchorage) <= anchorage / 4, "month " + (month + 1) + " was wet on " + share + " of its days, Anchorage on " + anchorage);
        }
        check(helper, coldest <= -28 && coldest >= -40, "the coldest sea-level taiga air in " + years + " years was " + coldest + " °C");
        double blizzardsAYear = blizzards / (double) years;
        double thunderstormsAYear = thunderstorms / (double) years;
        check(helper, blizzardsAYear >= 0.5 && blizzardsAYear <= 4, blizzardsAYear + " blizzards a year");
        check(helper, thunderstormsAYear >= 0.5 && thunderstormsAYear <= 4, thunderstormsAYear + " thunderstorms a year");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theSkyFollowsTheForecast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int day = Calendar.day(level);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        Biome biome = level.getBiome(at).value();
        double air = Climate.airC(level, at, day);
        double snow = Balance.get().weather().snowBelowC();
        Day cold = new Day(day, (float) (Weather.day(level, day).driftC() + 2 * (snow - 16 - air)), 1, 0, 0, Day.MODERATE, 0, Weather.HOURS, false);
        Day warm = new Day(day, (float) (Weather.day(level, day).driftC() + 2 * (snow + 16 - air)), 1, 0, 0, Day.MODERATE, 0, Weather.HOURS, false);
        Weather.set(cold);
        helper.startSequence().thenExecuteAfter(2, () -> {
            check(helper, level.getLevelData().isRaining(), "a day of snow left the sky dry");
            check(helper, WeatherManager.getPrecipitationAt(level, biome, at) == Biome.Precipitation.SNOW, "air well below freezing did not snow");
            Weather.set(warm);
        }).thenExecuteAfter(2, () -> {
            check(helper, WeatherManager.getPrecipitationAt(level, biome, at) == Biome.Precipitation.RAIN, "air well above freezing did not rain");
            Weather.set(Day.calm(day));
        }).thenExecuteAfter(2, () -> {
            Weather.unset(day);
            check(helper, !level.getLevelData().isRaining(), "a dry day left the sky raining");
            Sky.drive(level, day);
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void windChillsOnlyTheOpen(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        Body body = Metabolism.body(player);
        int winter = 4096 * Balance.get().year().days() + 30;
        Weather.set(new Day(winter, -8, 0, 32, 0, Day.DRY, 0, 0, false));
        Warmth.sense(player, body, winter);
        double air = Climate.airC(helper.getLevel(), player.blockPosition(), winter);
        check(helper, body.windy && body.effectiveC < air - 4, "32 km/h of wind at " + air + " °C felt like " + body.effectiveC + " °C in the open");
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.SPRUCE_PLANKS);
        Warmth.sense(player, body, winter);
        Weather.unset(winter);
        check(helper, !body.windy && Math.abs(body.effectiveC - air - Balance.get().warmth().shelterC()) < 0.01, "a roof did not take the wind off");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theWeatherCommandIsRefused(GameTestHelper helper) {
        List<Component> heard = new ArrayList<>();
        CommandSource listener = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                heard.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        ServerLevel level = helper.getLevel();
        CommandSourceStack source = new CommandSourceStack(listener, Vec3.ZERO, Vec2.ZERO, level, 4, "test", Component.literal("test"), level.getServer(), null);
        level.getServer().getCommands().performPrefixedCommand(source, "weather rain");
        String refusal = Component.translatable("message.backcountry.weather_follows_forecast").getString();
        check(helper, heard.stream().anyMatch(message -> message.getString().equals(refusal)), "/weather answered " + heard.stream().map(Component::getString).toList());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frostAlertsOnlyAfterAThaw(GameTestHelper helper) {
        check(helper, Alerts.frost(-1, 2), "a frost after a thaw did not alert");
        check(helper, !Alerts.frost(-1, -3), "a frost after a frost alerted");
        check(helper, !Alerts.frost(1, 3), "a night above freezing alerted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightAlertsEveryTwoKilograms(GameTestHelper helper) {
        Body body = new Body();
        body.reset(0, 1);
        double arrived = body.weightKg;
        double step = Balance.get().alerts().weightStepKg();
        body.weightKg = arrived - step / 2;
        check(helper, !Alerts.weighed(body), "half a step down alerted");
        body.weightKg = arrived - step;
        check(helper, Alerts.weighed(body), "a step down did not alert");
        body.weightKg = arrived - step * 1.5;
        check(helper, !Alerts.weighed(body), "half a step further alerted");
        body.weightKg = arrived + 1;
        check(helper, !Alerts.weighed(body), "gaining weight alerted");
        body.weightKg = arrived + 1 - step;
        check(helper, Alerts.weighed(body), "a step below the regained weight did not alert");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lightningSetsNoFire(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.GRASS_BLOCK);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        bolt.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(bolt);
        helper.runAfterDelay(4, () -> {
            for (BlockPos near : BlockPos.betweenClosed(new BlockPos(2, 1, 2), new BlockPos(4, 3, 4))) {
                check(helper, !helper.getBlockState(near).is(Blocks.FIRE), "lightning set fire at " + near);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void lightningBringsNoRider(GameTestHelper helper) {
        SkeletonHorse horse = EntityType.SKELETON_HORSE.create(helper.getLevel());
        horse.setTrap(true);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        horse.moveTo(at.x, at.y, at.z);
        check(helper, !helper.getLevel().addFreshEntity(horse), "a lightning trap's skeleton horse joined the world");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theForecastKnowsSnowFromRain(GameTestHelper helper) {
        check(helper, Kind.falling(Balance.get().weather().snowBelowC(), 0, false) == Kind.SNOW, "air at the snow line did not snow");
        check(helper, Kind.falling(Balance.get().weather().snowBelowC() - 4, Balance.get().weather().blizzardWindKmh(), false) == Kind.BLIZZARD,
                "snow in a gale was no blizzard");
        check(helper, Kind.falling(Balance.get().weather().snowBelowC() + 4, 0, true) == Kind.THUNDERSTORM, "warm thunder was no thunderstorm");
        helper.succeed();
    }
}
