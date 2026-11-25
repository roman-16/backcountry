package io.github.roman16.backcountry.guide;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record GuideView(Task.Stage stage, List<Task> tasks, List<Task> done) {
    public static final GuideView NONE = new GuideView(Task.Stage.FIRST_DAYS, List.of(), List.of());
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideView> STREAM_CODEC = StreamCodec.of(GuideView::write, GuideView::read);

    public boolean done(Task task) {
        return done.contains(task);
    }

    private static void write(RegistryFriendlyByteBuf buffer, GuideView view) {
        buffer.writeVarInt(view.stage.ordinal());
        writeTasks(buffer, view.tasks);
        writeTasks(buffer, view.done);
    }

    private static void writeTasks(RegistryFriendlyByteBuf buffer, List<Task> tasks) {
        buffer.writeVarInt(tasks.size());
        tasks.forEach(task -> buffer.writeVarInt(task.ordinal()));
    }

    private static GuideView read(RegistryFriendlyByteBuf buffer) {
        Task.Stage stage = Task.Stage.values()[buffer.readVarInt()];
        return new GuideView(stage, readTasks(buffer), readTasks(buffer));
    }

    private static List<Task> readTasks(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(Task.values()[buffer.readVarInt()]);
        }
        return List.copyOf(tasks);
    }
}
