package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Inspectable;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.client.Words;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DryingRackEntity extends BlockEntity implements Container, Inspectable {
    public static final int SLOTS = 4;
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double HOURS_A_DAY = 24;
    private static final int FIRE_BELOW = 2;
    private static final int PERCENT = 100;
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

    public double daysToDry(int slot) {
        double perDay = (smoked() ? Balance.get().drying().smokeFactor() : 1) / Balance.get().drying().days();
        return (1 - dried[slot]) / perDay;
    }

    public boolean spoilsFirst(int slot) {
        ItemStack piece = items.get(slot);
        if (level == null || piece.isEmpty() || piece.has(Things.DRIED.get())) {
            return false;
        }
        if (rainedOn()) {
            return true;
        }
        double perDay = (smoked() ? Balance.get().drying().smokeFactor() : 1) / Balance.get().drying().days();
        double aged = Spoilage.rate(Spoilage.airC(level, worldPosition)) * Math.pow(1 - dried[slot], 2) / (2 * perDay);
        return Spoilage.life(piece).map(life -> aged >= life - Spoilage.age(piece)).orElse(false);
    }

    @Override
    public List<Component> inspect() {
        List<Component> lines = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack piece = items.get(slot);
            if (!piece.isEmpty()) {
                lines.add(line(piece, (int) Math.round(dried[slot] * PERCENT), daysToDry(slot), spoilsFirst(slot)));
            }
        }
        if (rainedOn()) {
            lines.add(Component.translatable("message.backcountry.rack_rained"));
        }
        return lines;
    }

    public static Component line(ItemStack piece, int percent, double days, boolean spoilsFirst) {
        if (piece.has(Things.DRIED.get())) {
            return Component.translatable("rack.backcountry.dried", piece.getHoverName());
        }
        if (Spoilage.spoiled(piece)) {
            return Component.translatable("rack.backcountry.spoiled", piece.getHoverName());
        }
        if (spoilsFirst) {
            return Component.translatable("rack.backcountry.spoils_first", piece.getHoverName(), percent);
        }
        return Component.translatable("rack.backcountry.drying", piece.getHoverName(), percent, Words.one(days));
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return removeItemNoUpdate(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        dried[slot] = 0;
        changed();
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        dried[slot] = 0;
        changed();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return hangs(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        Arrays.fill(dried, 0);
        changed();
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
            Spoilage.age(piece, hours / HOURS_A_DAY * (1 - dried[slot]), air);
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
