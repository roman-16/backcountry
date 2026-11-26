package io.github.roman16.backcountry.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
abstract class WorldOpenFlowsMixin {
    @Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
    private static void backcountry$withoutWarning(Minecraft minecraft, CreateWorldScreen screen, Lifecycle lifecycle, Runnable loadWorld, boolean skipWarnings,
            CallbackInfo info) {
        loadWorld.run();
        info.cancel();
    }
}
