package io.github.roman16.backcountry.mixin;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.pandasystems.pandalib.config.ConfigSynchronizer$SyncableOption", remap = false)
abstract class PandaLibSyncMixin {
    @Redirect(method = "get(Ljava/util/UUID;)Ljava/lang/Object;", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;)V"), remap = false)
    private void backcountry$defaultQuietly(Logger logger, String warning) {}
}
