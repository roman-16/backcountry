package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Things;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class MessengerLight implements IItemDecorator {
    private static final int ABOVE_COUNT = 200;
    private static final int LEFT = 11;
    private static final int SIZE = 4;

    @SubscribeEvent
    static void register(RegisterItemDecorationsEvent event) {
        event.register(Things.SATELLITE_MESSENGER.get(), new MessengerLight());
    }

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (!ClientState.unread()) {
            return false;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, ABOVE_COUNT);
        graphics.fill(x + LEFT, y, x + LEFT + SIZE, y + SIZE, Look.LIGHT_EDGE);
        graphics.fill(x + LEFT + 1, y + 1, x + LEFT + SIZE - 1, y + SIZE - 1, Look.LIGHT);
        graphics.pose().popPose();
        return true;
    }
}
