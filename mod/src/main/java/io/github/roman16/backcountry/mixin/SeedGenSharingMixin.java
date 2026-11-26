package io.github.roman16.backcountry.mixin;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mobius.seedrenderer.net.SeedSharing", remap = false)
abstract class SeedGenSharingMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "say", at = @At("HEAD"), cancellable = true)
    private static void backcountry$logOnly(String line, CallbackInfo info) {
        LOGGER.info("SeedGen: {}", line);
        info.cancel();
    }
}
