package io.github.roman16.backcountry.weather;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.haul.Sled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class Ice {
    private Ice() {}

    public static void step(ServerPlayer player, Body body) {
        if (!player.onGround() || player.isCreative()) {
            return;
        }
        BlockPos under = player.blockPosition().below();
        BlockState state = player.level().getBlockState(under);
        if (!(state.getBlock() instanceof ThinIceBlock)) {
            return;
        }
        Balance.Ice ice = Balance.get().ice();
        int thickness = state.getValue(ThinIceBlock.THICKNESS);
        double sled = Sled.pulledBy(player).stream().mapToDouble(pulled -> pulled.contentsKg()).sum();
        boolean towing = !Sled.pulledBy(player).isEmpty();
        boolean breaks = switch (thickness) {
            case 0 -> true;
            case 1 -> body.loadKg >= ice.thinLoadKg() || towing;
            default -> towing && sled >= ice.thickSledKg();
        };
        if (breaks) {
            player.level().setBlockAndUpdate(under, Blocks.WATER.defaultBlockState());
            player.level().playSound(null, under, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, 0.6F);
            player.displayClientMessage(Component.translatable("message.backcountry.ice_broke"), true);
        } else if (player.getRandom().nextDouble() < ice.crackChance()) {
            player.level().playSound(null, under, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.8F, 0.5F);
            player.displayClientMessage(Component.translatable("message.backcountry.ice_cracks"), true);
        }
    }
}
