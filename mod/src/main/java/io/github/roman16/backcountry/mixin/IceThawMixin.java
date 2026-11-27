package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Hunting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
abstract class IceThawMixin {
    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void backcountry$thawsInWarmAir(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo info) {
        if (!state.is(Blocks.ICE)) {
            return;
        }
        Balance.Ice ice = Balance.get().ice();
        double air = Climate.airC(level, pos, Calendar.day(level));
        if (air > ice.thawAboveC()) {
            melt(state, level, pos);
            info.cancel();
            return;
        }
        if (Hunting.months(Seasons.month(level), ice.breakupMonths()) && random.nextDouble() < ice.rotChance()) {
            level.setBlockAndUpdate(pos, Things.THIN_ICE.get().defaultBlockState());
            info.cancel();
            return;
        }
        if (air <= ice.freezeBelowC() && level.getBiome(pos).is(BiomeTags.IS_RIVER) && level.getBlockState(pos.above()).isAir() && random.nextDouble() < ice.overflowChance()) {
            level.setBlockAndUpdate(pos.above(), Things.OVERFLOW.get().defaultBlockState());
        }
    }
}
