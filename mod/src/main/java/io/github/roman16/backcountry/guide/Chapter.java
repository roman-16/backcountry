package io.github.roman16.backcountry.guide;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum Chapter {
    FIRST_DAY,
    CAMP,
    FOOD,
    HUNTING,
    BEFORE_SNOW,
    WINTER,
    YEAR;

    public List<Task> tasks() {
        return Arrays.stream(Task.values()).filter(task -> task.chapter() == this).toList();
    }

    public boolean team() {
        return tasks().stream().allMatch(Task::team);
    }

    public Component title() {
        return Component.translatable("task.backcountry.chapter." + name().toLowerCase(Locale.ROOT));
    }
}
