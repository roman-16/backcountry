package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.food.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweetBerryBushBlock.class)
abstract class SweetBerryBushMixin {
    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void backcountry$always(BlockState state, CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(true);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void backcountry$shedOutOfSeason(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo info) {
        int ripest = Seasons.ripest(level, state.getBlock());
        if (state.getValue(SweetBerryBushBlock.AGE) > ripest) {
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, ripest), Block.UPDATE_CLIENTS);
            info.cancel();
        }
    }
}
