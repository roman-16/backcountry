package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Cuts;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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
    private static final String ANY = "*";
    private static final List<Rule> RULES = Balance.get().bulk().entrySet().stream().map(Rule::of).toList();

    private Bulk() {}

    private record Rule(boolean anyNamespace, String namespace, Pattern path, int pathLetters, int size) {
        static Rule of(Map.Entry<String, Double> entry) {
            String[] parts = entry.getKey().split(":", 2);
            String path = parts[1];
            return new Rule(parts[0].equals(ANY), parts[0], Pattern.compile(Pattern.quote(path).replace(ANY, "\\E.*\\Q")), path.replace(ANY, "").length(),
                    entry.getValue().intValue());
        }

        boolean covers(ResourceLocation id) {
            return (anyNamespace || namespace.equals(id.getNamespace())) && path.matcher(id.getPath()).matches();
        }
    }

    public static Optional<Integer> size(ResourceLocation id) {
        return RULES.stream().filter(rule -> rule.covers(id))
                .max(Comparator.comparingInt(Rule::pathLetters).thenComparing(rule -> !rule.anyNamespace())).map(Rule::size);
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
