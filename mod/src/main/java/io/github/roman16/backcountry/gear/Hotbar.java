package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Hotbar {
    public static final int ROW = 9;
    public static final int BACK_X = 77;
    public static final int BACK_Y = 44;

    private Hotbar() {}

    public static Slot replace(Slot slot) {
        if (slot.getClass() == Slot.class && slot.container instanceof Inventory inventory && slot.getContainerSlot() >= ROW
                && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
            return new BodySlot(inventory, slot.getContainerSlot(), slot.x, slot.y);
        }
        return slot;
    }

    public static int packSlots() {
        return Balance.get().load().packRows() * ROW;
    }

    public static NonNullList<ItemStack> contents(ItemStack pack) {
        NonNullList<ItemStack> items = NonNullList.withSize(packSlots(), ItemStack.EMPTY);
        pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void store(ItemStack pack, NonNullList<ItemStack> items) {
        pack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public static final class BodySlot extends Slot {
        private final Inventory inventory;

        BodySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.inventory = inventory;
        }

        private boolean open() {
            return inventory.player.isCreative();
        }

        private int packIndex() {
            return getContainerSlot() - ROW;
        }

        public boolean locked() {
            return !open() && (Metabolism.body(inventory.player).pack.isEmpty() || packIndex() >= packSlots());
        }

        @Override
        public ItemStack getItem() {
            if (open()) {
                return super.getItem();
            }
            if (locked()) {
                return ItemStack.EMPTY;
            }
            return contents(Metabolism.body(inventory.player).pack).get(packIndex());
        }

        @Override
        public void set(ItemStack stack) {
            if (open()) {
                super.set(stack);
                return;
            }
            if (!locked()) {
                Body body = Metabolism.body(inventory.player);
                NonNullList<ItemStack> items = contents(body.pack);
                items.set(packIndex(), stack);
                store(body.pack, items);
            }
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            if (open()) {
                return super.remove(amount);
            }
            ItemStack held = getItem().copy();
            ItemStack taken = held.split(amount);
            set(held);
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (open()) {
                return super.mayPlace(stack);
            }
            return !locked() && !stack.is(Things.HIDE_PACK.get());
        }

        @Override
        public boolean mayPickup(Player player) {
            return open() || !locked();
        }
    }

    public static final class BackSlot extends Slot {
        private final Player player;

        public BackSlot(Player player) {
            super(new SimpleContainer(1), 0, BACK_X, BACK_Y);
            this.player = player;
        }

        @Override
        public ItemStack getItem() {
            return Metabolism.body(player).pack;
        }

        @Override
        public void set(ItemStack stack) {
            Metabolism.body(player).pack = stack;
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack pack = Metabolism.body(player).pack;
            Metabolism.body(player).pack = ItemStack.EMPTY;
            return pack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Things.HIDE_PACK.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return !player.isCreative();
        }
    }

    @SubscribeEvent
    static void pickUp(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        Body body = Metabolism.body(player);
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide || player.isCreative() || body.pack.isEmpty() || stack.is(Things.HIDE_PACK.get())) {
            return;
        }
        int over = stack.getCount() - hotbarRoom(player.getInventory(), stack);
        if (over <= 0) {
            return;
        }
        NonNullList<ItemStack> items = contents(body.pack);
        int moved = 0;
        for (int i = 0; i < items.size() && moved < over; i++) {
            ItemStack inside = items.get(i);
            if (inside.isEmpty()) {
                int count = Math.min(over - moved, stack.getMaxStackSize());
                items.set(i, stack.copyWithCount(count));
                moved += count;
            } else if (ItemStack.isSameItemSameComponents(inside, stack)) {
                int count = Math.min(over - moved, inside.getMaxStackSize() - inside.getCount());
                inside.grow(count);
                moved += count;
            }
        }
        if (moved > 0) {
            store(body.pack, items);
            stack.shrink(moved);
            entity.setItem(stack);
        }
    }

    private static int hotbarRoom(Inventory inventory, ItemStack stack) {
        int room = 0;
        for (int slot = 0; slot < ROW; slot++) {
            ItemStack held = inventory.getItem(slot);
            if (held.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(held, stack)) {
                room += held.getMaxStackSize() - held.getCount();
            }
        }
        return room;
    }
}
