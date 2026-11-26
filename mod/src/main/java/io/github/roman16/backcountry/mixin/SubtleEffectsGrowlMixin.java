package io.github.roman16.backcountry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.roman16.backcountry.client.ClientState;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "einstein.subtle_effects.ticking.tickers.entity.StomachGrowlingTicker", remap = false)
abstract class SubtleEffectsGrowlMixin {
    private static final int EMPTY = 0;
    private static final int FED = Integer.MAX_VALUE;

    @WrapOperation(method = "entityTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getFoodLevel()I"))
    private int backcountry$growlsOnAnEmptyStomach(FoodData food, Operation<Integer> original) {
        return ClientState.stomachEmpty() ? EMPTY : FED;
    }
}
