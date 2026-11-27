package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;

public class CampPotItem extends WaterVesselItem {
    private static final int PERCENT = 100;

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
                Boiling boiling = stack.get(Things.BOILING.get());
                if (boiling != null) {
                    pot.resume(boiling);
                }
            }
            level.playSound(null, above, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.shrink(1);
            if (context.getPlayer() instanceof ServerPlayer player) {
                Guide.did(player, Deed.HUNG_POT);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        Boiling boiling = stack.get(Things.BOILING.get());
        if (boiling != null && boiling.water().equals(water(stack))) {
            lines.add(Component.translatable("tooltip.backcountry.boiling", Math.round(PERCENT * boiling.ticks() / CampPotEntity.ticksNeeded(boiling.water())))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
