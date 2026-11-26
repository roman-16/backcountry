package io.github.roman16.backcountry.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.roman16.backcountry.client.Binoculars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerMixin {
    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
    private float backcountry$binocularsZoom(float modifier) {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson() && ((AbstractClientPlayer) (Object) this).isScoping() ? Binoculars.fovModifier() : modifier;
    }
}
