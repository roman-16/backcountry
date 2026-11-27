package io.github.roman16.backcountry.trail;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Trail {
    private static final ResourceLocation ROCKS = ResourceLocation.fromNamespaceAndPath("projectvibrantjourneys", "rocks");

    private Trail() {}

    @SubscribeEvent
    static void used(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState clicked = level.getBlockState(pos);
        Direction face = event.getFace();
        if (stack.getItem() instanceof AxeItem && player.isShiftKeyDown() && face != null && face.getAxis().isHorizontal() && clicked.is(BlockTags.LOGS)
                && !clicked.is(Tags.Blocks.STRIPPED_LOGS)) {
            BlockPos at = pos.relative(face);
            if (level.getBlockState(at).canBeReplaced()) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(at, Things.BLAZE.get().defaultBlockState().setValue(BlazeBlock.FACING, face));
                    level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1, 1.2F);
                    stack.hurtAndBreak(1, player, event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            }
            return;
        }
        if (!stack.is(Things.ROCKS)) {
            return;
        }
        boolean onRocks = ROCKS.equals(BuiltInRegistries.BLOCK.getKey(clicked.getBlock()));
        boolean onCairn = clicked.is(Things.CAIRN.get());
        if (!onRocks && !onCairn) {
            return;
        }
        BlockPos at = onRocks ? pos : top(level, pos).above();
        if (onCairn && (height(level, at.below()) >= Balance.get().trail().cairnHeight() || !level.getBlockState(at).canBeReplaced())) {
            return;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(at, Things.CAIRN.get().defaultBlockState());
            level.playSound(null, at, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1, 0.8F);
            stack.consume(1, player);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    private static BlockPos top(Level level, BlockPos pos) {
        BlockPos at = pos;
        while (level.getBlockState(at.above()).is(Things.CAIRN.get())) {
            at = at.above();
        }
        return at;
    }

    private static int height(Level level, BlockPos top) {
        int height = 0;
        for (BlockPos at = top; level.getBlockState(at).is(Things.CAIRN.get()); at = at.below()) {
            height++;
        }
        return height;
    }
}
