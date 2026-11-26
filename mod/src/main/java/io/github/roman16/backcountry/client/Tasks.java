package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.guide.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

final class Tasks {
    private static final float CLICK_PITCH = 1.6F;
    private static final float CLICK_VOLUME = 0.25F;

    private Tasks() {}

    static void compare(GuideView before, GuideView after) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean ticked = false;
        for (Task task : Task.values()) {
            int gained = after.steps().get(task.ordinal()) & ~before.steps().get(task.ordinal());
            if (gained == 0) {
                continue;
            }
            if (after.done(task)) {
                minecraft.getToasts().addToast(new TaskToast(task));
            } else {
                ticked = true;
            }
        }
        if (ticked) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), CLICK_PITCH, CLICK_VOLUME));
        }
    }
}
