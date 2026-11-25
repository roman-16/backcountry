package io.github.roman16.backcountry;

import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Felling {
    private Felling() {}

    @SubscribeEvent
    static void harvest(PlayerEvent.HarvestCheck event) {
        if (event.getTargetBlock().is(BlockTags.LOGS) && !event.getEntity().getMainHandItem().canPerformAction(ItemAbilities.AXE_DIG)) {
            event.setCanHarvest(false);
        }
    }
}
