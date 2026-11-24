package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Things;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LeftPackEntity extends BaseContainerBlockEntity {
    private static final int SLOTS = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private Component title = Component.translatable("block.backcountry.left_pack");

    public LeftPackEntity(BlockPos pos, BlockState state) {
        super(Things.LEFT_PACK_ENTITY.get(), pos, state);
    }

    public void fill(List<ItemStack> carried, Component owner) {
        title = owner;
        for (int i = 0; i < carried.size(); i++) {
            if (i < SLOTS) {
                items.set(i, carried.get(i));
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), carried.get(i));
            }
        }
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return title;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putString("title", Component.Serializer.toJson(title, provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        if (tag.contains("title")) {
            Component parsed = Component.Serializer.fromJson(tag.getString("title"), provider);
            if (parsed != null) {
                title = parsed;
            }
        }
    }
}
