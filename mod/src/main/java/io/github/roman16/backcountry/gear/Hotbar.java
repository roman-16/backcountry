package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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

    private Hotbar() {}

    public static Slot replace(Slot slot) {
        if (slot.getClass() == Slot.class && slot.container instanceof Inventory inventory && slot.getContainerSlot() >= ROW
                && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
            return new BodySlot(inventory, slot.getContainerSlot(), slot.x, slot.y);
        }
        return slot;
    }

    public static List<ItemStack> carried(Player player) {
        List<ItemStack> carried = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < ROW; slot++) {
            carried.add(inventory.getItem(slot));
        }
        carried.add(player.getOffhandItem());
        inventory.armor.forEach(carried::add);
        for (Worn worn : Worn.values()) {
            carried.add(worn.get(player));
        }
        carried.addAll(contents(Worn.BACK.get(player)));
        return carried;
    }

    public static int count(Player player, Predicate<ItemStack> wanted) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < ROW; slot++) {
            count += wanted.test(inventory.getItem(slot)) ? inventory.getItem(slot).getCount() : 0;
        }
        count += wanted.test(player.getOffhandItem()) ? player.getOffhandItem().getCount() : 0;
        for (ItemStack inside : contents(Worn.BACK.get(player))) {
            count += wanted.test(inside) ? inside.getCount() : 0;
        }
        return count;
    }

    public static void take(Player player, Predicate<ItemStack> wanted, int amount) {
        int left = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < ROW && left > 0; slot++) {
            left -= shrink(inventory.getItem(slot), wanted, left);
        }
        left -= shrink(player.getOffhandItem(), wanted, left);
        ItemStack pack = Worn.BACK.get(player);
        if (left > 0 && !pack.isEmpty()) {
            NonNullList<ItemStack> items = contents(pack);
            for (ItemStack inside : items) {
                left -= shrink(inside, wanted, left);
            }
            store(pack, items);
        }
    }

    private static int shrink(ItemStack stack, Predicate<ItemStack> wanted, int left) {
        if (left <= 0 || !wanted.test(stack)) {
            return 0;
        }
        int taken = Math.min(left, stack.getCount());
        stack.shrink(taken);
        return taken;
    }

    public static NonNullList<ItemStack> contents(ItemStack pack) {
        NonNullList<ItemStack> items = NonNullList.withSize(PackItem.slots(pack), ItemStack.EMPTY);
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
            return !open() && packIndex() >= PackItem.slots(Worn.BACK.get(inventory.player));
        }

        public Optional<Component> needs() {
            int hidePack = Balance.get().load().hidePackRows() * ROW;
            if (!locked() || packIndex() >= hidePack) {
                return Optional.empty();
            }
            int canvasPack = Balance.get().load().canvasPackRows() * ROW;
            return Optional.of(Component.translatable(packIndex() < canvasPack ? "tooltip.backcountry.needs_pack" : "tooltip.backcountry.needs_hide_pack"));
        }

        @Override
        public ItemStack getItem() {
            if (open()) {
                return super.getItem();
            }
            if (locked()) {
                return ItemStack.EMPTY;
            }
            Body body = Metabolism.body(inventory.player);
            return body.packRows.items(Worn.BACK.get(inventory.player)).get(packIndex());
        }

        @Override
        public void set(ItemStack stack) {
            if (open()) {
                super.set(stack);
                return;
            }
            if (!locked()) {
                Body body = Metabolism.body(inventory.player);
                body.packRows.items(Worn.BACK.get(inventory.player)).set(packIndex(), stack);
            }
            setChanged();
        }

        @Override
        public void setChanged() {
            if (!open() && !locked()) {
                Body body = Metabolism.body(inventory.player);
                body.packRows.save(Worn.BACK.get(inventory.player));
            }
            super.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            if (open()) {
                return super.remove(amount);
            }
            ItemStack taken = getItem().split(amount);
            setChanged();
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (open()) {
                return super.mayPlace(stack);
            }
            return !locked() && !(stack.getItem() instanceof PackItem);
        }

        @Override
        public boolean mayPickup(Player player) {
            return open() || !locked();
        }
    }

    @SubscribeEvent
    static void pickUp(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemStack pack = Worn.BACK.get(player);
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide || player.isCreative() || pack.isEmpty() || stack.getItem() instanceof PackItem || entity.hasPickUpDelay()
                || entity.getTarget() != null && !entity.getTarget().equals(player.getUUID())) {
            return;
        }
        int over = stack.getCount() - hotbarRoom(player.getInventory(), stack);
        if (over <= 0) {
            return;
        }
        NonNullList<ItemStack> items = contents(pack);
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
            store(pack, items);
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
