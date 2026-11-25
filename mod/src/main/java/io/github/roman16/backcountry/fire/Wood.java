package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Activity;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Wood {
    private Wood() {}

    @SubscribeEvent
    static void stripped(BlockEvent.BlockToolModificationEvent event) {
        UseOnContext context = event.getContext();
        BlockState state = event.getState();
        Level level = context.getLevel();
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_STRIP || level.isClientSide || !state.is(BlockTags.BIRCH_LOGS)
                || state.is(Tags.Blocks.STRIPPED_LOGS) || state.is(Tags.Blocks.STRIPPED_WOODS)) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        Block.popResourceFromFace(level, pos, context.getClickedFace(), Things.BIRCH_BARK.toStack());
    }

    @SubscribeEvent
    static void split(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().getItem() instanceof FirewoodItem && !event.getEntity().level().isClientSide) {
            Metabolism.effortMinutes(event.getEntity(), Activity.SPLIT, Balance.get().effort().splitMinutes() * Skill.WOODCUTTING.effort(Metabolism.body(event.getEntity())));
            if (event.getEntity() instanceof ServerPlayer player) {
                Skill.WOODCUTTING.use(player, 1);
            }
        }
    }

    @SubscribeEvent
    static void chopping(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(BlockTags.LOGS)) {
            event.setNewSpeed(event.getNewSpeed() * (float) Skill.WOODCUTTING.speed(Metabolism.body(event.getEntity())));
        }
    }

    @SubscribeEvent
    static void chopped(BlockEvent.BreakEvent event) {
        if (event.getState().is(BlockTags.LOGS) && event.getPlayer() instanceof ServerPlayer player) {
            Skill.WOODCUTTING.use(player, 1);
        }
    }
}
