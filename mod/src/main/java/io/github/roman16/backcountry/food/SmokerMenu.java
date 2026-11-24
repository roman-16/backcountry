package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.fire.FirewoodItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SmokerMenu extends AbstractContainerMenu {
    public static final int HANGING_X = 62;
    public static final int HANGING_Y = 20;
    public static final int FIREWOOD_X = 80;
    public static final int FIREWOOD_Y = 44;
    public static final int INVENTORY_Y = 118;
    public static final int SLOT = 18;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int HOTBAR_GAP = 4;
    private static final int MINUTES_AN_HOUR = 60;
    private static final int SMOKE = 0;
    private static final int BATCH = 1;
    private static final int BURN = 2;
    private static final int VALUES = 3;

    private final Container smoker;
    private final ContainerData data;

    public SmokerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(SmokerEntity.SLOTS), new SimpleContainerData(VALUES));
        buffer.readBlockPos();
    }

    public SmokerMenu(int id, Inventory inventory, SmokerEntity smoker) {
        this(id, inventory, smoker, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case SMOKE -> smoker.smoke().ordinal();
                    case BATCH -> (int) Math.round(smoker.batchHours() * MINUTES_AN_HOUR);
                    case BURN -> (int) Math.round(smoker.burnHours() * MINUTES_AN_HOUR);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return VALUES;
            }
        });
    }

    private SmokerMenu(int id, Inventory inventory, Container smoker, ContainerData data) {
        super(Things.SMOKER_MENU.get(), id);
        this.smoker = smoker;
        this.data = data;
        for (int slot = 0; slot < SmokerEntity.HANGING; slot++) {
            addSlot(new Slot(smoker, slot, HANGING_X + slot * SLOT, HANGING_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SmokerEntity.hangs(stack);
                }
            });
        }
        for (int slot = 0; slot < SmokerEntity.FIREWOOD; slot++) {
            addSlot(new Slot(smoker, SmokerEntity.HANGING + slot, FIREWOOD_X + slot * SLOT, FIREWOOD_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FirewoodItem.catches(stack);
                }
            });
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(inventory, COLUMNS + row * COLUMNS + column, 8 + column * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(inventory, column, 8 + column * SLOT, INVENTORY_Y + ROWS * SLOT + HOTBAR_GAP));
        }
        addDataSlots(data);
    }

    public SmokerEntity.Smoke smoke() {
        return SmokerEntity.Smoke.values()[data.get(SMOKE)];
    }

    public int batchMinutes() {
        return data.get(BATCH);
    }

    public int burnMinutes() {
        return data.get(BURN);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean done;
        if (index < SmokerEntity.SLOTS) {
            done = moveItemStackTo(stack, SmokerEntity.SLOTS, slots.size(), true);
        } else if (SmokerEntity.hangs(stack)) {
            done = moveItemStackTo(stack, 0, SmokerEntity.HANGING, false);
        } else {
            done = FirewoodItem.catches(stack) && moveItemStackTo(stack, SmokerEntity.HANGING, SmokerEntity.SLOTS, false);
        }
        if (!done) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return smoker.stillValid(player);
    }
}
