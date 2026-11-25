package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.client.Look;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.guide.Task;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FirewoodItem extends Item {
    private static final int PERCENT = 100;
    private static final int BAR_WIDTH = 13;
    private final boolean hardwood;

    public FirewoodItem(Properties properties, boolean hardwood) {
        super(properties.component(Things.SEASONING.get(), percent(hardwood ? Balance.get().firewood().freshHardwood() : Balance.get().firewood().freshSoftwood())));
        this.hardwood = hardwood;
    }

    public static int percent(double share) {
        return (int) Math.floor(share * PERCENT + 1e-9);
    }

    public static double seasoning(ItemStack stack) {
        return stack.getOrDefault(Things.SEASONING.get(), 0) / (double) PERCENT;
    }

    public static ItemStack seasoned(ItemStack stack, double share) {
        stack.set(Things.SEASONING.get(), percent(Math.min(1, share)));
        return stack;
    }

    public static boolean catches(ItemStack stack) {
        return stack.getItem() instanceof FirewoodItem && seasoning(stack) >= Balance.get().firewood().catches();
    }

    public double hours() {
        return hardwood ? Balance.get().fire().hardwoodHours() : Balance.get().fire().softwoodHours();
    }

    public double heat(ItemStack stack) {
        return (hardwood ? Balance.get().fire().hardwoodHeat() : 1) * seasoning(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext place = new BlockPlaceContext(context);
        Level level = context.getLevel();
        BlockPos at = place.getClickedPos();
        BlockState pile = Things.WOODPILE.get().getStateForPlacement(place);
        if (!place.canPlace() || pile == null || !pile.canSurvive(level, at)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && level.setBlockAndUpdate(at, pile) && level.getBlockEntity(at) instanceof WoodpileEntity woodpile) {
            woodpile.add(context.getItemInHand().split(1));
            level.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 0.8F);
            woodpile.tell(context.getPlayer());
            if (context.getPlayer() instanceof ServerPlayer player) {
                Guide.done(player, Task.LAY_WOODPILE);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(BAR_WIDTH * (float) seasoning(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return catches(stack) ? Look.SEASONED : Look.GREEN_WOOD;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.backcountry.seasoned", percent(seasoning(stack))).withStyle(ChatFormatting.GRAY));
        if (!catches(stack)) {
            tooltip.add(Component.translatable("tooltip.backcountry.too_green").withStyle(ChatFormatting.RED));
        }
    }
}
