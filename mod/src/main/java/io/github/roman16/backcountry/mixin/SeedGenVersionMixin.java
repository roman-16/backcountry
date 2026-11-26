package io.github.roman16.backcountry.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mobius.seedrenderer.net.SeedShare$Mod", remap = false)
abstract class SeedGenVersionMixin {
    @Shadow
    @Final
    @Mutable
    private String version;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void backcountry$noVersionIsEmpty(CallbackInfo info) {
        if (version == null) {
            version = "";
        }
    }
}
