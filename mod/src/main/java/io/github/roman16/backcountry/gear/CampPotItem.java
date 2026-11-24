package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;

public class CampPotItem extends WaterVesselItem {
    public CampPotItem(Properties properties) {
        super(properties, Balance.get().water().potL());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos fire = context.getClickedPos();
        BlockPos above = fire.above();
        if (context.getClickedFace() != Direction.UP || !(level.getBlockState(fire).getBlock() instanceof CampfireBlock) || !level.getBlockState(above).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            level.setBlockAndUpdate(above, Things.CAMP_POT_BLOCK.get().defaultBlockState());
            if (level.getBlockEntity(above) instanceof CampPotEntity pot) {
                pot.hold(water(stack));
            }
            level.playSound(null, above, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
