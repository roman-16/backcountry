package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.guide.Step;
import io.github.roman16.backcountry.guide.Task;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

final class TaskTracker {
    private static final int EDGE = 4;
    private static final int PAD = 3;
    private static final int LINE = 10;
    private static final int ITEM = 16;
    private static final int ITEM_ROW = 17;
    private static final int NEED_GAP = 6;
    private static final int MOST_WIDTH = 192;
    private static final int INDENT = 8;

    private TaskTracker() {}

    static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Hud.shows() || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        GuideView guide = ClientState.guide;
        Optional<Task> following = guide.following(Calendar.date(Math.max(1, ClientState.view.day())).getMonthValue());
        if (following.isEmpty()) {
            return;
        }
        Task task = following.get();
        Optional<Step> next = guide.next(task);
        if (next.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        Step step = next.get();
        Component heading = Component.translatable("hud.backcountry.task", task.title(), Component.translatable("messenger.backcountry.tasks.count", guide.stepsDone(task),
                task.steps().size()));
        List<FormattedCharSequence> stepLines = font.split(Component.literal("▸ ").append(task.step(step)), MOST_WIDTH - INDENT);
        Optional<Component> progress = MessengerScreen.progress(task, guide);
        List<Step.Need> needs = progress.isPresent() ? List.of() : step.shown();
        List<ItemStack> carried = Hotbar.carried(minecraft.player);
        int needsWidth = needs.stream().mapToInt(need -> ITEM + 2 + font.width(count(need, carried)) + NEED_GAP).sum() - NEED_GAP;
        int width = Math.min(MOST_WIDTH, Math.max(font.width(heading), Math.max(stepLines.stream().mapToInt(font::width).max().orElse(0),
                INDENT + Math.max(needsWidth, progress.map(font::width).orElse(0)))));
        int height = LINE + stepLines.size() * LINE + (progress.isPresent() ? LINE : 0) + (needs.isEmpty() ? 0 : ITEM_ROW);
        graphics.fill(EDGE - PAD, EDGE - PAD, EDGE + width + PAD, EDGE + height + PAD - 1, Look.PLATE);
        int y = EDGE;
        graphics.drawString(font, font.plainSubstrByWidth(heading.getString(), MOST_WIDTH), EDGE, y, Look.TEXT);
        y += LINE;
        for (FormattedCharSequence line : stepLines) {
            graphics.drawString(font, line, EDGE + INDENT / 2, y, Look.TRACKED);
            y += LINE;
        }
        if (progress.isPresent()) {
            graphics.drawString(font, progress.get(), EDGE + INDENT, y, Look.TEXT_QUIET);
            return;
        }
        int x = EDGE + INDENT;
        for (Step.Need need : needs) {
            graphics.renderItem(need.thing().stack(), x, y);
            String count = count(need, carried);
            int have = have(need, carried);
            graphics.drawString(font, count, x + ITEM + 2, y + 4, have >= need.count() ? Look.GOOD : Look.TEXT_QUIET);
            x += ITEM + 2 + font.width(count) + NEED_GAP;
        }
    }

    private static int have(Step.Need need, List<ItemStack> carried) {
        return carried.stream().filter(need.thing()::matches).mapToInt(ItemStack::getCount).sum();
    }

    private static String count(Step.Need need, List<ItemStack> carried) {
        return Math.min(have(need, carried), need.count()) + "/" + need.count();
    }
}
