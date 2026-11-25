package io.github.roman16.backcountry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Portals {
    private Portals() {}

    @SubscribeEvent
    static void lit(BlockEvent.PortalSpawnEvent event) {
        event.setCanceled(true);
    }
}
