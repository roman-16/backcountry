package io.github.roman16.backcountry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Biome.class)
abstract class LakeIceMixin {
    @WrapOperation(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;warmEnoughToRain(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean backcountry$freezesWhereTheAirFreezes(Biome biome, BlockPos pos, Operation<Boolean> original, @Local(argsOnly = true) LevelReader reader) {
        if (!(reader instanceof Level level)) {
            return original.call(biome, pos);
        }
        return Climate.airC(level, pos, Calendar.day(level)) >= Balance.get().ice().freezeBelowC();
    }
}
