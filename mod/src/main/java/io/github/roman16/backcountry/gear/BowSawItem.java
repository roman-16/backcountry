package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;

public final class BowSawItem extends AxeItem {
    public BowSawItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return (float) (super.getDestroySpeed(stack, state) * Balance.get().kit().bowSawSpeed());
    }
}
