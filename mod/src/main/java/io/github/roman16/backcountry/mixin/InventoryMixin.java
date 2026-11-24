package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.gear.Hotbar;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
    private void backcountry$hotbarFree(CallbackInfoReturnable<Integer> result) {
        if (!player.isCreative() && result.getReturnValue() >= Hotbar.ROW) {
            result.setReturnValue(-1);
        }
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
    private void backcountry$hotbarSpace(CallbackInfoReturnable<Integer> result) {
        int slot = result.getReturnValue();
        if (!player.isCreative() && slot >= Hotbar.ROW && slot < Inventory.INVENTORY_SIZE) {
            result.setReturnValue(-1);
        }
    }
}
