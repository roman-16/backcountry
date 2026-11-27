package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.BlockMenu;
import io.github.roman16.backcountry.Things;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class WoodpileMenu extends BlockMenu {
    public static final int PIECES_X = 8;
    public static final int PIECES_Y = 20;
    public static final int PER_ROW = 8;

    public WoodpileMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(Balance.get().firewood().pilePieces()));
        buffer.readBlockPos();
    }

    public WoodpileMenu(int id, Inventory inventory, Container woodpile) {
        super(Things.WOODPILE_MENU.get(), id, woodpile);
        for (int slot = 0; slot < woodpile.getContainerSize(); slot++) {
            addSlot(new StoreSlot(woodpile, slot, PIECES_X + (slot % PER_ROW) * SLOT, PIECES_Y + (slot / PER_ROW) * SLOT) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof FirewoodItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        inventory(inventory);
    }

    public double seasoning() {
        return slots.subList(0, store.getContainerSize()).stream().filter(slot -> slot.hasItem()).mapToDouble(slot -> FirewoodItem.seasoning(slot.getItem())).average()
                .orElse(0);
    }

    public int pieces() {
        return (int) slots.subList(0, store.getContainerSize()).stream().filter(slot -> slot.hasItem()).count();
    }
}
