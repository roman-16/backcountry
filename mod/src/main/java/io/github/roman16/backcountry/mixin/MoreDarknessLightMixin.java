package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.client.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.payangar.moredarkness.darkness.DarknessCalculator", remap = false)
abstract class MoreDarknessLightMixin {
    @Shadow
    private static boolean active;

    @Inject(method = "updateLuminance", at = @At("HEAD"), cancellable = true)
    private static void backcountry$leaveNightToShader(CallbackInfo info) {
        if (Shaders.drawing()) {
            active = false;
            info.cancel();
        }
    }
}
