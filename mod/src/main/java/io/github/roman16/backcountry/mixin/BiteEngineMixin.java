package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.Cuts;
import java.lang.reflect.Field;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.riverfishing.engine.BiteEngine")
abstract class BiteEngineMixin {
    private static Field species;

    @Inject(method = "environmentScore", at = @At("HEAD"), cancellable = true, remap = false)
    private static void backcountry$onlyAlaskasFish(@Coerce Object profile, @Coerce Object context, CallbackInfoReturnable<Double> result) {
        if (!Cuts.livesOnTheIsland(species(profile))) {
            result.setReturnValue(0.0);
        }
    }

    private static ResourceLocation species(Object profile) {
        try {
            if (species == null) {
                species = profile.getClass().getField("id");
            }
            return (ResourceLocation) species.get(profile);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("River Fishing's fish profile has no id", exception);
        }
    }
}
