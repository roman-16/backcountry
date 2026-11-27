package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.Hotbar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Kindling {
    private Kindling() {}

    public static boolean carried(ServerPlayer player) {
        return Hotbar.count(player, Kindling::tinder) >= 1 && Hotbar.count(player, Kindling::stick) >= Balance.get().fire().kindlingSticks();
    }

    public static void use(ServerPlayer player, BlockPos fire) {
        boolean pitch = Hotbar.count(player, Kindling::pitch) > 0 && (damp(player, fire) || Hotbar.count(player, Kindling::bundle) == 0);
        Hotbar.take(player, pitch ? Kindling::pitch : Kindling::bundle, 1);
        Hotbar.take(player, Kindling::stick, Balance.get().fire().kindlingSticks());
    }

    public static boolean wet(ServerPlayer player, BlockPos fire) {
        return Hotbar.count(player, Kindling::pitch) == 0 && damp(player, fire);
    }

    private static boolean damp(ServerPlayer player, BlockPos fire) {
        return Fire.rainedOn(player.level(), fire) || Metabolism.body(player).wetness > Balance.get().conditions().dryWetness();
    }

    private static boolean tinder(ItemStack stack) {
        return bundle(stack) || pitch(stack);
    }

    private static boolean bundle(ItemStack stack) {
        return stack.is(Things.TINDER_BUNDLE.get());
    }

    private static boolean pitch(ItemStack stack) {
        return stack.is(Things.PITCH_TINDER.get());
    }

    private static boolean stick(ItemStack stack) {
        return stack.is(Items.STICK);
    }
}
