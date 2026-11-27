package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.weather.Weather;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class Spirits {
    private static final long TICKS_A_DAY = 24000;
    private static final double EVENING_HOUR = 18;
    private static final double MORNING_HOUR = 6;
    private static final double CLEAR_CLOUD = 0.25;
    private static final double SUNNY_CLOUD = 0.5;
    private static final double WET = 0.5;
    private static final long MEAL_TICKS = 1000;

    private Spirits() {}

    public enum Cause {
        FIRE(true),
        ROOM(true),
        COMPANY(true),
        SUN(true),
        AURORA(true),
        VARIED_FOOD(true),
        COLD(false),
        WET(false),
        HUNGER(false),
        DARK(false),
        SMOKE(false),
        SAME_FOOD(false);

        public final boolean lifts;

        Cause(boolean lifts) {
            this.lifts = lifts;
        }

        public int bit() {
            return 1 << ordinal();
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static boolean high(double spirits) {
        return spirits >= Balance.get().spirits().highShare();
    }

    public static boolean low(double spirits) {
        return spirits <= Balance.get().spirits().lowShare();
    }

    public static boolean bottom(double spirits) {
        return spirits <= Balance.get().spirits().bottomShare();
    }

    public static void step(ServerPlayer player, Body body, double hours, boolean asleep) {
        Balance.Spirits rules = Balance.get().spirits();
        Level level = player.level();
        int day = Calendar.day(level);
        double hour = Climate.hour(level);
        BlockPos head = BlockPos.containing(player.getEyePosition());
        boolean night = hour >= EVENING_HOUR || hour < MORNING_HOUR;
        boolean sky = level.canSeeSky(head);
        double cloud = Weather.cloud(level, day, hour);
        int causes = body.spiritCauses & (Cause.SAME_FOOD.bit() | Cause.VARIED_FOOD.bit());
        double rate = 0;
        if (body.fire && !asleep) {
            rate += rules.firePerHour();
            causes |= Cause.FIRE.bit();
        }
        if (body.room != Rooms.Kind.OPEN && body.roomC >= rules.warmRoomC() && body.wetness < WET) {
            rate += rules.roomPerHour();
            causes |= Cause.ROOM.bit();
        }
        boolean company = level.players().stream().anyMatch(other -> other != player && other instanceof ServerPlayer survivor && Metabolism.lives(survivor)
                && other.distanceTo(player) <= rules.companyBlocks());
        if (company) {
            rate += rules.companyPerHour();
            causes |= Cause.COMPANY.bit();
        }
        if (!night && sky && !level.isRaining() && cloud < SUNNY_CLOUD && !asleep) {
            rate += rules.sunPerHour();
            causes |= Cause.SUN.bit();
        }
        if (night && sky && cloud < CLEAR_CLOUD && !asleep && Hunting.months(Seasons.month(level), rules.auroraMonths())) {
            rate += rules.auroraPerHour();
            causes |= Cause.AURORA.bit();
        }
        if (body.coreC < Balance.get().warmth().shiverCoreC() && !asleep) {
            rate -= rules.coldPerHour();
            causes |= Cause.COLD.bit();
        }
        if (body.wetness >= WET) {
            rate -= rules.wetPerHour();
            causes |= Cause.WET.bit();
        }
        if (body.stomachKcal <= 0) {
            rate -= rules.hungerPerHour();
            causes |= Cause.HUNGER.bit();
        }
        if (!asleep && level.getMaxLocalRawBrightness(head) <= rules.darkLight()) {
            rate -= rules.darkPerHour();
            causes |= Cause.DARK.bit();
        }
        if (body.smoky) {
            rate -= rules.smokyPerHour();
            causes |= Cause.SMOKE.bit();
        }
        body.spiritCauses = causes;
        add(body, rate * hours);
    }

    public static void ate(ServerPlayer player, Body body, ItemStack stack) {
        Balance.Spirits rules = Balance.get().spirits();
        long now = player.level().getGameTime();
        long memory = (long) (rules.foodMemoryDays() * TICKS_A_DAY);
        for (int i = body.mealTicks.size() - 1; i >= 0; i--) {
            if (now - body.mealTicks.get(i) > memory) {
                body.mealTicks.remove(i);
                body.mealItems.remove(i);
            }
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (int i = 0; i < body.mealItems.size(); i++) {
            if (body.mealItems.get(i).equals(id) && now - body.mealTicks.get(i) < MEAL_TICKS) {
                return;
            }
        }
        long times = body.mealItems.stream().filter(id::equals).count();
        int causes = body.spiritCauses & ~(Cause.SAME_FOOD.bit() | Cause.VARIED_FOOD.bit());
        if (times >= rules.sameFoodTimes()) {
            add(body, -rules.sameFood());
            causes |= Cause.SAME_FOOD.bit();
        } else if (times == 0) {
            add(body, rules.newFood());
            causes |= Cause.VARIED_FOOD.bit();
        }
        body.spiritCauses = causes;
        if (hot(stack)) {
            add(body, rules.hotMeal());
        }
        body.mealItems.add(id);
        body.mealTicks.add(now);
    }

    private static boolean hot(ItemStack stack) {
        boolean stew = stack.has(DataComponents.FOOD) && stack.get(DataComponents.FOOD).usingConvertsTo().filter(left -> left.is(Items.BOWL)).isPresent();
        return stew || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("cooked_");
    }

    public static void add(Body body, double amount) {
        body.spirits = Math.max(0, Math.min(1, body.spirits + amount));
    }
}
