package io.github.roman16.backcountry.mixin.dev;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leclowndu93150.particular.NeoForgeClientEvents$ForgeEvents")
abstract class CascadesCounterMixin {
    @Inject(method = "onRenderHUD", at = @At("HEAD"), cancellable = true, remap = false)
    private static void backcountry$hide(RenderGuiEvent.Post event, CallbackInfo info) {
        info.cancel();
    }
}
