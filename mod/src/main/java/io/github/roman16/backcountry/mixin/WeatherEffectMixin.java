package io.github.roman16.backcountry.mixin;

import com.teamtea.eclipticseasons.api.data.weather.special_effect.WeatherEffect;
import com.teamtea.eclipticseasons.api.util.WeatherUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WeatherUtil.class, remap = false)
abstract class WeatherEffectMixin {
    @Inject(method = "getWeatherEffectByEntity", at = @At("HEAD"), cancellable = true)
    private static void backcountry$none(Entity entity, CallbackInfoReturnable<WeatherEffect> info) {
        info.setReturnValue(null);
    }
}
