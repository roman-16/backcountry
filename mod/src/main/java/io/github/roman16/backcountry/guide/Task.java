package io.github.roman16.backcountry.guide;

import io.github.roman16.backcountry.Balance;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum Task {
    FILL_BOTTLE(Stage.FIRST_DAYS),
    LIGHT_FIRE(Stage.FIRST_DAYS),
    BOIL_WATER(Stage.FIRST_DAYS),
    SLEEP_ROOFED(Stage.FIRST_DAYS),
    SPLIT_LOG(Stage.FIRST_DAYS),
    LAY_WOODPILE(Stage.FIRST_DAYS),
    BUILD_CACHE(Stage.FIRST_DAYS),
    LAND_FISH(Stage.FIRST_DAYS),
    TRAP_CATCH(Stage.FIRST_DAYS),
    FOOD_STORES(Stage.BEFORE_SNOW),
    DRY_FIREWOOD(Stage.BEFORE_SNOW),
    HIDE_CLOTHING(Stage.BEFORE_SNOW),
    REACH_APRIL(Stage.WINTER),
    REACH_MAY(Stage.YEAR);

    public enum Stage {
        FIRST_DAYS,
        BEFORE_SNOW,
        WINTER,
        YEAR;

        public boolean team() {
            return this != FIRST_DAYS;
        }

        public List<Task> tasks() {
            return Arrays.stream(Task.values()).filter(task -> task.stage == this).toList();
        }

        public Component title() {
            return Component.translatable("task.backcountry.stage." + name().toLowerCase(Locale.ROOT));
        }
    }

    private final Stage stage;

    Task(Stage stage) {
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        Balance.Tasks rules = Balance.get().tasks();
        return switch (this) {
            case FOOD_STORES -> Component.translatable("task.backcountry." + id(), (int) rules.foodDays());
            case DRY_FIREWOOD -> Component.translatable("task.backcountry." + id(), (int) rules.dryFirewood());
            default -> Component.translatable("task.backcountry." + id());
        };
    }

    public Component how() {
        return Component.translatable("task.backcountry." + id() + ".how");
    }
}
