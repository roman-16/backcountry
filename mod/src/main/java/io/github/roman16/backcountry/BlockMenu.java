package io.github.roman16.backcountry;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class BlockMenu extends AbstractContainerMenu {
    public static final int SLOT = 18;
    public static final int INVENTORY_Y = 118;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int HOTBAR_GAP = 4;
    private static final int LEFT = 8;
    protected final Container store;

    protected BlockMenu(MenuType<?> type, int id, Container store) {
        super(type, id);
        this.store = store;
    }

    protected void inventory(Inventory inventory) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(inventory, COLUMNS + row * COLUMNS + column, LEFT + column * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(inventory, column, LEFT + column * SLOT, INVENTORY_Y + ROWS * SLOT + HOTBAR_GAP));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        int size = store.getContainerSize();
        boolean done = index < size ? moveItemStackTo(stack, size, slots.size(), true) : moveIntoStore(stack, size);
        if (!done) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, moved);
        return moved;
    }

    private boolean moveIntoStore(ItemStack stack, int size) {
        for (int index = 0; index < size && !stack.isEmpty(); index++) {
            Slot target = slots.get(index);
            if (!target.hasItem() && target.mayPlace(stack)) {
                target.setByPlayer(stack.split(1));
                return true;
            }
        }
        return false;
    }

    public static class StoreSlot extends Slot {
        public StoreSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return store.stillValid(player);
    }
}
