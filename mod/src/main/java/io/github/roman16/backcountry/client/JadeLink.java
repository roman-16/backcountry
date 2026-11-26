package io.github.roman16.backcountry.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ui.IElement;

@WailaPlugin
public final class JadeLink implements IWailaPlugin {
    private static final int LAST = Integer.MAX_VALUE;

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addBeforeRenderCallback((box, rect, graphics, accessor) -> Minecraft.getInstance().player == null
                || !Minecraft.getInstance().player.isCrouching());
        registration.addTooltipCollectedCallback(LAST, (box, accessor) -> {
            List<IElement> name = List.copyOf(box.getTooltip().get(JadeIds.CORE_OBJECT_NAME));
            box.getTooltip().clear();
            box.getTooltip().add(name);
            box.setIcon(null);
        });
    }
}
