package io.github.roman16.backcountry.guide;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GuideView(List<Integer> steps, float storesDays, int dryFirewood, Optional<Task> tracked) {
    public static final GuideView NONE = new GuideView(Arrays.stream(Task.values()).map(task -> 0).toList(), 0, 0, Optional.empty());
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), GuideView::steps, ByteBufCodecs.FLOAT, GuideView::storesDays, ByteBufCodecs.VAR_INT, GuideView::dryFirewood,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT.map(ordinal -> Task.values()[ordinal], Task::ordinal)), GuideView::tracked, GuideView::new);

    public boolean done(Task task, Step step) {
        return (steps.get(task.ordinal()) & 1 << task.steps().indexOf(step)) != 0;
    }

    public boolean done(Task task) {
        return steps.get(task.ordinal()) == task.full();
    }

    public int stepsDone(Task task) {
        return Integer.bitCount(steps.get(task.ordinal()));
    }

    public int done(Chapter chapter) {
        return (int) chapter.tasks().stream().filter(this::done).count();
    }

    public Optional<Step> next(Task task) {
        return task.steps().stream().filter(step -> !done(task, step)).findFirst();
    }

    public Optional<Task> following(int month) {
        return tracked.filter(task -> !done(task)).or(() -> Arrays.stream(Task.values()).filter(task -> !done(task) && task.inSeason(month)
                && task.steps().stream().noneMatch(step -> step.check() instanceof Check.YearsEnd)).findFirst());
    }

    public boolean everyTaskDone() {
        return Arrays.stream(Task.values()).allMatch(task -> done(task) || task.steps().stream().anyMatch(step -> step.check() instanceof Check.YearsEnd));
    }
}
