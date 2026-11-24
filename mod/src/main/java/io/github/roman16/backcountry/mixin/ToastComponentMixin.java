package io.github.roman16.backcountry.mixin;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastComponent.class)
abstract class ToastComponentMixin {
    private static final String FIELD_GUIDE_ENTRY = "net.imasillylittleguy.cnc.client.toasts.FieldGuideNotificationToast";

    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void backcountry$quiet(Toast toast, CallbackInfo info) {
        if (toast instanceof AdvancementToast || toast instanceof RecipeToast || toast.getClass().getName().equals(FIELD_GUIDE_ENTRY)) {
            info.cancel();
        }
    }
}
