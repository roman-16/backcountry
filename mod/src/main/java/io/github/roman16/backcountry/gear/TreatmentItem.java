package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.body.Afflictions;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class TreatmentItem extends Item {
    private static final int TREAT_TICKS = 32;
    private final Treatment treatment;

    public enum Treatment {
        BANDAGE,
        MED_KIT,
        SALVE
    }

    public TreatmentItem(Properties properties, Treatment treatment) {
        super(properties);
        this.treatment = treatment;
    }

    private boolean treats(Body body) {
        if (treatment == Treatment.MED_KIT && Afflictions.incubating(body)) {
            return true;
        }
        for (var entry : body.conditions.entrySet()) {
            Condition condition = entry.getKey();
            boolean treated = switch (treatment) {
                case BANDAGE -> condition == Condition.BLISTERS || condition.takesBandage() && !entry.getValue().bandaged;
                case MED_KIT -> condition.curedByMedKit();
                case SALVE -> Afflictions.salves(condition);
            };
            if (treated) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer && !treats(Metabolism.body(player))) {
            player.displayClientMessage(Component.translatable("message.backcountry.nothing_to_treat"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && Afflictions.treat(Metabolism.body(player), treatment)) {
            if (treatment == Treatment.MED_KIT) {
                stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            } else {
                stack.consume(1, player);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return TREAT_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
