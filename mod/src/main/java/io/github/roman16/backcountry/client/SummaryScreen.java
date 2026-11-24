package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.net.Messages;
import io.github.roman16.backcountry.survivor.Summary;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

public class SummaryScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final int EDGE = 6;
    private static final int TITLE_ROOM = 36;
    private static final int BUTTONS_ROOM = 2 * 20 + 4;
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 120;
    private static final int PAD = 10;
    private static final int LINE = 12;
    private static final int ITEM = 16;
    private static final int STAT_ROW = 18;
    private static final int CHART_WIDTH = 170;
    private static final int CHART_HEIGHT = 72;
    private static final int AXIS_ROOM = 26;
    private static final int GRID_KG = 5;
    private static final int DASH = 3;
    private static final int SHOWN_ANIMALS = 3;
    private static final int SHOWN_SURVIVORS = 6;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private final Summary summary;
    private boolean arriving;
    private int panelLeft;
    private int panelTop;
    private int titleTop;

    public SummaryScreen(Summary summary) {
        super(title(summary));
        this.summary = summary;
    }

    private static Component title(Summary summary) {
        if (summary.kind() == Summary.Kind.YEAR) {
            return Component.translatable("summary.backcountry.year", Calendar.year(summary.day()), Calendar.dateText(summary.day() + 1));
        }
        return Component.translatable("summary.backcountry." + summary.kind().name().toLowerCase(Locale.ROOT), Calendar.label(summary.day()));
    }

    public boolean arriving() {
        return arriving;
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_WIDTH) / 2;
        titleTop = Math.max(EDGE, (height - TITLE_ROOM - PANEL_HEIGHT - EDGE - BUTTONS_ROOM) / 2);
        panelTop = titleTop + TITLE_ROOM;
        int buttons = panelTop + PANEL_HEIGHT + EDGE;
        if (summary.kind() == Summary.Kind.YEAR) {
            addRenderableWidget(Button.builder(Component.translatable("summary.backcountry.go_on", Calendar.year(summary.day()) + 1), button -> onClose())
                    .bounds((width - BUTTON_WIDTH) / 2, buttons, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("summary.backcountry.arrive"), button -> {
            arriving = true;
            button.active = false;
            ClientState.ended = null;
            Messages.arrive();
        }).bounds((width - BUTTON_WIDTH) / 2, buttons, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("deathScreen.titleScreen"), button -> {
            if (minecraft.level != null) {
                minecraft.level.disconnect();
            }
            minecraft.disconnect();
            minecraft.setScreen(new TitleScreen());
        }).bounds((width - BUTTON_WIDTH) / 2, buttons + BUTTON_HEIGHT + 4, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return summary.kind() == Summary.Kind.YEAR;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean died = summary.kind() == Summary.Kind.DIED;
        graphics.fillGradient(0, 0, width, height, died ? Look.DIED_TOP : Look.ENDED_TOP, died ? Look.DIED_BOTTOM : Look.ENDED_BOTTOM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().scale(2, 2, 2);
        graphics.drawCenteredString(font, title, width / 4, titleTop / 2, Look.TEXT);
        graphics.pose().popPose();
        graphics.drawCenteredString(font, subtitle(), width / 2, titleTop + 2 * LINE, Look.TEXT_QUIET);
        graphics.blitSprite(PANEL, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT);
        int statsLeft;
        if (summary.kind() == Summary.Kind.YEAR) {
            survivors(graphics, panelLeft + PAD, panelTop + PAD);
            statsLeft = panelLeft + PANEL_WIDTH / 2 + PAD;
        } else {
            chart(graphics, panelLeft + PAD + AXIS_ROOM, panelTop + PAD);
            statsLeft = panelLeft + PAD + AXIS_ROOM + CHART_WIDTH + PAD;
        }
        stats(graphics, statsLeft, panelTop + PAD);
    }

    private Component subtitle() {
        if (summary.kind() == Summary.Kind.YEAR) {
            int count = summary.survivors().size();
            return Component.translatable(count == 1 ? "summary.backcountry.year_line_one" : "summary.backcountry.year_line", count);
        }
        return Component.translatable("summary.backcountry.body", Words.one(summary.bmi()), Words.one(summary.weightKg()), Words.one(summary.arrivedKg()));
    }

    private void stats(GuiGraphics graphics, int x, int y) {
        y = stat(graphics, x, y, new ItemStack(Items.CLOCK),
                Component.translatable(summary.days() == 1 ? "summary.backcountry.day_count_one" : "summary.backcountry.day_count", summary.days()));
        y = stat(graphics, x, y, new ItemStack(Items.BREAD), Component.translatable("summary.backcountry.kcal", Words.kcal(summary.kcal())));
        if (summary.animals().isEmpty()) {
            stat(graphics, x, y, new ItemStack(Items.BONE), Component.translatable("summary.backcountry.no_animals"));
            return;
        }
        for (int i = 0; i < Math.min(SHOWN_ANIMALS, summary.animals().size()); i++) {
            Summary.Taken taken = summary.animals().get(i);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(taken.entity()));
            SpawnEggItem egg = SpawnEggItem.byId(type);
            y = stat(graphics, x, y, egg == null ? new ItemStack(Items.BONE) : new ItemStack(egg),
                    Component.literal(taken.count() + " " + type.getDescription().getString().toLowerCase(Locale.ROOT)));
        }
        int more = summary.animals().size() - SHOWN_ANIMALS;
        if (more > 0) {
            graphics.drawString(font, Component.translatable("summary.backcountry.more_animals", more), x + ITEM + 4, y + 4, Look.TEXT_QUIET, false);
        }
    }

    private int stat(GuiGraphics graphics, int x, int y, ItemStack icon, Component text) {
        graphics.renderItem(icon, x, y);
        graphics.drawString(font, text, x + ITEM + 4, y + 4, Look.TEXT, false);
        return y + STAT_ROW;
    }

    private void survivors(GuiGraphics graphics, int x, int y) {
        List<Summary.Line> lines = summary.survivors();
        for (int i = 0; i < Math.min(SHOWN_SURVIVORS, lines.size()); i++) {
            Summary.Line line = lines.get(i);
            graphics.drawString(font, line.name(), x, y, Look.TEXT, false);
            Component note = note(line);
            if (note != null) {
                graphics.drawString(font, note, x + font.width(line.name()) + 4, y, line.end() == Summary.Kind.LIVING ? Look.TEXT_QUIET : Look.DANGER, false);
            }
            graphics.drawString(font, Component.translatable("summary.backcountry.weights", Words.one(line.fromKg()), Words.one(line.toKg())), x, y + LINE - 2,
                    Look.TEXT_QUIET, false);
            y += 2 * LINE;
        }
    }

    private Component note(Summary.Line line) {
        if (line.end() != Summary.Kind.LIVING) {
            return Component.translatable("summary.backcountry.ended." + line.end().name().toLowerCase(Locale.ROOT), Calendar.dayOfYear(line.endedDay()));
        }
        if (Calendar.year(line.arrivedDay()) == Calendar.year(summary.day()) && Calendar.dayOfYear(line.arrivedDay()) > 1) {
            return Component.translatable("summary.backcountry.arrived", Calendar.dayOfYear(line.arrivedDay()));
        }
        return null;
    }

    private void chart(GuiGraphics graphics, int left, int top) {
        List<Float> weights = new ArrayList<>(summary.weights());
        if (weights.size() == 1) {
            weights.add(weights.get(0));
        }
        double height = Balance.get().body().heightM();
        float extraction = (float) (Balance.get().body().extractionBmi() * height * height);
        float high = (float) Math.ceil(Math.max(summary.arrivedKg(), weights.stream().reduce(Float::max).orElse(0F)) / GRID_KG) * GRID_KG;
        float low = (float) Math.floor(Math.min(extraction, weights.stream().reduce(Float::min).orElse(0F)) / GRID_KG) * GRID_KG;
        graphics.fill(left, top, left + CHART_WIDTH, top + CHART_HEIGHT, Look.CHART);
        for (float kg = low; kg <= high; kg += GRID_KG) {
            int y = top + Math.round((high - kg) / (high - low) * (CHART_HEIGHT - 1));
            graphics.fill(left, y, left + CHART_WIDTH, y + 1, Look.GRID);
            String label = Long.toString(Math.round(kg));
            graphics.drawString(font, label, left - font.width(label) - 4, y - 4, Look.TEXT_QUIET, false);
        }
        int line = top + Math.round((high - extraction) / (high - low) * (CHART_HEIGHT - 1));
        for (int x = left; x < left + CHART_WIDTH; x += 2 * DASH) {
            graphics.fill(x, line, Math.min(x + DASH, left + CHART_WIDTH), line + 1, Look.DANGER);
        }
        Component bmi = Component.translatable("summary.backcountry.extraction_line", Words.one(Balance.get().body().extractionBmi()));
        graphics.drawString(font, bmi, left + 2, line - LINE + 2, Look.DANGER, false);
        int previous = -1;
        for (int x = 0; x < CHART_WIDTH; x++) {
            float position = x * (weights.size() - 1) / (float) (CHART_WIDTH - 1);
            int from = Math.min(weights.size() - 2, (int) position);
            float weight = weights.get(from) + (weights.get(from + 1) - weights.get(from)) * (position - from);
            int y = top + Math.round((high - weight) / (high - low) * (CHART_HEIGHT - 1));
            int upper = previous < 0 ? y : Math.min(previous, y);
            int lower = previous < 0 ? y : Math.max(previous, y);
            graphics.fill(left + x, upper, left + x + 1, lower + 2, Look.CURVE);
            previous = y;
        }
        Component first = Component.translatable("summary.backcountry.curve_day", Calendar.dayOfYear(summary.day() - summary.days() + 1));
        Component last = Component.translatable("summary.backcountry.curve_day", Calendar.dayOfYear(summary.day()));
        graphics.drawString(font, first, left, top + CHART_HEIGHT + 4, Look.TEXT_QUIET, false);
        graphics.drawString(font, last, left + CHART_WIDTH - font.width(last), top + CHART_HEIGHT + 4, Look.TEXT_QUIET, false);
    }
}
