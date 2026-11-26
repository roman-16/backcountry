package io.github.roman16.backcountry.gear;

import com.mojang.datafixers.util.Pair;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Hotbar {
    public static final int ROW = 9;
    public static final int WORN_X = 77;

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
        Body body = Metabolism.body(player);
        carried.add(body.binoculars);
        carried.add(body.pack);
        carried.addAll(contents(body.pack));
        return carried;
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
            return !open() && packIndex() >= PackItem.slots(Metabolism.body(inventory.player).pack);
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
            return body.packRows.items(body.pack).get(packIndex());
        }

        @Override
        public void set(ItemStack stack) {
            if (open()) {
                super.set(stack);
                return;
            }
            if (!locked()) {
                Body body = Metabolism.body(inventory.player);
                body.packRows.items(body.pack).set(packIndex(), stack);
            }
            setChanged();
        }

        @Override
        public void setChanged() {
            if (!open() && !locked()) {
                Body body = Metabolism.body(inventory.player);
                body.packRows.save(body.pack);
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

    public enum Worn {
        BACK(44, PackItem.class, body -> body.pack, (body, stack) -> body.pack = stack),
        NECK(26, BinocularsItem.class, body -> body.binoculars, (body, stack) -> body.binoculars = stack);

        public final int y;
        private final Class<? extends Item> fits;
        private final Function<Body, ItemStack> get;
        private final BiConsumer<Body, ItemStack> set;
        private final ResourceLocation empty;

        Worn(int y, Class<? extends Item> fits, Function<Body, ItemStack> get, BiConsumer<Body, ItemStack> set) {
            this.y = y;
            this.fits = fits;
            this.get = get;
            this.set = set;
            this.empty = Backcountry.id("item/empty_slot_" + name().toLowerCase(Locale.ROOT));
        }
    }

    public static final class WornSlot extends Slot {
        private final Player player;
        private final Worn worn;

        public WornSlot(Player player, Worn worn) {
            super(new SimpleContainer(1), 0, WORN_X, worn.y);
            this.player = player;
            this.worn = worn;
        }

        @Override
        public ItemStack getItem() {
            return worn.get.apply(Metabolism.body(player));
        }

        @Override
        public void set(ItemStack stack) {
            worn.set.accept(Metabolism.body(player), stack);
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack taken = getItem();
            worn.set.accept(Metabolism.body(player), ItemStack.EMPTY);
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return worn.fits.isInstance(stack.getItem());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return !player.isCreative();
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, worn.empty);
        }
    }

    @SubscribeEvent
    static void pickUp(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        Body body = Metabolism.body(player);
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide || player.isCreative() || body.pack.isEmpty() || stack.getItem() instanceof PackItem || entity.hasPickUpDelay()
                || entity.getTarget() != null && !entity.getTarget().equals(player.getUUID())) {
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
