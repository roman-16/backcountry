package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.client.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.payangar.moredarkness.darkness.PerceptionEffects", remap = false)
abstract class MoreDarknessPerceptionMixin {
    @Inject(method = {"processFrame", "processDarkSight"}, at = @At("HEAD"), cancellable = true)
    private static void backcountry$leavePassesToShader(CallbackInfo info) {
        if (Shaders.drawing()) {
            info.cancel();
        }
    }
}
