package io.github.roman16.backcountry.body;

import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.weather.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class Warmth {
    private static final int VERTICAL_REACH = 2;

    private Warmth() {}

    public static void sense(ServerPlayer player, Body body, int day) {
        Balance.Warmth warmth = Balance.get().warmth();
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        body.sheltered = roofed(level, feet, warmth.roofBlocks());
        double heat = heat(level, feet, warmth.heatReachBlocks());
        body.fire = heat > 0;
        double air = Climate.airC(level, feet, day);
        double wind = Weather.day(level, day).windKmh();
        body.windKmh = wind;
        body.windy = !body.sheltered && wind > Climate.CALM_KMH;
        body.effectiveC = (body.sheltered ? air + warmth.shelterC() : Climate.feelsC(air, wind)) + heat;
        double clothing = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            clothing += Balance.get().insulation().getOrDefault(BuiltInRegistries.ITEM.getKey(player.getItemBySlot(slot).getItem()).toString(), 0.0);
        }
        body.clo = (warmth.baseClo() + clothing) * (1 - body.wetness * warmth.wetInsulationLoss());
    }

    public static boolean bareHead(ServerPlayer player) {
        return Balance.get().insulation().getOrDefault(BuiltInRegistries.ITEM.getKey(player.getItemBySlot(EquipmentSlot.HEAD).getItem()).toString(), 0.0) <= 0;
    }

    public static double comfortLowC(Body body) {
        Balance.Warmth warmth = Balance.get().warmth();
        return warmth.comfortC() - body.clo * warmth.warmCPerClo();
    }

    public static int comfort(Body body) {
        Balance.Warmth warmth = Balance.get().warmth();
        double low = comfortLowC(body);
        if (body.effectiveC < low - warmth.comfortSpanC()) {
            return -2;
        }
        if (body.effectiveC < low) {
            return -1;
        }
        if (body.effectiveC > low + 2 * warmth.comfortSpanC()) {
            return 2;
        }
        return body.effectiveC > low + warmth.comfortSpanC() ? 1 : 0;
    }

    public static void core(Body body, double hours) {
        Balance.Warmth warmth = Balance.get().warmth();
        double low = comfortLowC(body);
        double high = low + warmth.comfortSpanC();
        double target = warmth.coreC();
        if (body.effectiveC < low) {
            target -= (low - body.effectiveC) * warmth.coolingShare();
        } else if (body.effectiveC > high) {
            target += (body.effectiveC - high) * warmth.coolingShare() / 4;
        }
        double step = warmth.coreCPerHour() * hours;
        body.lastCoreC = body.coreC;
        body.coreC += Math.max(-step, Math.min(step, target - body.coreC));
    }

    public static void wet(ServerPlayer player, Body body, double hours) {
        Balance.Warmth warmth = Balance.get().warmth();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (player.isInWater()) {
            body.wetness = 1;
            return;
        }
        double change = -warmth.dryPerHour();
        if (EclipticSeasonsApi.getInstance().isRainOrSnowAt(player.level(), head) && player.level().canSeeSky(head)) {
            change = warmth.rainWetPerHour() * Weather.wetShare(Weather.day(player.level(), Calendar.day(player.level())));
        } else {
            if (body.fire) {
                change -= warmth.fireDryPerHour();
            }
            if (body.sheltered) {
                change -= warmth.shelterDryPerHour();
            }
        }
        if (body.coreC > warmth.sweatCoreC()) {
            change += warmth.sweatWetPerHour();
        }
        body.wetness = Math.max(0, Math.min(1, body.wetness + change * hours));
    }

    private static boolean roofed(Level level, BlockPos feet, int reach) {
        for (int up = 2; up <= reach + 1; up++) {
            BlockPos above = feet.above(up);
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static double heat(Level level, BlockPos feet, int reach) {
        double best = 0;
        for (BlockPos at : BlockPos.betweenClosed(feet.offset(-reach, -VERTICAL_REACH, -reach), feet.offset(reach, VERTICAL_REACH, reach))) {
            BlockState state = level.getBlockState(at);
            Double heat = state.getBlock() instanceof FireBlock && level.getBlockEntity(at) instanceof CampfireBlockEntity campfire ? Fire.of(campfire).heatC()
                    : Balance.get().heat().get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            if (heat == null || heat <= 0 || !(state.getBlock() instanceof FireBlock) && state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                continue;
            }
            double distance = Math.sqrt(at.distSqr(feet));
            best = Math.max(best, heat * Math.max(0, 1 - distance / (reach + 1)));
        }
        return best;
    }
}
