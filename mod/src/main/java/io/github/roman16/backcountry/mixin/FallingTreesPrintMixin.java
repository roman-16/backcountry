package io.github.roman16.backcountry.mixin;

import java.io.PrintStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.pandasystems.fallingtrees.trees.GenericTree", remap = false)
abstract class FallingTreesPrintMixin {
    @Redirect(method = "gatherTreeData", at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/Object;)V"), remap = false)
    private void backcountry$quiet(PrintStream out, Object blocks) {}
}
