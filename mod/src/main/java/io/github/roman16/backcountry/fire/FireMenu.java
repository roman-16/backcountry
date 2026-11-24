package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

public final class FireMenu extends AbstractContainerMenu {
    public static final int QUEUE_X = 70;
    public static final int QUEUE_Y = 20;
    public static final int QUEUE_PITCH = 26;
    public static final int INVENTORY_X = 35;
    public static final int INVENTORY_Y = 150;
    public static final int SLOT = 18;
    private static final int ROWS = 3;
    private static final int COLUMNS = 9;
    private static final int HOTBAR_GAP = 4;
    private static final int REACH_SQUARED = 64;
    private static final int MINUTES_AN_HOUR = 60;
    private static final int TENTHS = 10;
    private static final int BURNING = 0;
    private static final int QUEUED = 1;
    private static final int COALS = 2;
    private static final int HEAT = 3;
    private static final int SETTING = 4;
    private static final int FLAGS = 5;
    private static final int VALUES = 6;
    private static final int LIT = 1;
    private static final int ROOFED = 2;
    private static final int PACKED = 4;
    private static final int BANKED = 8;
    private static final int QUEUED_ANY = 16;

    private final Container queue;
    private final ContainerData data;
    private final CampfireBlockEntity campfire;

    public FireMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(Fire.QUEUE), new SimpleContainerData(VALUES), null);
        buffer.readBlockPos();
    }

    public FireMenu(int id, Inventory inventory, CampfireBlockEntity campfire) {
        this(id, inventory, new Queue(campfire), new Values(campfire), campfire);
    }

    private FireMenu(int id, Inventory inventory, Container queue, ContainerData data, CampfireBlockEntity campfire) {
        super(Things.FIRE_MENU.get(), id);
        this.queue = queue;
        this.data = data;
        this.campfire = campfire;
        for (int slot = 0; slot < Fire.QUEUE; slot++) {
            addSlot(new Slot(queue, slot, QUEUE_X + slot * QUEUE_PITCH, QUEUE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FirewoodItem.catches(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(inventory, COLUMNS + row * COLUMNS + column, INVENTORY_X + column * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * SLOT, INVENTORY_Y + ROWS * SLOT + HOTBAR_GAP));
        }
        addDataSlots(data);
    }

    public int burningMinutes() {
        return data.get(BURNING);
    }

    public int queuedMinutes() {
        return data.get(QUEUED);
    }

    public int coalMinutes() {
        return data.get(COALS);
    }

    public double heatC() {
        return data.get(HEAT) / (double) TENTHS;
    }

    public Fire.Setting setting() {
        return Fire.Setting.values()[data.get(SETTING)];
    }

    public boolean lit() {
        return (data.get(FLAGS) & LIT) != 0;
    }

    public boolean roofed() {
        return (data.get(FLAGS) & ROOFED) != 0;
    }

    public boolean packed() {
        return (data.get(FLAGS) & PACKED) != 0;
    }

    public boolean banked() {
        return (data.get(FLAGS) & BANKED) != 0;
    }

    public boolean queued() {
        return (data.get(FLAGS) & QUEUED_ANY) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (campfire == null || id < 0 || id >= Fire.Setting.values().length) {
            return false;
        }
        Fire.of(campfire).setting = Fire.Setting.values()[id];
        campfire.setChanged();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < Fire.QUEUE) {
            if (!moveItemStackTo(stack, Fire.QUEUE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!FirewoodItem.catches(stack) || !moveItemStackTo(stack, 0, Fire.QUEUE, false)) {
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
        return queue.stillValid(player);
    }

    private record Queue(CampfireBlockEntity campfire) implements Container {
        private Fire fire() {
            return Fire.of(campfire);
        }

        @Override
        public int getContainerSize() {
            return Fire.QUEUE;
        }

        @Override
        public boolean isEmpty() {
            return !fire().queued();
        }

        @Override
        public ItemStack getItem(int slot) {
            return fire().queue.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = ContainerHelper.removeItem(fire().queue, slot, amount);
            setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(fire().queue, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            fire().queue.set(slot, stack);
            setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            campfire.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            BlockPos pos = campfire.getBlockPos();
            return !campfire.isRemoved() && campfire.getLevel() != null && campfire.getLevel().getBlockEntity(pos) == campfire
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= REACH_SQUARED;
        }

        @Override
        public void clearContent() {
            fire().queue.clear();
        }
    }

    private record Values(CampfireBlockEntity campfire) implements ContainerData {
        @Override
        public int get(int index) {
            Fire fire = Fire.of(campfire);
            boolean wet = Fire.rainedOn(campfire.getLevel(), campfire.getBlockPos());
            return switch (index) {
                case BURNING -> minutes(fire.lit() ? fire.burningHours(wet) : 0);
                case QUEUED -> minutes(fire.queuedHours(wet));
                case COALS -> minutes(fire.coalHours());
                case HEAT -> (int) Math.round(fire.heatC() * TENTHS);
                case SETTING -> fire.setting.ordinal();
                case FLAGS -> (fire.lit() ? LIT : 0) | (campfire.getLevel().canSeeSky(campfire.getBlockPos().above()) ? 0 : ROOFED) | (fire.packed() ? PACKED : 0)
                        | (fire.banked() ? BANKED : 0) | (fire.queued() ? QUEUED_ANY : 0);
                default -> 0;
            };
        }

        private static int minutes(double hours) {
            return (int) Math.round(hours * MINUTES_AN_HOUR);
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return VALUES;
        }
    }
}
