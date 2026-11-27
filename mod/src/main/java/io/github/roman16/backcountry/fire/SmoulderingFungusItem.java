package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SmoulderingFungusItem extends Item {
    private static final double TICKS_AN_HOUR = 1000;

    public SmoulderingFungusItem(Properties properties) {
        super(properties);
    }

    public static ItemStack lit(Level level) {
        ItemStack stack = new ItemStack(Things.SMOULDERING_FUNGUS.get());
        stack.set(Things.EMBER_UNTIL.get(), level.getGameTime() + Math.round(Balance.get().fire().fungusHours() * TICKS_AN_HOUR));
        return stack;
    }

    public static boolean out(ItemStack stack, Level level) {
        return level.getGameTime() >= stack.getOrDefault(Things.EMBER_UNTIL.get(), 0L);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && out(stack, level)) {
            stack.setCount(0);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.backcountry.fungus_out"), true);
            }
        }
    }
}
