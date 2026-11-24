package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.trap.Wilds;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.riverfishing.engine.BarometricPressure")
abstract class RiverPressureMixin {
    @Inject(method = "hPa", at = @At("HEAD"), cancellable = true, remap = false)
    private static void backcountry$readsTheSky(ServerLevel level, CallbackInfoReturnable<Double> result) {
        result.setReturnValue(Wilds.pressure(level, Calendar.day(level), Climate.hour(level)));
    }

    @Inject(method = "trend", at = @At("HEAD"), cancellable = true, remap = false)
    private static void backcountry$followsTheSky(ServerLevel level, CallbackInfoReturnable<Double> result) {
        result.setReturnValue(Wilds.pressureTrend(level));
    }
}
