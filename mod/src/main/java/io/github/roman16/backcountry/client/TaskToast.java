package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.guide.Task;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class TaskToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final Component DONE = Component.translatable("toast.backcountry.task_done");
    private static final long SHOWN_MS = 5000;
    private static final int TEXT_LEFT = 30;
    private static final int ICON = 8;
    private final Task task;

    TaskToast(Task task) {
        this.task = task;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long shownMs) {
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        graphics.drawString(toasts.getMinecraft().font, DONE, TEXT_LEFT, 7, Look.TOAST_TITLE, false);
        String title = toasts.getMinecraft().font.plainSubstrByWidth(task.title().getString(), width() - TEXT_LEFT - ICON);
        graphics.drawString(toasts.getMinecraft().font, title, TEXT_LEFT, 18, Look.TEXT, false);
        graphics.renderFakeItem(task.icon(), ICON, ICON);
        return shownMs >= SHOWN_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
