package io.github.roman16.backcountry.haul;

import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public final class SledItem extends Item {
    public SledItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(at).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            Sled sled = Things.SLED_ENTITY.get().create(level);
            if (sled == null) {
                return InteractionResult.FAIL;
            }
            sled.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, context.getRotation(), 0);
            level.addFreshEntity(sled);
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, at);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
