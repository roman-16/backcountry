package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.survivor.Team;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CacheMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int CACHE_Y = 92;
    public static final int INVENTORY_Y = 148;
    public static final int SLOT = 18;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int HOTBAR_GAP = 4;
    private static final int SHORT = 1 << 15;
    private static final int TENTHS = 10;
    private static final int HUNDREDTHS = 100;
    private static final int KCAL_HIGH = 0;
    private static final int KCAL_LOW = 1;
    private static final int DAYS = 2;
    private static final int RATION = 3;
    private static final int FAT_HIGH = 4;
    private static final int FAT_LOW = 5;
    private static final int SPOILING = 6;
    private static final int AIR = 7;
    private static final int SCENT = 8;
    private static final int REACH = 9;
    private static final int VALUES = 10;

    private final Container cache;
    private final ContainerData data;

    public CacheMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(CacheEntity.SLOTS), new SimpleContainerData(VALUES));
        buffer.readBlockPos();
    }

    public CacheMenu(int id, Inventory inventory, CacheEntity cache) {
        this(id, inventory, cache, new Values(cache, inventory.player));
    }

    private CacheMenu(int id, Inventory inventory, Container cache, ContainerData data) {
        super(Things.FOOD_CACHE_MENU.get(), id);
        this.cache = cache;
        this.data = data;
        cache.startOpen(inventory.player);
        for (int slot = 0; slot < CacheEntity.SLOTS; slot++) {
            addSlot(new Slot(cache, slot, 8 + slot % COLUMNS * SLOT, CACHE_Y + slot / COLUMNS * SLOT));
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

    private int split(int high, int low) {
        return data.get(high) * SHORT + data.get(low);
    }

    public int kcal() {
        return split(KCAL_HIGH, KCAL_LOW);
    }

    public double days() {
        return data.get(DAYS) / (double) TENTHS;
    }

    public Ration ration() {
        return Ration.values()[data.get(RATION)];
    }

    public int fatKcal() {
        return split(FAT_HIGH, FAT_LOW);
    }

    public double spoiling() {
        return data.get(SPOILING) / (double) HUNDREDTHS;
    }

    public int airC() {
        return data.get(AIR);
    }

    public boolean scented() {
        return data.get(SCENT) != 0;
    }

    public CacheEntity.Reach reach() {
        return CacheEntity.Reach.values()[Math.floorMod(data.get(REACH), CacheEntity.Reach.values().length)];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level() instanceof ServerLevel level) || id < 0 || id >= Ration.values().length) {
            return false;
        }
        Team.get(level.getServer()).ration(Ration.values()[id]);
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
        boolean done = index < CacheEntity.SLOTS ? moveItemStackTo(stack, CacheEntity.SLOTS, slots.size(), true) : moveItemStackTo(stack, 0, CacheEntity.SLOTS, false);
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
        return cache.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        cache.stopOpen(player);
    }

    private record Values(CacheEntity cache, Player player) implements ContainerData {
        @Override
        public int get(int index) {
            Team team = player.level() instanceof ServerLevel level ? Team.get(level.getServer()) : null;
            Ration ration = team == null ? Ration.NORMAL : team.ration();
            Body body = Metabolism.body(player);
            Balance.Body rules = Balance.get().body();
            int kcal = (int) Math.round(cache.usableKcal());
            int fat = (int) Math.max(0, Math.round((body.weightKg - rules.extractionBmi() * rules.heightM() * rules.heightM()) * rules.kcalPerKg()));
            return switch (index) {
                case KCAL_HIGH -> kcal / SHORT;
                case KCAL_LOW -> kcal % SHORT;
                case DAYS -> (int) Math.round(cache.usableKcal() / ration.dayKcal() * TENTHS);
                case RATION -> ration.ordinal();
                case FAT_HIGH -> fat / SHORT;
                case FAT_LOW -> fat % SHORT;
                case SPOILING -> (int) Math.round(Spoilage.rate(cache.airC()) * HUNDREDTHS);
                case AIR -> (int) Math.round(cache.airC());
                case SCENT -> cache.scented() ? 1 : 0;
                case REACH -> cache.reach().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return VALUES;
        }
    }
}
