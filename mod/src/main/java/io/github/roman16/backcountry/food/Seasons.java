package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Seasons {
    private static final int MONTHS = 12;
    private static final int BARE = 1;
    private static final int KEPT = 2;
    private static final int RIPE = 3;

    private Seasons() {}

    public static int month(Level level) {
        return Calendar.date(Calendar.day(level)).getMonthValue();
    }

    public static boolean within(Level level, int first, int last) {
        return within(month(level), first, last);
    }

    public static boolean within(int month, int first, int last) {
        return first <= last ? month >= first && month <= last : month >= first || month <= last;
    }

    public static int ripest(Level level, Block bush) {
        return ripest(month(level), bush);
    }

    public static int ripest(int month, Block bush) {
        List<Double> months = berryMonths(bush);
        int last = months.get(1).intValue();
        if (within(month, months.get(0).intValue(), last)) {
            return RIPE;
        }
        return !(bush instanceof BerryBushBlock) && within(month, last % MONTHS + 1, Balance.get().berries().lingonberryKeptUntil()) ? KEPT : BARE;
    }

    public static List<Double> berryMonths(Block bush) {
        Balance.Berries berries = Balance.get().berries();
        return bush instanceof BerryBushBlock berry ? berry.berry().months() : berries.lingonberryMonths();
    }

    @SubscribeEvent
    static void grow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        double air = Spoilage.airC(level, pos);
        if (state.getBlock() instanceof SweetBerryBushBlock bush) {
            List<Double> months = berryMonths(bush);
            if (air < 0 || !within(level, months.get(0).intValue(), months.get(1).intValue())) {
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            }
            return;
        }
        if (!(state.getBlock() instanceof CropBlock crop)) {
            return;
        }
        Balance.Farming farming = Balance.get().farming();
        Farmland farmland = Farmland.get(level);
        if (farmland.dud(pos) || air < 0 && !crop.isMaxAge(state)) {
            farmland.forget(pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            return;
        }
        boolean season = within(level, farming.firstMonth(), farming.lastMonth()) && air >= farming.warmC();
        event.setResult(season && level.random.nextDouble() < farming.growthShare() ? CropGrowEvent.Pre.Result.DEFAULT : CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    @SubscribeEvent
    static void boneMeal(BonemealEvent event) {
        if (event.isValidBonemealTarget()) {
            event.setSuccessful(false);
        }
    }

    @SubscribeEvent
    static void sown(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlacedBlock().getBlock() instanceof CropBlock)) {
            return;
        }
        if (level.random.nextDouble() < Balance.get().farming().germination()) {
            Farmland.get(level).forget(event.getPos());
        } else {
            Farmland.get(level).sow(event.getPos());
        }
    }

    @SubscribeEvent
    static void brushed(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) {
            event.setCanceled(true);
        }
    }
}
