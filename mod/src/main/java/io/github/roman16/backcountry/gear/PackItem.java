package io.github.roman16.backcountry.gear;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PackItem extends Item {
    private final int rows;

    public PackItem(Properties properties, int rows) {
        super(properties);
        this.rows = rows;
    }

    public static int slots(ItemStack pack) {
        return pack.getItem() instanceof PackItem item ? item.rows * Hotbar.ROW : 0;
    }
}
