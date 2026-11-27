package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Unit;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DryingRackEntity extends BlockEntity {
    public static final int SLOTS = 4;
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double HOURS_A_DAY = 24;
    private static final int FIRE_BELOW = 2;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final double[] dried = new double[SLOTS];
    private long clock;
    private boolean awake;

    public DryingRackEntity(BlockPos pos, BlockState state) {
        super(Things.DRYING_RACK_ENTITY.get(), pos, state);
    }

    public static boolean hangs(ItemStack stack) {
        return Pantry.meaty(stack) && raw(stack) && !stack.has(Things.SMOKED.get()) && !stack.has(Things.DRIED.get()) && !Spoilage.spoiled(stack);
    }

    private static boolean raw(ItemStack stack) {
        return Nutrition.of(stack).map(Nutrition::raw).orElse(false);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public double driedShare(int slot) {
        return dried[slot];
    }

    public boolean hang(ItemStack stack) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, stack.split(1));
                dried[slot] = 0;
                changed();
                return true;
            }
        }
        return false;
    }

    public ItemStack take() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!items.get(slot).isEmpty()) {
                ItemStack taken = items.get(slot);
                items.set(slot, ItemStack.EMPTY);
                dried[slot] = 0;
                changed();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean rainedOn() {
        return level != null && Fire.rainedOn(level, worldPosition);
    }

    public boolean smoked() {
        if (level == null) {
            return false;
        }
        for (int down = 1; down <= FIRE_BELOW; down++) {
            BlockPos below = worldPosition.below(down);
            if (level.getBlockState(below).getBlock() instanceof FireBlock && level.getBlockEntity(below) instanceof CampfireBlockEntity campfire && Fire.of(campfire).lit()) {
                return true;
            }
        }
        return false;
    }

    public void advance(double hours) {
        if (level == null || hours <= 0) {
            return;
        }
        double air = Spoilage.airC(level, worldPosition);
        boolean wet = rainedOn();
        double rate = wet ? 0 : (smoked() ? Balance.get().drying().smokeFactor() : 1) / (Balance.get().drying().days() * HOURS_A_DAY);
        boolean changed = false;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack piece = items.get(slot);
            if (piece.isEmpty() || piece.has(Things.DRIED.get())) {
                continue;
            }
            Spoilage.age(piece, hours / HOURS_A_DAY, air);
            if (Spoilage.spoiled(piece)) {
                continue;
            }
            dried[slot] += hours * rate;
            if (dried[slot] >= 1) {
                piece.remove(Things.AGE.get());
                piece.set(Things.DRIED.get(), Unit.INSTANCE);
            }
            changed = true;
        }
        if (changed) {
            changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DryingRackEntity rack) {
        long now = level.getDayTime();
        if (!rack.awake) {
            rack.awake = true;
            rack.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) {
            return;
        }
        double hours = Math.max(0, now - rack.clock) / TICKS_AN_HOUR;
        rack.clock = now;
        rack.advance(hours);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        for (int slot = 0; slot < SLOTS; slot++) {
            tag.putDouble("dried_" + slot, dried[slot]);
        }
        tag.putLong("clock", clock);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        for (int slot = 0; slot < SLOTS; slot++) {
            dried[slot] = tag.getDouble("dried_" + slot);
        }
        clock = tag.getLong("clock");
        awake = tag.contains("clock");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, provider);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
