package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SupplyCrateBlock extends Block {
    public SupplyCrateBlock(Properties properties) {
        super(properties);
    }

    public static boolean carriesMessenger(Player player) {
        if (player.getInventory().contains(new ItemStack(Things.SATELLITE_MESSENGER.get()))) {
            return true;
        }
        return Metabolism.body(player).pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream()
                .anyMatch(stack -> stack.is(Things.SATELLITE_MESSENGER.get()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (carriesMessenger(player)) {
                player.displayClientMessage(Component.translatable("message.backcountry.carries_messenger"), true);
            } else {
                ItemStack messenger = new ItemStack(Things.SATELLITE_MESSENGER.get());
                if (!player.getInventory().add(messenger)) {
                    player.drop(messenger, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
