package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Activity;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Lighting {
    private static final double REACH_SQUARED = 64;
    private static final int MOST_STROKES = 4096;

    private Lighting() {}

    public static void drilled(ServerPlayer player, BlockPos pos, int strokes, boolean lit) {
        if (Metabolism.lives(player)) {
            int done = Math.min(strokes, MOST_STROKES);
            Metabolism.effortMinutes(player, Activity.BOW_DRILL, done * Balance.get().effort().strokeMinutes() * Skill.FIRE_MAKING.effort(Metabolism.body(player)));
            Skill.FIRE_MAKING.use(player, done);
        }
        Level level = player.level();
        if (!lit || player.distanceToSqr(pos.getCenter()) > REACH_SQUARED || !level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FireBlock) || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire)) {
            return;
        }
        if (!FireBlock.cold(state)) {
            player.displayClientMessage(Component.translatable("message.backcountry.already_burning"), true);
            return;
        }
        InteractionHand hand = player.getMainHandItem().is(Things.BOW_DRILL.get()) ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().is(Things.BOW_DRILL.get()) ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            return;
        }
        if (!Kindling.carried(player)) {
            player.displayClientMessage(Component.translatable("message.backcountry.needs_kindling", Balance.get().fire().kindlingSticks()), true);
            return;
        }
        Kindling.use(player);
        ItemStack drill = player.getItemInHand(hand);
        drill.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
        FireBlock.light(level, pos, state, campfire);
        Guide.did(player, Deed.LIT_FIRE);
    }
}
