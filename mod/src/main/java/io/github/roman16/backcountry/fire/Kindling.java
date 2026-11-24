package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.Hotbar;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Kindling {
    private Kindling() {}

    public static boolean carried(ServerPlayer player) {
        return count(player, Kindling::tinder) >= 1 && count(player, Kindling::stick) >= Balance.get().fire().kindlingSticks();
    }

    public static void use(ServerPlayer player) {
        take(player, Kindling::tinder, 1);
        take(player, Kindling::stick, Balance.get().fire().kindlingSticks());
    }

    public static boolean wet(ServerPlayer player, BlockPos fire) {
        return Fire.rainedOn(player.level(), fire) || Metabolism.body(player).wetness > Balance.get().conditions().dryWetness();
    }

    private static boolean tinder(ItemStack stack) {
        return stack.is(Things.TINDER_BUNDLE.get());
    }

    private static boolean stick(ItemStack stack) {
        return stack.is(Items.STICK);
    }

    private static int count(ServerPlayer player, Predicate<ItemStack> wanted) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Hotbar.ROW; slot++) {
            count += wanted.test(inventory.getItem(slot)) ? inventory.getItem(slot).getCount() : 0;
        }
        count += wanted.test(player.getOffhandItem()) ? player.getOffhandItem().getCount() : 0;
        for (ItemStack inside : Hotbar.contents(Metabolism.body(player).pack)) {
            count += wanted.test(inside) ? inside.getCount() : 0;
        }
        return count;
    }

    private static void take(ServerPlayer player, Predicate<ItemStack> wanted, int amount) {
        int left = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Hotbar.ROW && left > 0; slot++) {
            left -= shrink(inventory.getItem(slot), wanted, left);
        }
        left -= shrink(player.getOffhandItem(), wanted, left);
        Body body = Metabolism.body(player);
        if (left > 0 && !body.pack.isEmpty()) {
            NonNullList<ItemStack> items = Hotbar.contents(body.pack);
            for (ItemStack inside : items) {
                left -= shrink(inside, wanted, left);
            }
            Hotbar.store(body.pack, items);
        }
    }

    private static int shrink(ItemStack stack, Predicate<ItemStack> wanted, int left) {
        if (left <= 0 || !wanted.test(stack)) {
            return 0;
        }
        int taken = Math.min(left, stack.getCount());
        stack.shrink(taken);
        return taken;
    }
}
