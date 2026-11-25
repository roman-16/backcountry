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

    private Exertion() {}

    public static final class Strides {
        public Strides() {}

        double walk;
        double walkEta;
        double sneak;
        double sneakEta;
        double run;
        double runEta;
        double swim;
        double climb;
        double distance;
        double rise;

        void clear() {
            walk = 0;
            walkEta = 0;
            sneak = 0;
            sneakEta = 0;
            run = 0;
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
        double blocks = Math.sqrt(Math.pow(to.x - from.x, 2) + Math.pow(to.z - from.z, 2));
        double rise = to.y - from.y;
        if (player.isInWater() || player.isSwimming()) {
            strides.swim += Math.min(1, blocks / walking.swimBlocksPerTick());
            return;
        }
        if (player.onClimbable() && rise > 0) {
            strides.climb += 1;
            return;
        }
        if (blocks <= 0) {
            return;
        }
        double eta = terrain(player);
        if (player.isSprinting()) {
            double share = Math.min(1, blocks / walking.sprintBlocksPerTick());
            strides.run += share;
            strides.runEta += share * eta;
        } else if (player.isCrouching()) {
            double share = Math.min(1, blocks / walking.sneakBlocksPerTick());
            strides.sneak += share;
            strides.sneakEta += share * eta;
        } else {
            double share = Math.min(1, blocks / walking.walkBlocksPerTick());
            strides.walk += share;
            strides.walkEta += share * eta;
        }
        strides.distance += blocks;
        strides.rise += rise;
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
        double gait = KCAL_PER_WATT_HOUR * (strides.walkEta * motion(walking.walkMS()) + strides.sneakEta * motion(walking.sneakMS())) / TICKS_AN_HOUR
                + strides.runEta / TICKS_AN_HOUR * Math.max(0, Activity.RUN.met() - standingKcalPerKgHour());
        double carried = (gait * hill * wind + strides.climb / TICKS_AN_HOUR * Math.max(0, Activity.CLIMB.met() - standingKcalPerKgHour()));
        double moving = carried * kg + strides.swim / TICKS_AN_HOUR * Math.max(0, Activity.SWIM.met() - standingKcalPerKgHour()) * kg;
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

    private static double motion(double metresPerSecond) {
        return Balance.get().walking().motionWPerKg() * metresPerSecond * metresPerSecond;
    }
}
