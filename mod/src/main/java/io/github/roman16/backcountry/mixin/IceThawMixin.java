package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
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
        if (Climate.airC(level, pos, Calendar.day(level)) > Balance.get().ice().thawAboveC()) {
            melt(state, level, pos);
            info.cancel();
        }
    }
}
