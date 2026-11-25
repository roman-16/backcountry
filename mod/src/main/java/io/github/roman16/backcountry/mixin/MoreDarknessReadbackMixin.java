package io.github.roman16.backcountry.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.roman16.backcountry.client.LuminanceReadback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.payangar.moredarkness.darkness.PerceptionEffects", remap = false)
abstract class MoreDarknessReadbackMixin {
    @Redirect(method = "readBackLuminance",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;downloadTexture(IZ)V"))
    private static void backcountry$readWithoutWaiting(NativeImage image, int level, boolean opaque) {
        LuminanceReadback.request();
    }

    @Redirect(method = "readBackLuminance",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;getPixelRGBA(II)I"))
    private static int backcountry$latestPixel(NativeImage image, int x, int y) {
        return LuminanceReadback.pixel();
    }
}
