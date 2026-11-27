package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Cuts;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Bulk {
    private static final ItemRules RULES = new ItemRules(Balance.get().bulk());

    private Bulk() {}

    public static Optional<Integer> size(ResourceLocation id) {
        return RULES.find(id).map(Double::intValue);
    }

    private static boolean stacks(Item item) {
        return item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1;
    }

    public static List<ResourceLocation> uncovered() {
        return BuiltInRegistries.ITEM.stream().filter(item -> stacks(item) && !Cuts.cut(item)).map(BuiltInRegistries.ITEM::getKey).filter(id -> size(id).isEmpty())
                .sorted().toList();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void sized(ModifyDefaultComponentsEvent event) {
        event.getAllItems().filter(Bulk::stacks).forEach(item -> size(BuiltInRegistries.ITEM.getKey(item))
                .ifPresent(size -> event.modify(item, patch -> patch.set(DataComponents.MAX_STACK_SIZE, size))));
    }
}
