package io.github.roman16.backcountry.gear;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class PackRows {
    private NonNullList<ItemStack> items = NonNullList.create();
    private ItemStack pack = ItemStack.EMPTY;
    private ItemContainerContents stored = ItemContainerContents.EMPTY;

    public NonNullList<ItemStack> items(ItemStack worn) {
        ItemContainerContents now = worn.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (worn != pack || !now.equals(stored)) {
            items = Hotbar.contents(worn);
            pack = worn;
            stored = now;
        }
        return items;
    }

    public void save(ItemStack worn) {
        Hotbar.store(worn, items(worn));
        stored = worn.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }
}
