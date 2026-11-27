package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

public final class HuntingBowItem extends BowItem {
    private final boolean heavy;

    public HuntingBowItem(Properties properties, boolean heavy) {
        super(properties);
        this.heavy = heavy;
    }

    public boolean heavy() {
        return heavy;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
        if (heavy) {
            arrow.setBaseDamage(arrow.getBaseDamage() * Balance.get().hunting().heavyPower());
            arrow.setData(Things.HEAVY_ARROW, true);
        }
        return arrow;
    }
}
