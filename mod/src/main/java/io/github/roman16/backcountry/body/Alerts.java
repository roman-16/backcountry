package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.gear.SupplyCrateBlock;
import io.github.roman16.backcountry.weather.Forecast;
import io.github.roman16.backcountry.weather.Outlook;
import io.github.roman16.backcountry.weather.Weather;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;

public final class Alerts {
    private static final double FREEZING_C = 0;
    private static final long TICKS_A_DAY = 24000;
    private static final float BEEP_VOLUME = 1;
    private static final float BEEP_PITCH = 2;

    private Alerts() {}

    public static void check(ServerPlayer player, Body body, int day) {
        Balance.Alerts rules = Balance.get().alerts();
        long now = player.level().getGameTime();
        if (body.alertsDay != day) {
            body.alerts.keySet().removeIf(Alert::daily);
            if (body.bmi() >= rules.wastingBmi()) {
                body.alerts.remove(Alert.BODY_WASTING);
            }
            body.alertsDay = day;
        }
        Long dropped = body.alerts.get(Alert.WEIGHT_DROPPED);
        if (dropped != null && now - dropped >= TICKS_A_DAY) {
            body.alerts.remove(Alert.WEIGHT_DROPPED);
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        boolean raised = false;
        Outlook tomorrow = Forecast.outlook(level, day, 1, pos);
        if (tomorrow.likely(rules.chance())) {
            raised |= switch (tomorrow.falling()) {
                case SNOW -> raise(body, Alert.SNOW_TOMORROW, now);
                case BLIZZARD -> raise(body, Alert.BLIZZARD_TOMORROW, now);
                case THUNDERSTORM -> raise(body, Alert.THUNDERSTORM_TOMORROW, now);
                default -> false;
            };
        }
        if (frost(nightLowC(level, pos, day), nightLowC(level, pos, day - 1))) {
            raised |= raise(body, Alert.FROST_TONIGHT, now);
        }
        if (weighed(body)) {
            body.alerts.put(Alert.WEIGHT_DROPPED, now);
            raised = true;
        }
        if (body.bmi() < rules.wastingBmi()) {
            raised |= raise(body, Alert.BODY_WASTING, now);
        }
        if (raised) {
            body.unread = true;
            if (SupplyCrateBlock.carriesMessenger(player)) {
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, BEEP_VOLUME, BEEP_PITCH);
            }
        }
    }

    public static boolean frost(double tonightLowC, double lastNightLowC) {
        return tonightLowC <= FREEZING_C && lastNightLowC > FREEZING_C;
    }

    public static boolean weighed(Body body) {
        double step = Balance.get().alerts().weightStepKg();
        if (body.weightKg > body.weightMarkKg) {
            body.weightMarkKg = body.weightKg;
        }
        if (body.weightKg > body.weightMarkKg - step) {
            return false;
        }
        while (body.weightKg <= body.weightMarkKg - step) {
            body.weightMarkKg -= step;
        }
        return true;
    }

    public static List<Alert> shown(Body body) {
        return body.alerts.keySet().stream().sorted().toList();
    }

    private static boolean raise(Body body, Alert alert, long now) {
        return body.alerts.putIfAbsent(alert, now) == null;
    }

    private static double nightLowC(ServerLevel level, BlockPos pos, int day) {
        Biome biome = level.getBiome(pos).value();
        return Forecast.lowest(hour -> Climate.airC(level, biome, pos, day, hour), Weather.EVENING, Weather.HOURS);
    }
}
