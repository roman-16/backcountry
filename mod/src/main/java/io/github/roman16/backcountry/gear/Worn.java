package io.github.roman16.backcountry.gear;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public enum Worn {
    BACK("back"),
    HANDS("hands"),
    NECK("necklace");

    public final String slot;

    Worn(String slot) {
        this.slot = slot;
    }

    private Optional<IDynamicStackHandler> stacks(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).flatMap(inventory -> inventory.getStacksHandler(slot)).filter(handler -> handler.getSlots() > 0)
                .map(handler -> handler.getStacks());
    }

    public ItemStack get(LivingEntity entity) {
        return stacks(entity).map(stacks -> stacks.getStackInSlot(0)).orElse(ItemStack.EMPTY);
    }

    public void set(LivingEntity entity, ItemStack stack) {
        stacks(entity).ifPresent(stacks -> stacks.setStackInSlot(0, stack));
    }

    public ItemStack take(LivingEntity entity) {
        ItemStack taken = get(entity);
        set(entity, ItemStack.EMPTY);
        return taken;
    }
}
