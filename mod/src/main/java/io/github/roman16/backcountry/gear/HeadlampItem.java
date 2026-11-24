package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.client.Words;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class HeadlampItem extends ArmorItem {
    private static final int TICKS_A_SECOND = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final int MINUTES_AN_HOUR = 60;
    private static final int WINDING = 72000;

    public HeadlampItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties.stacksTo(1).component(Things.CHARGE.get(), (float) Balance.get().headlamp().chargeHours()).component(Things.LIGHT.get(), 0));
    }

    public static double charge(ItemStack stack) {
        return stack.getOrDefault(Things.CHARGE.get(), 0F);
    }

    public static int light(double charge, double airC) {
        Balance.Headlamp lamp = Balance.get().headlamp();
        if (charge <= 0) {
            return 0;
        }
        double cold = Math.max(0, Math.min(1, (lamp.dimFromC() - airC) / (lamp.dimFromC() - lamp.dimmestC())));
        double share = 1 - cold * (1 - lamp.dimmestShare());
        return Math.max(1, (int) Math.round(lamp.light() * share));
    }

    public static void shine(ServerPlayer player, ItemStack stack, long ticks) {
        double charge = Math.max(0, charge(stack) - ticks / TICKS_AN_HOUR);
        stack.set(Things.CHARGE.get(), (float) charge);
        BlockPos head = BlockPos.containing(player.getEyePosition());
        int light = light(charge, Climate.airC(player.level(), head, Calendar.day(player.level())));
        if (stack.getOrDefault(Things.LIGHT.get(), 0) != light) {
            stack.set(Things.LIGHT.get(), light);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || entity.tickCount % TICKS_A_SECOND != 0) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD) == stack && Metabolism.lives(player)) {
            shine(player, stack, TICKS_A_SECOND);
        } else if (stack.getOrDefault(Things.LIGHT.get(), 0) != 0) {
            stack.set(Things.LIGHT.get(), 0);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (charge(stack) >= Balance.get().headlamp().chargeHours()) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return WINDING;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || (WINDING - remaining) % TICKS_A_SECOND != 0 || !(entity instanceof Player player)) {
            return;
        }
        Balance.Headlamp lamp = Balance.get().headlamp();
        double wound = Math.min(lamp.chargeHours(), charge(stack) + lamp.crankHoursPerSecond());
        stack.set(Things.CHARGE.get(), (float) wound);
        Metabolism.work(player, lamp.crankKcalPerSecond());
        level.playSound(null, player, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        if (wound >= lamp.chargeHours()) {
            player.stopUsingItem();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.backcountry.headlamp_charge", Words.duration((int) Math.round(charge(stack) * MINUTES_AN_HOUR))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.backcountry.headlamp_wind").withStyle(ChatFormatting.GRAY));
    }
}
