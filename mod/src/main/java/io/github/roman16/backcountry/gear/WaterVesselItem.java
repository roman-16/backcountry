package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class WaterVesselItem extends Item {
    private static final int DRINK_TICKS = 32;
    private final double capacity;

    public WaterVesselItem(Properties properties, double capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public static Water water(ItemStack stack) {
        return stack.getOrDefault(Things.WATER.get(), Water.EMPTY);
    }

    public float capacity() {
        return (float) capacity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos at = hit.getBlockPos();
            if (level.getFluidState(at).is(FluidTags.WATER) && water(stack).litres() < capacity) {
                stack.set(Things.WATER.get(), water(stack).add(false, capacity(), capacity()));
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        if (water(stack).empty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!Metabolism.thirsty(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.backcountry.not_thirsty"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            Water held = water(stack);
            float sip = (float) Math.min(held.litres(), Balance.get().water().sipL());
            Metabolism.drink(player, sip, held.boiled());
            stack.set(Things.WATER.get(), held.take(sip));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Water held = water(stack);
        lines.add((held.empty() ? Component.translatable("tooltip.backcountry.empty")
                : Component.translatable(held.boiled() ? "tooltip.backcountry.boiled_water" : "tooltip.backcountry.lake_water",
                        String.format(Locale.ROOT, "%.2f", held.litres()))).withStyle(ChatFormatting.GRAY));
    }
}
