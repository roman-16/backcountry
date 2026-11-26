package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.Backcountry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.config.ConfigManager", remap = false)
abstract class SodiumConfigOrderMixin {
    @ModifyArg(method = "registerConfigs", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/config/structure/Config;<init>(Ljava/util/List;)V"))
    private static List<ModOptions> backcountry$first(List<ModOptions> mods) {
        List<ModOptions> ordered = new ArrayList<>(mods);
        ordered.sort(Comparator.comparing(mod -> !mod.configId().equals(Backcountry.MODID)));
        return ordered;
    }
}
