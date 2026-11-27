package io.github.roman16.backcountry.gear;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;

public final class ItemRules {
    private static final String ANY = "*";
    private final List<Rule> rules;

    private record Rule(boolean anyNamespace, String namespace, Pattern path, int pathLetters, double value) {
        static Rule of(Map.Entry<String, Double> entry) {
            String[] parts = entry.getKey().split(":", 2);
            String path = parts[1];
            return new Rule(parts[0].equals(ANY), parts[0], Pattern.compile(Pattern.quote(path).replace(ANY, "\\E.*\\Q")), path.replace(ANY, "").length(),
                    entry.getValue());
        }

        boolean covers(ResourceLocation id) {
            return (anyNamespace || namespace.equals(id.getNamespace())) && path.matcher(id.getPath()).matches();
        }
    }

    public ItemRules(Map<String, Double> entries) {
        rules = entries.entrySet().stream().map(Rule::of).toList();
    }

    public Optional<Double> find(ResourceLocation id) {
        return rules.stream().filter(rule -> rule.covers(id)).max(Comparator.comparingInt(Rule::pathLetters).thenComparing(rule -> !rule.anyNamespace()))
                .map(Rule::value);
    }
}
