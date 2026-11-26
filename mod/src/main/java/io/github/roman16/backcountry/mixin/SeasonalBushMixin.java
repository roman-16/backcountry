package io.github.roman16.backcountry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.roman16.backcountry.food.Seasons;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleBlockFeature.class)
abstract class SeasonalBushMixin {
    @ModifyExpressionValue(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;getState(Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState backcountry$asTheDateStands(BlockState state, @Local(argsOnly = true) FeaturePlaceContext<SimpleBlockConfiguration> context) {
        return state.getBlock() instanceof SweetBerryBushBlock ? state.setValue(SweetBerryBushBlock.AGE, Seasons.ripest(context.level().getLevel(), state.getBlock())) : state;
    }
}
