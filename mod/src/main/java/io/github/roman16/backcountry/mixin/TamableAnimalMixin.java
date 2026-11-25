package io.github.roman16.backcountry.mixin;

import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
abstract class TamableAnimalMixin {
    @Inject(method = "setTame", at = @At("HEAD"), cancellable = true)
    private void backcountry$staysWild(boolean tame, boolean applyTamingSideEffects, CallbackInfo info) {
        if (tame) {
            info.cancel();
        }
    }
}
