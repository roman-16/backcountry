package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.client.Panorama;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CubeMap.class)
abstract class CubeMapMixin {
    @Shadow
    @Final
    private ResourceLocation[] images;

    @Unique
    private boolean backcountry$dressed;

    @Inject(method = "preload", at = @At("HEAD"))
    private void backcountry$beforeLoading(TextureManager textures, Executor executor, CallbackInfoReturnable<CompletableFuture<Void>> info) {
        backcountry$dress();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void backcountry$beforeDrawing(Minecraft minecraft, float pitch, float yaw, float alpha, CallbackInfo info) {
        backcountry$dress();
    }

    @Unique
    private void backcountry$dress() {
        if (!backcountry$dressed) {
            backcountry$dressed = true;
            Panorama.dress(images);
        }
    }
}
