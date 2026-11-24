package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.IslandShape;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DensityFunctions.Spline.Coordinate.class)
abstract class SplineCoordinateMixin {
    @Inject(method = "mapAll", at = @At("HEAD"), cancellable = true)
    private void backcountry$shapeTheIsland(DensityFunction.Visitor visitor, CallbackInfoReturnable<DensityFunctions.Spline.Coordinate> result) {
        Holder<DensityFunction> function = ((DensityFunctions.Spline.Coordinate) (Object) this).function();
        if (function.unwrapKey().filter(key -> key.location().equals(IslandShape.continents())).isPresent()) {
            result.setReturnValue(new DensityFunctions.Spline.Coordinate(Holder.direct(IslandShape.around(function.value().mapAll(visitor), visitor))));
        }
    }
}
