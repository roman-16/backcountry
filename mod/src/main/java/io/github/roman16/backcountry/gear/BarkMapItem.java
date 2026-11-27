package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Things;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class BarkMapItem extends Item {
    public BarkMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server) {
                MapSheets.show(server, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        Optional<Component> refused = MapSheets.cannotSurvey(player);
        if (refused.isPresent()) {
            player.displayClientMessage(refused.get(), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            MapSheets.survey(player, stack);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MapSheets.surveyTicks();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return stack.has(Things.MAP_SHEET.get()) ? Component.translatable("item.backcountry.bark_map.drawn") : super.getName(stack);
    }
}
