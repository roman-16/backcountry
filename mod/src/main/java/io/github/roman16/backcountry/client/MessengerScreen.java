package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Burn;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.food.Ration;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.guide.Chapter;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.guide.Step;
import io.github.roman16.backcountry.guide.Task;
import io.github.roman16.backcountry.net.Messages;
import io.github.roman16.backcountry.weather.Kind;
import io.github.roman16.backcountry.weather.Outlook;
import io.github.roman16.backcountry.weather.WeatherView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

public class MessengerScreen extends Screen {
    private static final ResourceLocation TEMPERATURE = Backcountry.id("hud/temperature");
    private static final ResourceLocation WET = Backcountry.id("hud/wet");
    private static final ResourceLocation FIRE = Backcountry.id("hud/burns");
    private static final ResourceLocation SHELTER = Backcountry.id("hud/shelter");
    private static final ResourceLocation LOAD = Backcountry.id("hud/load");
    private static final ResourceLocation ENERGY = Backcountry.id("hud/energy_full");
    private static final ResourceLocation AWAKE = Backcountry.id("hud/exhausted");
    private static final ResourceLocation WIND = Backcountry.id("hud/wind");
    private static final ResourceLocation FORECAST = Backcountry.id("hud/forecast");
    private static final ResourceLocation ALERT = Backcountry.id("hud/alert");
    private static final ResourceLocation CLOTHING = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation FOOD = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation RATION = Backcountry.id("hud/ration");
    private static final ResourceLocation TASKS = Backcountry.id("hud/tasks");
    private static final ResourceLocation GUIDE = Backcountry.id("hud/guide");
    private static final String JEI = "jei";
    private static final int MOST_WIDTH = 640;
    private static final int MOST_HEIGHT = 360;
    private static final int CASE_TOP = 18;
    private static final int CASE_SIDE = 8;
    private static final int PAD = 6;
    private static final int ICON = 9;
    private static final int ICON_GAP = 3;
    private static final int LABEL = ICON + 4;
    private static final int METER = 3;
    private static final int SHORT_LINE = 11;
    private static final int TALL_LINE = 13;
    private static final int SHORT_METER_ROW = 16;
    private static final int TALL_METER_ROW = 20;
    private static final int TALL = 320;
    private static final int GAP = 4;
    private static final int TAB_PAD = 4;
    private static final int TAB_GAP = 3;
    private static final int TABS_Y = 15;
    private static final int CONTENT_Y = 32;
    private static final int ALERT_GAP = 8;
    private static final int ALERT_LINES = 2;
    private static final int BOX = 7;
    private static final int ITEM = 16;
    private static final int ITEM_ROW = 18;
    private static final int NEED_GAP = 8;
    private static final int HOW_LINES = 3;
    private static final int WHY_LINES = 2;
    private static final int WIDE = 400;
    private static final int LIST_LEAST = 112;
    private static final int LIST_MOST = 176;
    private static final float LIST_SHARE = 0.36F;
    private static final float CORE_LOW = 33;
    private static final float CORE_HIGH = 39;
    private static final float AIR_LOW = -40;
    private static final float AIR_HIGH = 30;
    private static final float FREEZING = 0;
    private static final float WIND_FULL = 64;
    private static final float BMI_LOW = 15;
    private static final float BMI_HIGH = 25;
    private static final float CLOTHING_FULL = 6;
    private static final float AWAKE_FULL_HOURS = 48;
    private static final float LOW_SHARE = 0.25F;
    private static final float TIRED_SHARE = 0.75F;
    private static final float STORES_LOW_DAYS = 2;
    private static final int BMI_WARNING = 1;
    private static final int BURNS_A_LINE = 3;
    private static final float METRES_A_KM = 1000;
    private static final int SKILL_STEP = 4;
    private static final int SOS_TOP = 3;
    private static final int SOS_HEIGHT = 12;
    private static final List<String> SOS_LINES = List.of("boat", "pack", "arrival");
    private static Page page = Page.TASKS;
    private static Optional<Task> chosen = Optional.empty();
    private static Optional<Chapter> opened = Optional.empty();
    private static Topic topic = Topic.RATIONS;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Shown> items = new ArrayList<>();
    private int left;
    private int top;
    private int deviceWidth;
    private int deviceHeight;
    private int line;
    private int meterRow;
    private boolean asking;

    private enum Page {
        TASKS(MessengerScreen.TASKS),
        WEATHER(Kind.PARTLY_CLOUDY.icon()),
        WARMTH(FIRE),
        FOOD(MessengerScreen.FOOD),
        BODY(HEART),
        GUIDE(MessengerScreen.GUIDE);

        private final ResourceLocation icon;

        Page(ResourceLocation icon) {
            this.icon = icon;
        }

        Component title() {
            return Component.translatable("messenger.backcountry.page." + name().toLowerCase(Locale.ROOT));
        }
    }

    private enum Topic {
        RATIONS,
        CACHES,
        WILDLIFE,
        WATER,
        FIRE,
        COLD,
        HEALTH,
        FOOD,
        HUNTING,
        ROCK;

        Component title() {
            return Component.translatable("messenger.backcountry.guide." + name().toLowerCase(Locale.ROOT));
        }

        Component text() {
            return Component.translatable("messenger.backcountry.guide." + name().toLowerCase(Locale.ROOT) + ".text");
        }
    }

