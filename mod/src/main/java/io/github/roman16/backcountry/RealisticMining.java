package io.github.roman16.backcountry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class RealisticMining {
    private RealisticMining() {}

    @SubscribeEvent
    static void breakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES)) {
            event.setNewSpeed((float) (event.getNewSpeed() * Balance.get().mining().speed()));
        }
    }
}
