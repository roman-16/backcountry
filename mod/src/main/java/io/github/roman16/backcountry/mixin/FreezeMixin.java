package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.Things;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerLevel.class)
abstract class FreezeMixin {
    @ModifyArg(method = "tickPrecipitation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), index = 1)
    private BlockState backcountry$freshIceIsThin(BlockState state) {
        return state.is(Blocks.ICE) ? Things.THIN_ICE.get().defaultBlockState() : state;
    }
}
