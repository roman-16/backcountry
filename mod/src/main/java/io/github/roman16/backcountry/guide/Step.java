package io.github.roman16.backcountry.guide;

import java.util.List;

public record Step(String id, Check check, List<Need> needs) {
    public record Need(Thing thing, int count) {}

    public static Step of(String id, Check check, Need... needs) {
        return new Step(id, check, List.of(needs));
    }

    public static Need need(Thing thing, int count) {
        return new Need(thing, count);
    }

    public List<Need> shown() {
        if (!needs.isEmpty()) {
            return needs;
        }
        return check instanceof Check.Held held ? List.of(new Need(held.thing(), held.count())) : List.of();
    }
}
