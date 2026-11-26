package io.github.roman16.backcountry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.roman16.backcountry.body.Climate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "einstein.subtle_effects.ticking.tickers.entity.FrostyBreathTicker", remap = false)
abstract class SubtleEffectsBreathMixin {
    @WrapOperation(method = "entityTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;coldEnoughToSnow(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean backcountry$breathFollowsTheAir(Biome biome, BlockPos pos, Operation<Boolean> original) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && Climate.breathShows(level, pos);
    }
}