    private record Hit(int x, int y, int width, int height, Runnable action) {
        boolean inside(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record Shown(int x, int y, ItemStack stack) {
        boolean inside(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + ITEM && mouseY >= y && mouseY < y + ITEM;
        }
    }

    public MessengerScreen() {
        super(Component.translatable("item.backcountry.satellite_messenger"));
    }

    @Override
    protected void init() {
        deviceWidth = Math.min(width, MOST_WIDTH);
        deviceHeight = Math.min(height, MOST_HEIGHT);
        left = (width - deviceWidth) / 2;
        top = (height - deviceHeight) / 2;
        line = deviceHeight >= TALL ? TALL_LINE : SHORT_LINE;
        meterRow = deviceHeight >= TALL ? TALL_METER_ROW : SHORT_METER_ROW;
        if (ClientState.unread()) {
            ClientState.read();
            Messages.read();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int lcdLeft() {
        return left + CASE_SIDE;
    }

    private int lcdRight() {
        return left + deviceWidth - CASE_SIDE;
    }

    private int lcdTop() {
        return top + CASE_TOP;
    }

    private int lcdBottom() {
        return top + deviceHeight - CASE_SIDE;
    }

    private int x0() {
        return lcdLeft() + PAD;
    }

    private int x1() {
        return lcdRight() - PAD;
    }

    private int alertsHeight() {
        return ALERT_LINES * line + 7;
    }

    private int contentBottom() {
        return lcdBottom() - alertsHeight() - PAD;
    }

    private static int month() {
        return Calendar.date(Math.max(1, ClientState.view.day())).getMonthValue();
    }

    private static void show(Page shown) {
        page = shown;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        if (asking) {
            return key == GLFW.GLFW_KEY_ESCAPE ? super.keyPressed(key, scan, modifiers) : true;
        }
        boolean back = key == GLFW.GLFW_KEY_LEFT || minecraft.options.keyLeft.matches(key, scan);
        if (back || key == GLFW.GLFW_KEY_RIGHT || minecraft.options.keyRight.matches(key, scan)) {
            show(Page.values()[Math.floorMod(page.ordinal() + (back ? -1 : 1), Page.values().length)]);
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + Page.values().length) {
            show(Page.values()[key - GLFW.GLFW_KEY_1]);
            return true;
        }
        boolean up = key == GLFW.GLFW_KEY_UP || minecraft.options.keyUp.matches(key, scan);
        if (page == Page.TASKS && (up || key == GLFW.GLFW_KEY_DOWN || minecraft.options.keyDown.matches(key, scan))) {
            Task current = chosenTask(ClientState.guide);
            Task next = Task.values()[Math.floorMod(current.ordinal() + (up ? -1 : 1), Task.values().length)];
            chosen = Optional.of(next);
            opened = Optional.of(next.chapter());
            return true;
        }
        if (page == Page.GUIDE && (up || key == GLFW.GLFW_KEY_DOWN || minecraft.options.keyDown.matches(key, scan))) {
            topic = Topic.values()[Math.floorMod(topic.ordinal() + (up ? -1 : 1), Topic.values().length)];
            return true;
        }
        if (page == Page.TASKS && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || minecraft.options.keyJump.matches(key, scan))) {
            track(chosenTask(ClientState.guide));
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        for (Shown shown : items) {
            if (shown.inside(x, y) && ModList.get().isLoaded(JEI)) {
                JeiLink.recipes(shown.stack());
                return true;
            }
        }
        for (Hit hit : List.copyOf(hits)) {
            if (hit.inside(x, y)) {
                hit.action().run();
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private static void track(Task task) {
        GuideView guide = ClientState.guide;
        ClientState.guide = new GuideView(guide.steps(), guide.storesDays(), guide.dryFirewood(), Optional.of(task));
        Messages.track(Optional.of(task));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hits.clear();
        items.clear();
        casing(graphics);
        sosButton(graphics, mouseX, mouseY);
        BodyView view = ClientState.view;
        graphics.drawString(font, Calendar.label(view.day()), x0(), lcdTop() + 4, Look.LCD_DIM, false);
        String time = Words.time(minecraft.level.getDayTime());
        graphics.drawString(font, time, x1() - font.width(time), lcdTop() + 4, Look.LCD_DIM, false);
        tabs(graphics, mouseX, mouseY);
        int y = lcdTop() + CONTENT_Y;
        graphics.enableScissor(lcdLeft(), y, lcdRight(), contentBottom());
        if (asking) {
            sos(graphics, y, mouseX, mouseY);
        } else {
            switch (page) {
                case TASKS -> tasks(graphics, y, mouseX, mouseY);
                case WEATHER -> weather(graphics, y, ClientState.weather);
                case WARMTH -> warmth(graphics, y, view);
                case FOOD -> food(graphics, y, view);
                case BODY -> body(graphics, y, view);
                case GUIDE -> guide(graphics, y, mouseX, mouseY);
            }
        }
        graphics.disableScissor();
        alerts(graphics, view.alerts());
        for (Shown shown : items) {
            if (shown.inside(mouseX, mouseY)) {
                List<Component> lines = new ArrayList<>(List.of(shown.stack().getHoverName()));
                if (ModList.get().isLoaded(JEI)) {
                    lines.add(Component.translatable("messenger.backcountry.recipe"));
                }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }

    private void casing(GuiGraphics graphics) {
        int right = left + deviceWidth;
        int bottom = top + deviceHeight;
        graphics.fill(left, top, right, bottom, Look.CASE_EDGE);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, Look.CASE);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, Look.CASE_LIGHT);
        graphics.fill(left + 1, top + 1, left + 2, bottom - 1, Look.CASE_LIGHT);
        graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, Look.CASE_DARK);
        graphics.fill(right - 2, top + 2, right - 1, bottom - 1, Look.CASE_DARK);
        graphics.drawString(font, Component.literal(title.getString().toUpperCase(Locale.ROOT)), lcdLeft(), top + PAD, Look.CASE_LABEL, false);
        graphics.fill(lcdLeft() - 2, lcdTop() - 2, lcdRight() + 2, lcdBottom() + 2, Look.BEZEL);
        graphics.fill(lcdLeft(), lcdTop(), lcdRight(), lcdBottom(), Look.LCD);
        graphics.fill(lcdLeft(), lcdTop(), lcdRight(), lcdTop() + 1, Look.LCD_GLARE);
    }

    private boolean canTapOut() {
        return ClientState.viewed && ClientState.ended == null && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer() && minecraft.player.isAlive();
    }

    private void sosButton(GuiGraphics graphics, int mouseX, int mouseY) {
        Component label = Component.translatable("messenger.backcountry.sos");
        int buttonWidth = font.width(label) + 2 * TAB_PAD + 2;
        int x = lcdRight() + 2 - buttonWidth;
        int y = top + SOS_TOP;
        int right = x + buttonWidth;
        int bottom = y + SOS_HEIGHT;
        boolean live = canTapOut();
        boolean hovered = live && mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom;
        graphics.fill(x, y, right, bottom, Look.CASE_EDGE);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, live ? hovered ? Look.SOS_LIGHT : Look.SOS : Look.CASE_DARK);
        if (live) {
            graphics.fill(x + 1, y + 1, right - 1, y + 2, Look.SOS_LIGHT);
            graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, Look.SOS_DARK);
        }
        graphics.drawString(font, label, x + (buttonWidth - font.width(label)) / 2, y + 2, live ? Look.TEXT : Look.CASE_LABEL, false);
        if (live) {
            hits.add(new Hit(x, y, buttonWidth, SOS_HEIGHT, () -> asking = true));
        }
    }

    private void sos(GuiGraphics graphics, int y, int mouseX, int mouseY) {
        int x = x0();
        graphics.drawString(font, Component.translatable("messenger.backcountry.sos.question"), x, y + 1, Look.LCD_WARN, false);
        y += line + GAP;
        for (String consequence : SOS_LINES) {
            for (FormattedCharSequence wrapped : font.split(Component.translatable("messenger.backcountry.sos." + consequence), x1() - x)) {
                graphics.drawString(font, wrapped, x, y + 1, Look.LCD_TEXT, false);
                y += line;
            }
        }
        y += GAP + 1;
        int tapOut = button(graphics, x, y, Component.translatable("messenger.backcountry.sos.tap_out"), Look.LCD_DANGER, mouseX, mouseY, () -> {
            asking = false;
            Messages.sos();
        });
        button(graphics, x + tapOut + GAP, y, CommonComponents.GUI_CANCEL, Look.LCD_TEXT, mouseX, mouseY, () -> asking = false);
    }

    private int button(GuiGraphics graphics, int x, int y, Component label, int colour, int mouseX, int mouseY, Runnable action) {
        int labelWidth = font.width(label) + 2 * TAB_PAD;
        boolean hovered = mouseX >= x && mouseX < x + labelWidth && mouseY >= y - 1 && mouseY < y + line;
        graphics.fill(x, y - 1, x + labelWidth, y + line, colour == Look.LCD_TEXT ? Look.LCD_DIM : colour);
        graphics.fill(x + 1, y, x + labelWidth - 1, y + line - 1, hovered ? Look.LCD_TRACK : Look.LCD);
        graphics.drawString(font, label, x + TAB_PAD, y + 1, colour, false);
        hits.add(new Hit(x, y - 1, labelWidth, line + 1, action));
        return labelWidth;
    }

    private void tabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = lcdTop() + TABS_Y;
        int x = x0() - TAB_PAD;
        for (Page shown : Page.values()) {
            int tabWidth = TAB_PAD + ICON + ICON_GAP + font.width(shown.title()) + TAB_PAD;
            boolean hovered = mouseX >= x && mouseX < x + tabWidth && mouseY >= y - 2 && mouseY < y + line - 1;
            boolean selected = shown == page && !asking;
            if (selected) {
                graphics.fill(x, y - 2, x + tabWidth, y + line - 1, Look.LCD_TEXT);
            } else if (hovered) {
                graphics.fill(x, y - 2, x + tabWidth, y + line - 1, Look.LCD_TRACK);
            }
            graphics.blitSprite(shown.icon, x + TAB_PAD, y - 1, ICON, ICON);
            graphics.drawString(font, shown.title(), x + TAB_PAD + ICON + ICON_GAP, y, selected ? Look.LCD : Look.LCD_DIM, false);
            hits.add(new Hit(x, y - 2, tabWidth, line + 1, () -> {
                asking = false;
                show(shown);
            }));
            x += tabWidth + TAB_GAP;
        }
        graphics.fill(x0(), y + line + 1, x1(), y + line + 2, Look.LCD_TRACK);
    }

    private void alerts(GuiGraphics graphics, List<Alert> alerts) {
        int start = x0();
        int right = x1();
        int y = lcdBottom() - PAD - ALERT_LINES * line - 3;
        graphics.fill(start, y, right, y + 1, Look.LCD_TRACK);
        y += 4;
        if (alerts.isEmpty()) {
            graphics.drawString(font, Component.translatable("messenger.backcountry.alerts.none"), start, y + 1, Look.LCD_DIM, false);
            return;
        }
        int cx = start;
        int row = 0;
        for (int i = 0; i < alerts.size(); i++) {
            int width = alertWidth(alerts.get(i));
            if (cx > start && cx + width > right) {
                if (row == ALERT_LINES - 1) {
                    more(graphics, cx, y + row * line, alerts.size() - i);
                    return;
                }
                row++;
                cx = start;
            }
            int rest = alerts.size() - i - 1;
            if (row == ALERT_LINES - 1 && rest > 0 && cx + restWidth(alerts, i) > right && cx + width + ALERT_GAP + font.width(moreText(rest)) > right) {
                more(graphics, cx, y + row * line, rest + 1);
                return;
            }
            Alert alert = alerts.get(i);
            graphics.blitSprite(ALERT, cx, y + row * line, ICON, ICON);
            graphics.drawString(font, alert.title(), cx + ICON + ICON_GAP, y + row * line + 1, alert.danger() ? Look.LCD_DANGER : Look.LCD_WARN, false);
            cx += width + ALERT_GAP;
        }
    }

    private int alertWidth(Alert alert) {
        return ICON + ICON_GAP + font.width(alert.title());
    }

    private int restWidth(List<Alert> alerts, int from) {
        int width = -ALERT_GAP;
        for (int i = from; i < alerts.size(); i++) {
            width += alertWidth(alerts.get(i)) + ALERT_GAP;
        }
        return width;
    }

    private static Component moreText(int count) {
        return Component.translatable("messenger.backcountry.alerts.more", count);
    }

    private void more(GuiGraphics graphics, int x, int y, int count) {
        graphics.drawString(font, moreText(count), x, y + 1, Look.LCD_WARN, false);
    }

    private int row(GuiGraphics graphics, int y, ResourceLocation icon, Component label, Component value, int valueColour) {
        graphics.blitSprite(icon, x0(), y, ICON, ICON);
        graphics.drawString(font, label, x0() + LABEL, y + 1, Look.LCD_DIM, false);
        String shown = font.plainSubstrByWidth(value.getString(), x1() - x0() - LABEL - 4 - font.width(label));
        graphics.drawString(font, shown, x1() - font.width(shown), y + 1, valueColour, false);
        return y + line;
    }

    private int advice(GuiGraphics graphics, int y, Component advice) {
        for (FormattedCharSequence wrapped : font.split(advice, x1() - x0() - LABEL)) {
            graphics.drawString(font, wrapped, x0() + LABEL, y + 1, Look.LCD_DIM, false);
            y += line;
        }
        return y;
    }

    private int meter(GuiGraphics graphics, int y, float share, int colour) {
        int from = x0() + LABEL;
        int to = x1();
        graphics.fill(from, y, to, y + METER, Look.LCD_TRACK);
        graphics.fill(from, y, from + Math.round((to - from) * Mth.clamp(share, 0, 1)), y + METER, colour);
        for (int tick = from + (to - from) / 10; tick < to; tick += (to - from) / 10) {
            graphics.fill(tick, y, tick + 1, y + METER, Look.LCD);
        }
        return y + meterRow - line;
    }

    private void mark(GuiGraphics graphics, int y, float low, float high, float at) {
        int position = position(x0() + LABEL, x1(), low, high, at);
        graphics.fill(position, y - 2, position + 1, y + METER + 2, Look.LCD_DANGER);
    }

    private int gauge(GuiGraphics graphics, int y, float low, float high, float value, float[] zones, int[] colours, float mark) {
        int from = x0() + LABEL;
        int to = x1();
        float start = low;
        for (int i = 0; i <= zones.length; i++) {
            float end = i < zones.length ? zones[i] : high;
            graphics.fill(position(from, to, low, high, start), y, position(from, to, low, high, end), y + METER, colours[i]);
            start = end;
        }
        if (mark > low) {
            mark(graphics, y, low, high, mark);
        }
        int at = position(from, to, low, high, Mth.clamp(value, low, high));
        graphics.fill(at - 1, y - 2, at + 2, y + METER + 2, Look.LCD_TEXT);
        return y + meterRow - line;
    }

    private static int position(int from, int to, float low, float high, float value) {
        return from + Math.round((to - from) * (value - low) / (high - low));
    }

    private static int temperatureColour(float celsius) {
        return celsius <= Balance.get().conditions().bareSkinC() ? Look.LCD_DANGER : celsius <= FREEZING ? Look.LCD_COLD : Look.LCD_TEXT;
    }

    private static Component wind(float windKmh, int from) {
        return Component.translatable("messenger.backcountry.wind_value", Math.round(windKmh), Words.compass(from));
    }

    private Task chosenTask(GuideView guide) {
        return chosen.or(() -> guide.following(month())).orElse(Task.values()[0]);
    }

    private void tasks(GuiGraphics graphics, int y, int mouseX, int mouseY) {
        GuideView guide = ClientState.guide;
        Task task = chosenTask(guide);
        Chapter open = opened.orElse(task.chapter());
        Optional<Task> following = guide.following(month());
        int listWidth = Mth.clamp(Math.round((x1() - x0()) * LIST_SHARE), LIST_LEAST, LIST_MOST);
        int split = x0() + listWidth;
        int listY = y;
        for (Chapter chapter : Chapter.values()) {
            boolean expanded = chapter == open;
            Component heading = chapter.team() ? Component.translatable("messenger.backcountry.tasks.team", chapter.title()) : chapter.title();
            Component count = Component.translatable("messenger.backcountry.tasks.count", guide.done(chapter), chapter.tasks().size());
            boolean hovered = mouseX >= x0() && mouseX < split && mouseY >= listY && mouseY < listY + line;
            graphics.drawString(font, heading, x0(), listY + 1, expanded || hovered ? Look.LCD_TEXT : Look.LCD_DIM, false);
            graphics.drawString(font, count, split - PAD - font.width(count), listY + 1, Look.LCD_DIM, false);
            hits.add(new Hit(x0(), listY, listWidth, line, () -> {
                opened = Optional.of(chapter);
                chosen = Optional.of(chapter.tasks().stream().filter(item -> !ClientState.guide.done(item)).findFirst().orElse(chapter.tasks().getFirst()));
            }));
            listY += line;
            if (!expanded) {
                continue;
            }
            for (Task item : chapter.tasks()) {
                boolean done = guide.done(item);
                if (item == task) {
                    graphics.fill(x0() - 2, listY, split - PAD + 2, listY + line - 1, Look.LCD_TRACK);
                }
                if (following.isPresent() && following.get() == item) {
                    graphics.drawString(font, "◆", x0(), listY + 1, Look.LCD_WARN, false);
                }
                int boxLeft = x0() + font.width("◆") + 1;
                box(graphics, boxLeft, listY + 1, done);
                int textLeft = boxLeft + BOX + ICON_GAP;
                String title = font.plainSubstrByWidth(item.title().getString(), split - PAD - textLeft);
                graphics.drawString(font, title, textLeft, listY + 1, done || !item.inSeason(month()) ? Look.LCD_DIM : Look.LCD_TEXT, false);
                hits.add(new Hit(x0(), listY, listWidth, line, () -> chosen = Optional.of(item)));
                listY += line;
            }
        }
        graphics.fill(split - 1, y, split, contentBottom(), Look.LCD_TRACK);
        detail(graphics, task, guide, split + PAD, y, mouseX, mouseY);
    }

    private void guide(GuiGraphics graphics, int y, int mouseX, int mouseY) {
        int listWidth = Mth.clamp(Math.round((x1() - x0()) * LIST_SHARE), LIST_LEAST, LIST_MOST);
        int split = x0() + listWidth;
        int listY = y;
        for (Topic item : Topic.values()) {
            boolean hovered = mouseX >= x0() && mouseX < split && mouseY >= listY && mouseY < listY + line;
            if (item == topic) {
                graphics.fill(x0() - 2, listY, split - PAD + 2, listY + line - 1, Look.LCD_TRACK);
            }
            String title = font.plainSubstrByWidth(item.title().getString(), split - PAD - x0());
            graphics.drawString(font, title, x0(), listY + 1, item == topic || hovered ? Look.LCD_TEXT : Look.LCD_DIM, false);
            hits.add(new Hit(x0(), listY, listWidth, line, () -> topic = item));
            listY += line;
        }
        graphics.fill(split - 1, y, split, contentBottom(), Look.LCD_TRACK);
        int x = split + PAD;
        graphics.drawString(font, topic.title(), x, y + 1, Look.LCD_TEXT, false);
        y += line + GAP;
        for (FormattedCharSequence wrapped : font.split(topic.text(), x1() - x)) {
            graphics.drawString(font, wrapped, x, y + 1, Look.LCD_TEXT, false);
            y += line;
        }
    }

    private void box(GuiGraphics graphics, int x, int y, boolean done) {
        graphics.fill(x, y, x + BOX, y + BOX, done ? Look.LCD_DIM : Look.LCD_TEXT);
        graphics.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, done ? Look.LCD_DIM : Look.LCD);
    }

    private void detail(GuiGraphics graphics, Task task, GuideView guide, int x, int y, int mouseX, int mouseY) {
        int right = x1();
        Component count = Component.translatable("messenger.backcountry.tasks.count", guide.stepsDone(task), task.steps().size());
        int countLeft = right - font.width(count);
        graphics.drawString(font, count, countLeft, y + 1, Look.LCD_DIM, false);
        boolean tracked = guide.tracked().filter(item -> item == task).isPresent() || guide.tracked().isEmpty() && guide.following(month()).filter(item -> item == task).isPresent();
        int titleRight = countLeft - GAP;
        if (!guide.done(task)) {
            Component label = Component.translatable(tracked ? "messenger.backcountry.tasks.tracked" : "messenger.backcountry.tasks.track");
            int labelWidth = font.width(label) + (tracked ? 0 : 2 * TAB_PAD);
            int labelLeft = countLeft - GAP - labelWidth;
            if (tracked) {
                graphics.drawString(font, label, labelLeft, y + 1, Look.LCD_WARN, false);
            } else {
                button(graphics, labelLeft, y, label, Look.LCD_TEXT, mouseX, mouseY, () -> track(task));
            }
            titleRight = labelLeft - GAP;
        }
        graphics.drawString(font, font.plainSubstrByWidth(task.title().getString(), titleRight - x), x, y + 1, Look.LCD_TEXT, false);
        y += line;
        List<FormattedCharSequence> why = font.split(task.why(), right - x);
        for (FormattedCharSequence wrapped : why.subList(0, Math.min(WHY_LINES, why.size()))) {
            graphics.drawString(font, wrapped, x, y + 1, Look.LCD_DIM, false);
            y += line;
        }
        Optional<Task.Season> season = task.season().filter(range -> !range.holds(month()));
        if (season.isPresent()) {
            graphics.drawString(font, Component.translatable("messenger.backcountry.tasks.from", Words.month(season.get().from())), x, y + 1, Look.LCD_WARN, false);
            y += line;
        }
        y += GAP;
        Optional<Step> next = guide.next(task);
        for (Step step : task.steps()) {
            boolean done = guide.done(task, step);
            box(graphics, x, y + 1, done);
            int textLeft = x + BOX + ICON_GAP;
            boolean current = next.isPresent() && next.get() == step;
            String title = font.plainSubstrByWidth(task.step(step).getString(), right - textLeft);
            graphics.drawString(font, current ? "▸ " + title : title, textLeft, y + 1, done ? Look.LCD_DIM : Look.LCD_TEXT, false);
            y += line;
            if (current) {
                y = needs(graphics, task, step, guide, textLeft, y);
                List<FormattedCharSequence> how = font.split(task.how(step), right - textLeft);
                for (FormattedCharSequence wrapped : how.subList(0, Math.min(HOW_LINES, how.size()))) {
                    graphics.drawString(font, wrapped, textLeft, y + 1, Look.LCD_DIM, false);
                    y += line;
                }
            }
        }
    }

    private int needs(GuiGraphics graphics, Task task, Step step, GuideView guide, int x, int y) {
        Optional<Component> progress = progress(task, guide);
        if (progress.isPresent()) {
            graphics.drawString(font, progress.get(), x, y + 1, Look.LCD_TEXT, false);
            return y + line;
        }
        List<Step.Need> needs = step.shown();
        if (needs.isEmpty()) {
            return y;
        }
        List<ItemStack> carried = Hotbar.carried(minecraft.player);
        int cx = x;
        for (Step.Need need : needs) {
            ItemStack stack = need.thing().stack();
            int have = carried.stream().filter(need.thing()::matches).mapToInt(ItemStack::getCount).sum();
            String count = Math.min(have, need.count()) + "/" + need.count();
            int width = ITEM + 2 + font.width(count);
            if (cx > x && cx + width > x1()) {
                cx = x;
                y += ITEM_ROW;
            }
            graphics.renderItem(stack, cx, y);
            items.add(new Shown(cx, y, stack));
            graphics.drawString(font, count, cx + ITEM + 2, y + 5, have >= need.count() ? Look.LCD_TEXT : Look.LCD_DIM, false);
            cx += width + NEED_GAP;
        }
        return y + ITEM_ROW;
    }

    static Optional<Component> progress(Task task, GuideView guide) {
        Balance.Tasks rules = Balance.get().tasks();
        return switch (task) {
            case FOOD_STORES -> Optional.of(Component.translatable("messenger.backcountry.tasks.days", Words.one(guide.storesDays()), (int) rules.foodDays()));
            case DRY_FIREWOOD -> Optional.of(Component.translatable("messenger.backcountry.tasks.pieces", guide.dryFirewood(), (int) rules.dryFirewood()));
            default -> Optional.empty();
        };
    }

    private void weather(GuiGraphics graphics, int y, WeatherView weather) {
        Balance.Weather rules = Balance.get().weather();
        y = row(graphics, y, TEMPERATURE, Component.translatable("messenger.backcountry.air"),
                Component.translatable("messenger.backcountry.air_value", Words.degrees(weather.airC()), Words.degrees(weather.feelsC())), temperatureColour(weather.feelsC()));
        y = gauge(graphics, y, AIR_LOW, AIR_HIGH, weather.airC(), new float[] {(float) Balance.get().conditions().bareSkinC(), FREEZING},
                new int[] {Look.LCD_DANGER_DIM, Look.LCD_COLD_DIM, Look.LCD_TRACK}, AIR_LOW);
        Component falling = weather.falling().map(kind -> (Component) Component.translatable(weather.now() ? "messenger.backcountry.falling.until" : "messenger.backcountry.falling.from",
                kind.title(weather.intensity()), Words.clock(weather.now() ? weather.toHour() : weather.fromHour()))).orElse(Component.translatable("messenger.backcountry.falling.none"));
        ResourceLocation fallingIcon = weather.falling().map(Kind::icon).orElse(Kind.sky(weather.cloud()).icon());
        y = row(graphics, y, fallingIcon, Component.translatable("messenger.backcountry.precipitation"), falling, weather.falling().isPresent() ? Look.LCD_TEXT : Look.LCD_DIM);
        boolean blizzard = weather.windKmh() >= rules.blizzardWindKmh();
        y = row(graphics, y, WIND, Component.translatable("messenger.backcountry.wind"), wind(weather.windKmh(), weather.windFrom()), blizzard ? Look.LCD_WARN : Look.LCD_TEXT);
        meter(graphics, y, weather.windKmh() / WIND_FULL, blizzard ? Look.LCD_WARN : Look.LCD_TEXT);
        mark(graphics, y, 0, WIND_FULL, (float) rules.blizzardWindKmh());
        y += meterRow - line;
        y = row(graphics, y, Kind.CLOUDY.icon(), Component.translatable("messenger.backcountry.cloud"), Component.literal(Words.percent(weather.cloud())), Look.LCD_TEXT);
        y = row(graphics, y, FORECAST, Component.translatable("messenger.backcountry.today_range"),
                Component.translatable("messenger.backcountry.high_low", Words.degrees(weather.highC()), Words.degrees(weather.lowC())), Look.LCD_TEXT);
        y += GAP;
        graphics.fill(x0(), y, x1(), y + 1, Look.LCD_TRACK);
        y += GAP;
        boolean wide = x1() - x0() >= WIDE;
        double likely = Balance.get().alerts().chance();
        for (int i = 0; i < weather.outlooks().size(); i++) {
            Outlook outlook = weather.outlooks().get(i);
            boolean wet = outlook.likely(likely);
            graphics.blitSprite(wet ? outlook.falling().icon() : outlook.sky().icon(), x0(), y, ICON, ICON);
            Component date = i == 0 ? Component.translatable("messenger.backcountry.tomorrow", Calendar.dateText(outlook.day()))
                    : Component.translatable("calendar.backcountry.day", Calendar.dayOfYear(outlook.day()), Calendar.dateText(outlook.day()));
            Component chance = Component.translatable("messenger.backcountry.chance", outlook.falling().title(), outlook.chance() + "%");
            Component temperatures = Component.translatable("messenger.backcountry.high_low", Words.degrees(outlook.highC()), Words.degrees(outlook.lowC()));
            Component wind = wind(outlook.windKmh(), outlook.windFrom());
            graphics.drawString(font, date, x0() + LABEL, y + 1, Look.LCD_DIM, false);
            if (wide) {
                int span = x1() - x0() - LABEL;
                graphics.drawString(font, chance, x0() + LABEL + span * 3 / 10, y + 1, wet ? Look.LCD_TEXT : Look.LCD_DIM, false);
                graphics.drawString(font, temperatures, x0() + LABEL + span * 9 / 20, y + 1, temperatureColour(outlook.lowC()), false);
                graphics.drawString(font, wind, x1() - font.width(wind), y + 1, Look.LCD_DIM, false);
                y += line;
            } else {
                graphics.drawString(font, chance, x1() - font.width(chance), y + 1, wet ? Look.LCD_TEXT : Look.LCD_DIM, false);
                y += line;
                graphics.drawString(font, temperatures, x0() + LABEL, y + 1, temperatureColour(outlook.lowC()), false);
                graphics.drawString(font, wind, x1() - font.width(wind), y + 1, Look.LCD_DIM, false);
                y += line;
            }
        }
    }

    private void warmth(GuiGraphics graphics, int y, BodyView view) {
        Balance.Warmth warmth = Balance.get().warmth();
        boolean cold = view.coreC() < warmth.shiverCoreC();
        int colour = view.coreC() < Balance.get().conditions().hypothermiaCoreC() ? Look.LCD_DANGER : cold ? Look.LCD_COLD : view.coreC() > warmth.sweatCoreC() ? Look.LCD_WARN : Look.LCD_TEXT;
        y = row(graphics, y, TEMPERATURE, Component.translatable("messenger.backcountry.core"),
                Component.translatable("messenger.backcountry.core_value", Words.one(view.coreC()), Words.comfort(view.comfort()), Words.trend(view.trend())), colour);
        y = gauge(graphics, y, CORE_LOW, CORE_HIGH, view.coreC(), new float[] {(float) Balance.get().conditions().hypothermiaCoreC(), (float) warmth.shiverCoreC(),
                (float) warmth.sweatCoreC()}, new int[] {Look.LCD_DANGER_DIM, Look.LCD_COLD_DIM, Look.LCD_TRACK, Look.LCD_WARN_DIM}, 0);
        if (cold) {
            y = advice(graphics, y, Component.translatable("messenger.backcountry.help.cold"));
        }
        y += GAP;
        y = row(graphics, y, FIRE, Component.translatable("messenger.backcountry.fire"),
                Component.translatable(view.fire() ? "messenger.backcountry.fire.near" : "messenger.backcountry.fire.none"), view.fire() ? Look.LCD_TEXT : Look.LCD_DIM);
        String shelter = view.sheltered() ? "messenger.backcountry.shelter.roofed" : view.windy() ? "messenger.backcountry.shelter.windy" : "messenger.backcountry.shelter.open";
        y = row(graphics, y, SHELTER, Component.translatable("messenger.backcountry.shelter"), Component.translatable(shelter),
                view.sheltered() ? Look.LCD_TEXT : view.windy() ? Look.LCD_COLD : Look.LCD_DIM);
        if (view.windy() && !view.sheltered()) {
            y = advice(graphics, y, Component.translatable("messenger.backcountry.help.wind"));
        }
        y += GAP;
        y = row(graphics, y, CLOTHING, Component.translatable("messenger.backcountry.clothing"), clothing(), Look.LCD_TEXT);
        y = meter(graphics, y, view.clo() / CLOTHING_FULL, Look.LCD_TEXT);
        y = row(graphics, y, WET, Component.translatable("messenger.backcountry.wet"), Component.literal(Words.percent(view.wetness())), Look.LCD_TEXT);
        y = meter(graphics, y, view.wetness(), Look.LCD_WATER);
        if (view.wetness() >= LOW_SHARE) {
            advice(graphics, y, Component.translatable("messenger.backcountry.help.wet"));
        }
    }

    private void food(GuiGraphics graphics, int y, BodyView view) {
        y = row(graphics, y, FOOD, Component.translatable("messenger.backcountry.today"),
                Component.translatable("messenger.backcountry.eaten_burned", Words.kcal(view.kcalEaten()), Words.kcal(view.kcalBurned())), Look.LCD_TEXT);
        y = meter(graphics, y, view.kcalBurned() <= 0 ? 0 : view.kcalEaten() / view.kcalBurned(), Look.LCD_TEXT);
        if (view.burned().size() == Burn.values().length) {
            List<String> parts = new ArrayList<>();
            for (Burn burn : Burn.values()) {
                String kcal = Words.kcal(view.burned().get(burn.ordinal()));
                parts.add(burn == Burn.MOVING ? Component.translatable("messenger.backcountry.burn.moving_distance", kcal, Words.one(view.metresToday() / METRES_A_KM)).getString()
                        : burn.title().getString() + " " + kcal);
            }
            boolean oneLine = font.width(String.join(" · ", parts)) <= x1() - x0() - LABEL;
            for (int burnLine = 0; burnLine < (oneLine ? 1 : 2); burnLine++) {
                List<String> shown = oneLine ? parts : parts.subList(burnLine * BURNS_A_LINE, (burnLine + 1) * BURNS_A_LINE);
                graphics.drawString(font, String.join(" · ", shown), x0() + LABEL, y + 1, Look.LCD_DIM, false);
                y += line;
            }
        }
        Component nutrients = Component.translatable("messenger.backcountry.nutrients", Words.grams(view.protein()), Words.grams(view.carbs()), Words.grams(view.fat()));
        graphics.drawString(font, nutrients, x0() + LABEL, y + 1, Look.LCD_DIM, false);
        y += line + GAP;
        y = row(graphics, y, FOOD, Component.translatable("messenger.backcountry.stomach"), Component.translatable("messenger.backcountry.full", Words.percent(view.stomach())),
                Look.LCD_TEXT);
        y = meter(graphics, y, view.stomach(), Look.LCD_TEXT);
        Ration ration = Ration.values()[view.ration()];
        y = row(graphics, y, RATION, Component.translatable("messenger.backcountry.ration"),
                Component.translatable("messenger.backcountry.ration_value", ration.title(), Words.kcal(ration.mealKcal())), Look.LCD_TEXT);
        Balance.Rations rules = Balance.get().rations();
        int hour = (int) ((minecraft.level.getDayTime() / 1000 + 6) % 24);
        boolean dawn = hour < rules.dawnHour() || hour >= rules.duskHour();
        y = row(graphics, y, FORECAST, Component.translatable("messenger.backcountry.next_meal"),
                Component.translatable(dawn ? "messenger.backcountry.dawn_at" : "messenger.backcountry.dusk_at", Words.clockHour(dawn ? rules.dawnHour() : rules.duskHour())),
                Look.LCD_TEXT);
        boolean low = view.storesDays() < STORES_LOW_DAYS;
        y = row(graphics, y, LOAD, Component.translatable("messenger.backcountry.stores"), Component.translatable("messenger.backcountry.days", Words.one(view.storesDays())),
                low ? Look.LCD_WARN : Look.LCD_TEXT);
        if (low) {
            advice(graphics, y, Component.translatable("messenger.backcountry.help.stores", Balance.get().rations().reachBlocks()));
        }
    }

    private void body(GuiGraphics graphics, int y, BodyView view) {
        Balance.Body rules = Balance.get().body();
        float bmi = view.bmi(rules.heightM());
        boolean thin = bmi < rules.extractionBmi() + BMI_WARNING;
        y = row(graphics, y, HEART, Component.translatable("messenger.backcountry.weight"),
                Component.translatable("messenger.backcountry.weight_value", Words.one(view.weightKg()), Words.one(bmi)), thin ? Look.LCD_DANGER : Look.LCD_TEXT);
        y = gauge(graphics, y, BMI_LOW, BMI_HIGH, bmi, new float[] {(float) rules.extractionBmi()}, new int[] {Look.LCD_DANGER_DIM, Look.LCD_TRACK}, (float) rules.extractionBmi());
        if (thin) {
            y = advice(graphics, y, Component.translatable("messenger.backcountry.help.thin", Words.one(rules.extractionBmi())));
        }
        y = row(graphics, y, ENERGY, Component.translatable("messenger.backcountry.energy"), Component.literal(Words.percent(view.energy())), Look.LCD_TEXT);
        y = meter(graphics, y, view.energy(), Look.LCD_ENERGY);
        if (view.energy() < LOW_SHARE) {
            y = advice(graphics, y, Component.translatable("messenger.backcountry.help.energy"));
        }
        boolean thirsty = view.water() < LOW_SHARE;
        y = row(graphics, y, WET, Component.translatable("messenger.backcountry.water"),
                Component.translatable("messenger.backcountry.litres", Words.one(view.waterL()), Words.one(view.needL())), thirsty ? Look.LCD_WARN : Look.LCD_TEXT);
        y = meter(graphics, y, view.water(), Look.LCD_WATER);
        if (thirsty) {
            y = advice(graphics, y, Component.translatable("messenger.backcountry.help.water"));
        }
        boolean over = view.loadKg() > view.capacityKg();
        y = row(graphics, y, LOAD, Component.translatable("messenger.backcountry.load"),
                Component.translatable("messenger.backcountry.kilograms", Words.one(view.loadKg()), Words.one(view.capacityKg())), over ? Look.LCD_DANGER : Look.LCD_TEXT);
        y = meter(graphics, y, view.loadKg() / view.capacityKg(), over ? Look.LCD_DANGER : Look.LCD_TEXT);
        if (over) {
            y = advice(graphics, y, Component.translatable("messenger.backcountry.help.load"));
        }
        float awakeShare = view.awakeHours() / AWAKE_FULL_HOURS;
        y = row(graphics, y, AWAKE, Component.translatable("messenger.backcountry.awake"), Component.translatable("messenger.backcountry.hours", Math.round(view.awakeHours())),
                awakeShare > TIRED_SHARE ? Look.LCD_WARN : Look.LCD_TEXT);
        y += GAP;
        if (view.conditions().isEmpty()) {
            graphics.drawString(font, Component.translatable("messenger.backcountry.conditions.none"), x0(), y + 1, Look.LCD_DIM, false);
            y += line;
        }
        for (Condition condition : view.conditions()) {
            graphics.blitSprite(condition.icon(), x0(), y, ICON, ICON);
            graphics.drawString(font, condition.title(), x0() + LABEL, y + 1, condition.deadly() ? Look.LCD_DANGER : Look.LCD_WARN, false);
            int cureLeft = x0() + LABEL + font.width(condition.title()) + GAP;
            String cure = font.plainSubstrByWidth(Component.translatable("condition.backcountry." + condition.id() + ".cure").getString(), x1() - cureLeft);
            graphics.drawString(font, cure, cureLeft, y + 1, Look.LCD_DIM, false);
            y += line;
        }
        y += GAP;
        int columns = x1() - x0() >= WIDE ? Skill.values().length : 2;
        int column = (x1() - x0()) / columns;
        for (Skill skill : Skill.values()) {
            int level = view.skills().size() == Skill.values().length ? view.skills().get(skill.ordinal()) : 1;
            int cx = x0() + (skill.ordinal() % columns) * column;
            int cy = y + (skill.ordinal() / columns) * line;
            graphics.drawString(font, skill.title(), cx, cy + 1, Look.LCD_DIM, false);
            int barRight = cx + column - PAD;
            int barLeft = barRight - Skill.HIGHEST * SKILL_STEP;
            for (int step = 0; step < Skill.HIGHEST; step++) {
                graphics.fill(barLeft + step * SKILL_STEP, cy + 2, barLeft + step * SKILL_STEP + SKILL_STEP - 1, cy + 2 + METER + 3, step < level ? Look.LCD_TEXT : Look.LCD_TRACK);
            }
        }
    }

    private Component clothing() {
        List<String> worn = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = minecraft.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                worn.add(stack.getHoverName().getString());
            }
        }
        return worn.isEmpty() ? Component.translatable("messenger.backcountry.clothing.arrival") : Component.literal(String.join(", ", worn));
    }
}
