package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.haul.Sled;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class Exertion {
    public static final double TICKS_AN_HOUR = 1000;
    private static final double KCAL_PER_WATT_HOUR = 0.8598;
    private static final double CM_A_LAYER = 12.5;
    private static final double SHORTEST_RUN_BLOCKS = 0.5;
    private static final double SECONDS_AN_HOUR = 3600;

    private Exertion() {}

    public static final class Strides {
        public Strides() {}

        double walkEta;
        double sneakEta;
        double runEta;
        double swim;
        double climb;
        double distance;
        double rise;

        void clear() {
            walkEta = 0;
            sneakEta = 0;
            runEta = 0;
            swim = 0;
            climb = 0;
            distance = 0;
            rise = 0;
        }
    }

    public static void stride(ServerPlayer player, Body body, Vec3 from, Vec3 to) {
        Balance.Walking walking = Balance.get().walking();
        Strides strides = body.strides;
        double farthest = walking.sprintBlocksPerTick();
        double blocks = Math.min(farthest, Math.sqrt(Math.pow(to.x - from.x, 2) + Math.pow(to.z - from.z, 2)));
        double rise = to.y - from.y;
        if (player.isPassenger()) {
            return;
        }
        if (player.isInWater() || player.isSwimming()) {
            double swum = Math.min(farthest, Math.sqrt(blocks * blocks + rise * rise));
            strides.swim += swum;
            body.metresToday += swum;
            return;
        }
        if (player.onClimbable() && rise > 0) {
            strides.climb += Math.min(farthest, rise);
            return;
        }
        if (blocks <= 0) {
            return;
        }
        double eta = terrain(player);
        if (player.isSprinting()) {
            strides.runEta += blocks * eta;
        } else if (player.isCrouching()) {
            strides.sneakEta += blocks * eta;
        } else {
            strides.walkEta += blocks * eta;
        }
        strides.distance += blocks;
        strides.rise += Math.max(-farthest, Math.min(farthest, rise));
        body.metresToday += blocks;
    }

    public static double terrain(ServerPlayer player) {
        Balance.Walking walking = Balance.get().walking();
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        BlockState at = level.getBlockState(feet);
        BlockState below = level.getBlockState(feet.below());
        double depthCm = 0;
        if (at.getBlock() instanceof SnowLayerBlock) {
            depthCm = at.getValue(SnowLayerBlock.LAYERS) * CM_A_LAYER;
        }
        if (below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW)) {
            depthCm = walking.snowDeepestCm();
        } else if (below.getBlock() instanceof SnowLayerBlock) {
            depthCm += below.getValue(SnowLayerBlock.LAYERS) * CM_A_LAYER;
        }
        if (depthCm > 0) {
            depthCm = Math.min(depthCm, walking.snowDeepestCm());
            if (player.getItemBySlot(EquipmentSlot.FEET).is(Things.SNOWSHOES.get())) {
                depthCm *= walking.snowshoeSink();
            }
            return walking.snowEta() + walking.snowEtaPerCm() * depthCm;
        }
        return below.is(BlockTags.ICE) ? walking.iceEta() : 1;
    }

    public static double slope(double grade) {
        double steepest = Balance.get().walking().steepestGrade();
        return minetti(Math.max(-steepest, Math.min(steepest, grade))) / minetti(0);
    }

    private static double minetti(double i) {
        return 280.5 * Math.pow(i, 5) - 58.7 * Math.pow(i, 4) - 76.8 * Math.pow(i, 3) + 51.9 * i * i + 19.6 * i + 2.5;
    }

    public static double standingKcalPerKgHour() {
        return Balance.get().walking().standingWPerKg() * KCAL_PER_WATT_HOUR;
    }

    public static double activityKcal(double met, double kg, double hours) {
        return Math.max(0, met - standingKcalPerKgHour()) * kg * hours;
    }

    public static double effectiveLoadKg(ServerPlayer player, Body body) {
        return body.loadKg + (Balance.get().walking().feetShare() - 1) * Load.kg(player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static double burn(ServerPlayer player, Body body, double hours, boolean asleep, double absorbed) {
        Balance balance = Balance.get();
        Balance.Walking walking = balance.walking();
        Balance.Warmth warmth = balance.warmth();
        double kg = body.weightKg;
        double load = effectiveLoadKg(player, body);
        double illness = (body.has(Condition.INFECTED_WOUND) ? balance.conditions().feverBurn() : 0) + (body.has(Condition.SCURVY) ? balance.conditions().scurvyBurn() : 0);
        double restingRate = (asleep ? balance.body().sleepKcalPerKgHour() : standingKcalPerKgHour()) * kg;
        double resting = restingRate * hours * (1 + illness);
        double coldShare = asleep ? 0 : switch (Warmth.comfort(body)) {
            case -2 -> warmth.veryColdShare();
            case -1 -> warmth.coldShare();
            default -> 0;
        };
        double shiverPerHour = body.coreC < warmth.shiverCoreC()
                ? Math.min(warmth.shiverKcalPerHourC() * (warmth.shiverCoreC() - body.coreC), warmth.shiverMostBmr() * balance.body().bmrKcalPerKgHour() * kg)
                : 0;
        double shiver = shiverPerHour * hours;
        double cold = restingRate * coldShare * hours + shiver;
        Strides strides = body.strides;
        double grade = strides.distance > SHORTEST_RUN_BLOCKS ? strides.rise / strides.distance : 0;
        double hill = slope(grade);
        double wind = body.windy ? 1 + body.windKmh / walking.windKmhDoubled() : 1;
        double walked = hoursOver(strides.walkEta, walking.walkMS());
        double sneaked = hoursOver(strides.sneakEta, walking.sneakMS());
        double gait = KCAL_PER_WATT_HOUR * (walked * motion(walking.walkMS()) + sneaked * motion(walking.sneakMS()))
                + activityKcal(Activity.RUN.met(), 1, hoursOver(strides.runEta, walking.runMS()));
        double carried = gait * hill * wind + activityKcal(Activity.CLIMB.met(), 1, hoursOver(strides.climb, walking.climbMS()));
        double moving = carried * kg + activityKcal(Activity.SWIM.met(), kg, hoursOver(strides.swim, walking.swimMS()));
        double loadStanding = asleep ? 0 : walking.loadWPerKg() * (kg + load) * Math.pow(load / kg, 2) * KCAL_PER_WATT_HOUR * hours;
        double pulled = Sled.pulledKg(player);
        double loaded = carried * (load + pulled) + loadStanding;
        double work = body.workKcal;
        double digestion = absorbed * balance.body().thermicShare();
        strides.clear();
        body.workKcal = 0;
        body.burned[Burn.RESTING.ordinal()] += resting;
        body.burned[Burn.MOVING.ordinal()] += moving;
        body.burned[Burn.LOAD.ordinal()] += loaded;
        body.burned[Burn.WORK.ordinal()] += work;
        body.burned[Burn.COLD.ordinal()] += cold;
        body.burned[Burn.DIGESTION.ordinal()] += digestion;
        body.exertedKcal = moving + loaded + work + shiver;
        return resting + moving + loaded + work + cold + digestion;
    }

    private static double hoursOver(double metres, double metresPerSecond) {
        return metres / metresPerSecond / SECONDS_AN_HOUR;
    }

    private static double motion(double metresPerSecond) {
        return Balance.get().walking().motionWPerKg() * metresPerSecond * metresPerSecond;
    }
}
