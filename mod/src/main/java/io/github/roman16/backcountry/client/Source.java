package io.github.roman16.backcountry.client;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record Source(Page page, List<List<ItemStack>> inputs, List<ItemStack> outputs, Component note) {
    public enum Page {
        GATHERING,
        DRESSING,
        COOKING_POT,
        SMOKER,
        DRYING_RACK,
        SALTING,
        CAMP_POT,
        BIRCH_TAP,
        WOODPILE,
        TRAPS,
        SHARPENING,
        MAPS,
        LIGHTING;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
