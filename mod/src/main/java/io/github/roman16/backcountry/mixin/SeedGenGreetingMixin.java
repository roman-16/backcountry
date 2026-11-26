package io.github.roman16.backcountry.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mobius.seedrenderer.SeedRenderer", remap = false)
abstract class SeedGenGreetingMixin {
    @Inject(method = "greet", at = @At("HEAD"), cancellable = true)
    private static void backcountry$quiet(ServerPlayer player, CallbackInfo info) {
        info.cancel();
    }
}
