package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.gear.Hotbar;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
    @ModifyVariable(method = "addSlot", at = @At("HEAD"), argsOnly = true)
    private Slot backcountry$lockRows(Slot slot) {
        return Hotbar.replace(slot);
    }
}
