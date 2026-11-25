package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Burn;
import io.github.roman16.backcountry.food.Ration;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.guide.Task;
import io.github.roman16.backcountry.net.Messages;
import io.github.roman16.backcountry.weather.Kind;
import io.github.roman16.backcountry.weather.Outlook;
import io.github.roman16.backcountry.weather.WeatherView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
    private static final String[] PAGES = {"tasks", "weather", "forecast", "warmth", "body", "nutrition", "rations", "vitals"};
    private static final ResourceLocation[] PAGE_ICONS = {TASKS, Kind.PARTLY_CLOUDY.icon(), FORECAST, FIRE, TEMPERATURE, FOOD, RATION, HEART};
    private static final int WIDTH = 232;
    private static final int HEIGHT = 214;
    private static final int LCD_X = 12;
    private static final int LCD_Y = 24;
    private static final int LCD_WIDTH = 208;
    private static final int LCD_HEIGHT = 164;
    private static final int PAD = 6;
    private static final int ICON = 9;
    private static final int ICON_GAP = 3;
    private static final int METER = 3;
    private static final int ROW = 20;
    private static final int LINE = 11;
    private static final int DAY_GAP = 6;
    private static final int ALERT_GAP = 8;
    private static final int ALERT_LINES = 2;
    private static final int KEY_WIDTH = 22;
    private static final int KEY_HEIGHT = 12;
    private static final int ANTENNA_WIDTH = 8;
    private static final int ANTENNA_HEIGHT = 16;
    private static final int TABS_Y = 15;
    private static final int TAB_PAD = 4;
    private static final int CONTENT_Y = 32;
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
    private static final int SHOWN_CONDITIONS = 6;
    private static final int TASK_LINES = 8;
    private static final int HOW_LINES = 2;
    private static final int BOX = 7;
    private static final int BURN_LINES = 2;
    private static final int SKILL_STEP = 4;
    private static final int BURNS_A_LINE = 3;
    private static int page;
    private int left;
    private int top;

    public MessengerScreen() {
        super(Component.translatable("item.backcountry.satellite_messenger"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        if (ClientState.unread()) {
            ClientState.read();
            Messages.read();
        }
    }

    private void show(int index) {
        page = Math.floorMod(index, PAGES.length);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            show(page + (key == GLFW.GLFW_KEY_LEFT ? -1 : 1));
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + PAGES.length) {
            show(key - GLFW.GLFW_KEY_1);
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        for (int i = 0; i < PAGES.length; i++) {
            if (inside(x, y, tabLeft(i), tabsTop() - 2, tabWidth(i), LINE + 1)) {
                show(i);
                return true;
            }
        }
        if (inside(x, y, keyLeft(), keyTop(), KEY_WIDTH, KEY_HEIGHT)) {
            show(page - 1);
            return true;
        }
        if (inside(x, y, keyLeft() + KEY_WIDTH + PAD, keyTop(), KEY_WIDTH, KEY_HEIGHT)) {
            show(page + 1);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private static boolean inside(double x, double y, int boxLeft, int boxTop, int boxWidth, int boxHeight) {
        return x >= boxLeft && x < boxLeft + boxWidth && y >= boxTop && y < boxTop + boxHeight;
    }

    private int keyLeft() {
        return left + WIDTH / 2 - KEY_WIDTH - PAD / 2;
    }

    private int keyTop() {
        return top + LCD_Y + LCD_HEIGHT + (HEIGHT - LCD_Y - LCD_HEIGHT - KEY_HEIGHT) / 2;
    }

    private int tabsTop() {
        return top + LCD_Y + TABS_Y;
    }

    private int shownTabWidth() {
        return TAB_PAD + ICON + ICON_GAP + font.width(pageName(page)) + TAB_PAD;
    }

    private int tabWidth(int index) {
        return index == page ? shownTabWidth() : (LCD_WIDTH - shownTabWidth()) / (PAGES.length - 1);
    }

    private int tabLeft(int index) {
        int x = left + LCD_X;
        for (int i = 0; i < index; i++) {
            x += tabWidth(i);
        }
        return x;
    }

    private static Component pageName(int index) {
        return Component.translatable("messenger.backcountry.page." + PAGES[index]);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        casing(graphics);
        int x = left + LCD_X;
        int y = top + LCD_Y;
        BodyView view = ClientState.view;
        graphics.drawString(font, Calendar.label(view.day()), x + PAD, y + 3, Look.LCD_DIM, false);
        Component signal = Component.translatable("messenger.backcountry.signal");
        graphics.drawString(font, signal, x + LCD_WIDTH - PAD - font.width(signal), y + 3, Look.LCD_DIM, false);
        int hovered = tabs(graphics, x, mouseX, mouseY);
        int row = y + CONTENT_Y;
        switch (PAGES[page]) {
            case "tasks" -> tasks(graphics, x, row, ClientState.guide);
            case "weather" -> weather(graphics, x, row, ClientState.weather);
            case "forecast" -> forecast(graphics, x, row, ClientState.weather);
            case "warmth" -> warmth(graphics, x, row, view);
            case "body" -> body(graphics, x, row, view);
            case "nutrition" -> nutrition(graphics, x, row, view);
            case "rations" -> rations(graphics, x, row, view);
            default -> vitals(graphics, x, row, view);
        }
        alerts(graphics, x, view.alerts());
        if (hovered >= 0) {
            graphics.renderTooltip(font, pageName(hovered), mouseX, mouseY);
        }
    }

    private void casing(GuiGraphics graphics) {
        int right = left + WIDTH;
        int bottom = top + HEIGHT;
        int antenna = right - 3 * ANTENNA_WIDTH;
        graphics.fill(antenna, top - ANTENNA_HEIGHT, antenna + ANTENNA_WIDTH, top + 1, Look.CASE_EDGE);
        graphics.fill(antenna + 1, top - ANTENNA_HEIGHT + 1, antenna + ANTENNA_WIDTH - 1, top + 1, Look.CASE_KNOB);
        graphics.fill(left, top, right, bottom, Look.CASE_EDGE);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, Look.CASE);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, Look.CASE_LIGHT);
        graphics.fill(left + 1, top + 1, left + 2, bottom - 1, Look.CASE_LIGHT);
        graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, Look.CASE_DARK);
        graphics.fill(right - 2, top + 2, right - 1, bottom - 1, Look.CASE_DARK);
        graphics.drawString(font, Component.literal(title.getString().toUpperCase(Locale.ROOT)), left + LCD_X, top + PAD + 2, Look.CASE_LABEL, false);
        int x = left + LCD_X;
        int y = top + LCD_Y;
        graphics.fill(x - 2, y - 2, x + LCD_WIDTH + 2, y + LCD_HEIGHT + 2, Look.BEZEL);
        graphics.fill(x, y, x + LCD_WIDTH, y + LCD_HEIGHT, Look.LCD);
        graphics.fill(x, y, x + LCD_WIDTH, y + 1, Look.LCD_GLARE);
        for (int i = 0; i < 2; i++) {
            int keyLeft = keyLeft() + i * (KEY_WIDTH + PAD);
            graphics.fill(keyLeft, keyTop(), keyLeft + KEY_WIDTH, keyTop() + KEY_HEIGHT, Look.CASE_EDGE);
            graphics.fill(keyLeft + 1, keyTop() + 1, keyLeft + KEY_WIDTH - 1, keyTop() + KEY_HEIGHT - 1, Look.KEY);
            Component arrow = Component.literal(i == 0 ? "◀" : "▶");
            graphics.drawString(font, arrow, keyLeft + (KEY_WIDTH - font.width(arrow)) / 2, keyTop() + 2, Look.CASE_LIGHT, false);
        }
    }

    private int tabs(GuiGraphics graphics, int x, int mouseX, int mouseY) {
        int y = tabsTop();
        int hovered = -1;
        for (int i = 0; i < PAGES.length; i++) {
            int tabLeft = tabLeft(i);
            int tabWidth = tabWidth(i);
            if (i == page) {
                graphics.fill(tabLeft + 1, y - 2, tabLeft + tabWidth - 1, y + LINE - 1, Look.LCD_TEXT);
                graphics.blitSprite(PAGE_ICONS[i], tabLeft + TAB_PAD, y - 1, ICON, ICON);
                graphics.drawString(font, pageName(i), tabLeft + TAB_PAD + ICON + ICON_GAP, y, Look.LCD, false);
                continue;
            }
            if (inside(mouseX, mouseY, tabLeft, y - 2, tabWidth, LINE + 1)) {
                graphics.fill(tabLeft + 1, y - 2, tabLeft + tabWidth - 1, y + LINE - 1, Look.LCD_TRACK);
                hovered = i;
            }
            graphics.blitSprite(PAGE_ICONS[i], tabLeft + (tabWidth - ICON) / 2, y - 1, ICON, ICON);
        }
        graphics.fill(x + PAD, y + LINE + 1, x + LCD_WIDTH - PAD, y + LINE + 2, Look.LCD_TRACK);
        return hovered;
    }

    private void alerts(GuiGraphics graphics, int x, List<Alert> alerts) {
        int start = x + PAD;
        int right = x + LCD_WIDTH - PAD;
        int y = top + LCD_Y + LCD_HEIGHT - PAD - ALERT_LINES * LINE - 3;
        graphics.fill(start, y, right, y + 1, Look.LCD_TRACK);
        y += 4;
        if (alerts.isEmpty()) {
            graphics.drawString(font, Component.translatable("messenger.backcountry.alerts.none"), start, y + 1, Look.LCD_DIM, false);
            return;
        }
        int cx = start;
        int line = 0;
        for (int i = 0; i < alerts.size(); i++) {
            int width = alertWidth(alerts.get(i));
            if (cx > start && cx + width > right) {
                if (line == ALERT_LINES - 1) {
                    more(graphics, cx, y + line * LINE, alerts.size() - i);
                    return;
                }
                line++;
                cx = start;
            }
            int rest = alerts.size() - i - 1;
            if (line == ALERT_LINES - 1 && rest > 0 && cx + restWidth(alerts, i) > right
                    && cx + width + ALERT_GAP + font.width(moreText(rest)) > right) {
                more(graphics, cx, y + line * LINE, rest + 1);
                return;
            }
            Alert alert = alerts.get(i);
            graphics.blitSprite(ALERT, cx, y + line * LINE, ICON, ICON);
            graphics.drawString(font, alert.title(), cx + ICON + ICON_GAP, y + line * LINE + 1, alert.danger() ? Look.LCD_DANGER : Look.LCD_WARN, false);
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

    private int row(GuiGraphics graphics, int x, int y, ResourceLocation icon, Component label, Component value, int valueColour) {
        graphics.blitSprite(icon, x + PAD, y, ICON, ICON);
        graphics.drawString(font, label, x + PAD + ICON + 4, y + 1, Look.LCD_DIM, false);
        String shown = font.plainSubstrByWidth(value.getString(), LCD_WIDTH - 2 * PAD - ICON - 8 - font.width(label));
        graphics.drawString(font, shown, x + LCD_WIDTH - PAD - font.width(shown), y + 1, valueColour, false);
        return y + LINE;
    }

    private void meter(GuiGraphics graphics, int x, int y, float share, int colour) {
        int from = x + PAD + ICON + 4;
        int to = x + LCD_WIDTH - PAD;
        graphics.fill(from, y, to, y + METER, Look.LCD_TRACK);
        graphics.fill(from, y, from + Math.round((to - from) * Mth.clamp(share, 0, 1)), y + METER, colour);
        for (int tick = from + (to - from) / 10; tick < to; tick += (to - from) / 10) {
            graphics.fill(tick, y, tick + 1, y + METER, Look.LCD);
        }
    }

    private void mark(GuiGraphics graphics, int x, int y, float low, float high, float at) {
        int position = position(x + PAD + ICON + 4, x + LCD_WIDTH - PAD, low, high, at);
        graphics.fill(position, y - 2, position + 1, y + METER + 2, Look.LCD_DANGER);
    }

    private void gauge(GuiGraphics graphics, int x, int y, float low, float high, float value, float[] zones, int[] colours, float mark) {
        int from = x + PAD + ICON + 4;
        int to = x + LCD_WIDTH - PAD;
        float start = low;
        for (int i = 0; i <= zones.length; i++) {
            float end = i < zones.length ? zones[i] : high;
            graphics.fill(position(from, to, low, high, start), y, position(from, to, low, high, end), y + METER, colours[i]);
            start = end;
        }
        if (mark > low) {
            mark(graphics, x, y, low, high, mark);
        }
        int at = position(from, to, low, high, Mth.clamp(value, low, high));
        graphics.fill(at - 1, y - 2, at + 2, y + METER + 2, Look.LCD_TEXT);
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

    private void weather(GuiGraphics graphics, int x, int y, WeatherView weather) {
        Balance.Weather rules = Balance.get().weather();
        y = row(graphics, x, y, TEMPERATURE, Component.translatable("messenger.backcountry.air"),
                Component.translatable("messenger.backcountry.air_value", Words.degrees(weather.airC()), Words.degrees(weather.feelsC())), temperatureColour(weather.feelsC()));
        gauge(graphics, x, y, AIR_LOW, AIR_HIGH, weather.airC(), new float[] {(float) Balance.get().conditions().bareSkinC(), FREEZING},
                new int[] {Look.LCD_DANGER_DIM, Look.LCD_COLD_DIM, Look.LCD_TRACK}, AIR_LOW);
        y += ROW - LINE;
        Component falling = weather.falling().map(kind -> (Component) Component.translatable(weather.now() ? "messenger.backcountry.falling.until" : "messenger.backcountry.falling.from",
                kind.title(weather.intensity()), Words.clock(weather.now() ? weather.toHour() : weather.fromHour()))).orElse(Component.translatable("messenger.backcountry.falling.none"));
        ResourceLocation fallingIcon = weather.falling().map(Kind::icon).orElse(Kind.sky(weather.cloud()).icon());
        y = row(graphics, x, y, fallingIcon, Component.translatable("messenger.backcountry.precipitation"), falling,
                weather.falling().isPresent() ? Look.LCD_TEXT : Look.LCD_DIM);
        y += ROW - LINE;
        boolean blizzard = weather.windKmh() >= rules.blizzardWindKmh();
        y = row(graphics, x, y, WIND, Component.translatable("messenger.backcountry.wind"), wind(weather.windKmh(), weather.windFrom()), blizzard ? Look.LCD_WARN : Look.LCD_TEXT);
        meter(graphics, x, y, weather.windKmh() / WIND_FULL, blizzard ? Look.LCD_WARN : Look.LCD_TEXT);
        mark(graphics, x, y, 0, WIND_FULL, (float) rules.blizzardWindKmh());
        y += ROW - LINE;
        y = row(graphics, x, y, Kind.CLOUDY.icon(), Component.translatable("messenger.backcountry.cloud"), Component.literal(Words.percent(weather.cloud())), Look.LCD_TEXT);
        meter(graphics, x, y, weather.cloud(), Look.LCD_TEXT);
        y += ROW - LINE;
        row(graphics, x, y, FORECAST, Component.translatable("messenger.backcountry.today_range"),
                Component.translatable("messenger.backcountry.high_low", Words.degrees(weather.highC()), Words.degrees(weather.lowC())), Look.LCD_TEXT);
    }

    private void forecast(GuiGraphics graphics, int x, int y, WeatherView weather) {
        double likely = Balance.get().alerts().chance();
        for (int i = 0; i < weather.outlooks().size(); i++) {
            Outlook outlook = weather.outlooks().get(i);
            boolean wet = outlook.likely(likely);
            graphics.blitSprite(wet ? outlook.falling().icon() : outlook.sky().icon(), x + PAD, y, ICON, ICON);
            Component date = i == 0 ? Component.translatable("messenger.backcountry.tomorrow", Calendar.dateText(outlook.day()))
                    : Component.translatable("calendar.backcountry.day", Calendar.dayOfYear(outlook.day()), Calendar.dateText(outlook.day()));
            graphics.drawString(font, date, x + PAD + ICON + 4, y + 1, Look.LCD_DIM, false);
            Component chance = Component.translatable("messenger.backcountry.chance", outlook.falling().title(), outlook.chance() + "%");
            graphics.drawString(font, chance, x + LCD_WIDTH - PAD - font.width(chance), y + 1, wet ? Look.LCD_TEXT : Look.LCD_DIM, false);
            y += LINE;
            Component temperatures = Component.translatable("messenger.backcountry.high_low", Words.degrees(outlook.highC()), Words.degrees(outlook.lowC()));
            graphics.drawString(font, temperatures, x + PAD + ICON + 4, y + 1, temperatureColour(outlook.lowC()), false);
            Component wind = wind(outlook.windKmh(), outlook.windFrom());
            graphics.drawString(font, wind, x + LCD_WIDTH - PAD - font.width(wind), y + 1, Look.LCD_DIM, false);
            y += LINE + DAY_GAP;
        }
    }

    private void tasks(GuiGraphics graphics, int x, int y, GuideView guide) {
        Component stage = guide.stage().title();
        graphics.drawString(font, stage, x + PAD, y + 1, Look.LCD_DIM, false);
        if (!guide.stage().team()) {
            Component count = Component.translatable("messenger.backcountry.tasks.count", guide.done().size(), guide.tasks().size());
            graphics.drawString(font, count, x + LCD_WIDTH - PAD - font.width(count), y + 1, Look.LCD_DIM, false);
        }
        y += LINE;
        int width = LCD_WIDTH - 2 * PAD - BOX - ICON_GAP;
        List<Task> open = guide.tasks().stream().filter(task -> !guide.done(task)).limit(Balance.get().tasks().shown()).toList();
        List<FormattedCharSequence> how = open.isEmpty() ? List.of() : font.split(open.getFirst().how(), width);
        int lines = open.size() + Math.min(HOW_LINES, how.size());
        List<Task> done = guide.done();
        List<Task> shownDone = done.subList(Math.max(0, done.size() - Math.max(0, TASK_LINES - 1 - lines)), done.size());
        for (Task task : shownDone) {
            y = task(graphics, x, y, task, true);
        }
        for (Task task : open) {
            y = task(graphics, x, y, task, false);
            if (task == open.getFirst()) {
                for (FormattedCharSequence line : how.subList(0, Math.min(HOW_LINES, how.size()))) {
                    graphics.drawString(font, line, x + PAD + BOX + ICON_GAP, y + 1, Look.LCD_DIM, false);
                    y += LINE;
                }
            }
        }
    }

    private int task(GuiGraphics graphics, int x, int y, Task task, boolean done) {
        int left = x + PAD;
        int boxTop = y + 1;
        graphics.fill(left, boxTop, left + BOX, boxTop + BOX, done ? Look.LCD_DIM : Look.LCD_TEXT);
        graphics.fill(left + 1, boxTop + 1, left + BOX - 1, boxTop + BOX - 1, done ? Look.LCD_DIM : Look.LCD);
        String title = font.plainSubstrByWidth(task.title().getString(), LCD_WIDTH - 2 * PAD - BOX - ICON_GAP);
        graphics.drawString(font, title, left + BOX + ICON_GAP, y + 1, done ? Look.LCD_DIM : Look.LCD_TEXT, false);
        return y + LINE;
    }

    private void warmth(GuiGraphics graphics, int x, int y, BodyView view) {
        y = row(graphics, x, y, FIRE, Component.translatable("messenger.backcountry.fire"),
                Component.translatable(view.fire() ? "messenger.backcountry.fire.near" : "messenger.backcountry.fire.none"), view.fire() ? Look.LCD_TEXT : Look.LCD_DIM);
        y += ROW - LINE;
        String shelter = view.sheltered() ? "messenger.backcountry.shelter.roofed" : view.windy() ? "messenger.backcountry.shelter.windy" : "messenger.backcountry.shelter.open";
        y = row(graphics, x, y, SHELTER, Component.translatable("messenger.backcountry.shelter"), Component.translatable(shelter),
                view.sheltered() ? Look.LCD_TEXT : view.windy() ? Look.LCD_COLD : Look.LCD_DIM);
        y += ROW - LINE;
        y = row(graphics, x, y, WET, Component.translatable("messenger.backcountry.wet"), Component.literal(Words.percent(view.wetness())), Look.LCD_TEXT);
        meter(graphics, x, y, view.wetness(), Look.LCD_WATER);
        y += ROW - LINE;
        y = row(graphics, x, y, CLOTHING, Component.translatable("messenger.backcountry.clothing"), clothing(), Look.LCD_TEXT);
        meter(graphics, x, y, view.clo() / CLOTHING_FULL, Look.LCD_TEXT);
    }

    private void body(GuiGraphics graphics, int x, int y, BodyView view) {
        int colour = view.coreC() < Balance.get().conditions().frostCoreC() ? Look.LCD_DANGER
                : view.coreC() < Balance.get().warmth().shiverCoreC() ? Look.LCD_COLD : view.coreC() > Balance.get().warmth().sweatCoreC() ? Look.LCD_WARN : Look.LCD_TEXT;
        y = row(graphics, x, y, TEMPERATURE, Component.translatable("messenger.backcountry.core"),
                Component.translatable("messenger.backcountry.core_value", Words.one(view.coreC()), Words.comfort(view.comfort()), Words.trend(view.trend())), colour);
        Balance.Warmth warmth = Balance.get().warmth();
        gauge(graphics, x, y, CORE_LOW, CORE_HIGH, view.coreC(), new float[] {(float) Balance.get().conditions().frostCoreC(), (float) warmth.shiverCoreC(),
                (float) warmth.sweatCoreC()}, new int[] {Look.LCD_DANGER_DIM, Look.LCD_COLD_DIM, Look.LCD_TRACK, Look.LCD_WARN_DIM}, 0);
        y += ROW - LINE;
        y = row(graphics, x, y, WET, Component.translatable("messenger.backcountry.wet"), Component.literal(Words.percent(view.wetness())), Look.LCD_TEXT);
        meter(graphics, x, y, view.wetness(), Look.LCD_WATER);
        y += ROW - LINE;
        float awakeShare = view.awakeHours() / AWAKE_FULL_HOURS;
        y = row(graphics, x, y, AWAKE, Component.translatable("messenger.backcountry.awake"), Component.translatable("messenger.backcountry.hours", Math.round(view.awakeHours())),
                awakeShare > 0.75 ? Look.LCD_WARN : Look.LCD_TEXT);
        meter(graphics, x, y, awakeShare, awakeShare > 0.75 ? Look.LCD_WARN : Look.LCD_TEXT);
        y += ROW - LINE;
        if (view.conditions().isEmpty()) {
            graphics.drawString(font, Component.translatable("messenger.backcountry.conditions.none"), x + PAD, y + 1, Look.LCD_DIM, false);
            return;
        }
        int column = (LCD_WIDTH - 2 * PAD) / 2;
        for (int i = 0; i < Math.min(SHOWN_CONDITIONS, view.conditions().size()); i++) {
            Condition condition = view.conditions().get(i);
            int cx = x + PAD + (i % 2) * column;
            int cy = y + (i / 2) * LINE;
            graphics.blitSprite(condition.icon(), cx, cy, ICON, ICON);
            graphics.drawString(font, condition.title(), cx + ICON + 3, cy + 1, condition == Condition.SEVERE_FROSTBITE ? Look.LCD_DANGER : Look.LCD_WARN, false);
        }
    }

    private void nutrition(GuiGraphics graphics, int x, int y, BodyView view) {
        y = row(graphics, x, y, FOOD, Component.translatable("messenger.backcountry.today"),
                Component.translatable("messenger.backcountry.eaten_burned", Words.kcal(view.kcalEaten()), Words.kcal(view.kcalBurned())), Look.LCD_TEXT);
        meter(graphics, x, y, view.kcalBurned() <= 0 ? 0 : view.kcalEaten() / view.kcalBurned(), Look.LCD_TEXT);
        y += ROW - LINE;
        if (view.burned().size() == Burn.values().length) {
            for (int line = 0; line < BURN_LINES; line++) {
                List<String> parts = new ArrayList<>();
                for (int i = line * BURNS_A_LINE; i < (line + 1) * BURNS_A_LINE; i++) {
                    parts.add(Burn.values()[i].title().getString() + " " + Words.kcal(view.burned().get(i)));
                }
                String shown = font.plainSubstrByWidth(String.join(" · ", parts), LCD_WIDTH - 2 * PAD - ICON - 4);
                graphics.drawString(font, shown, x + PAD + ICON + 4, y + 1, Look.LCD_DIM, false);
                y += LINE;
            }
        }
        Component nutrients = Component.translatable("messenger.backcountry.nutrients", Words.grams(view.protein()), Words.grams(view.carbs()), Words.grams(view.fat()));
        graphics.drawString(font, nutrients, x + PAD + ICON + 4, y + 1, Look.LCD_TEXT, false);
        y += ROW;
        y = row(graphics, x, y, ENERGY, Component.translatable("messenger.backcountry.energy"), Component.literal(Words.percent(view.energy())), Look.LCD_TEXT);
        meter(graphics, x, y, view.energy(), Look.LCD_ENERGY);
        y += ROW - LINE;
        y = row(graphics, x, y, FOOD, Component.translatable("messenger.backcountry.stomach"),
                Component.translatable("messenger.backcountry.full", Words.percent(view.stomach())), Look.LCD_TEXT);
        meter(graphics, x, y, view.stomach(), Look.LCD_TEXT);
    }

    private void rations(GuiGraphics graphics, int x, int y, BodyView view) {
        Ration ration = Ration.values()[view.ration()];
        y = row(graphics, x, y, FOOD, Component.translatable("messenger.backcountry.stomach"),
                Component.translatable("messenger.backcountry.full", Words.percent(view.stomach())), Look.LCD_TEXT);
        meter(graphics, x, y, view.stomach(), Look.LCD_TEXT);
        y += ROW - LINE;
        y = row(graphics, x, y, RATION, Component.translatable("messenger.backcountry.ration"),
                Component.translatable("messenger.backcountry.ration_value", ration.title(), Words.kcal(ration.mealKcal())), Look.LCD_TEXT);
        Balance.Rations rules = Balance.get().rations();
        int hour = (int) ((minecraft.level.getDayTime() / 1000 + 6) % 24);
        boolean dawn = hour < rules.dawnHour() || hour >= rules.duskHour();
        y = row(graphics, x, y, FORECAST, Component.translatable("messenger.backcountry.next_meal"),
                Component.translatable(dawn ? "messenger.backcountry.dawn_at" : "messenger.backcountry.dusk_at", Words.clockHour(dawn ? rules.dawnHour() : rules.duskHour())),
                Look.LCD_TEXT);
        row(graphics, x, y, LOAD, Component.translatable("messenger.backcountry.stores"), Component.translatable("messenger.backcountry.days", Words.one(view.storesDays())),
                view.storesDays() < 2 ? Look.LCD_WARN : Look.LCD_TEXT);
    }

    private void vitals(GuiGraphics graphics, int x, int y, BodyView view) {
        Balance.Body rules = Balance.get().body();
        float bmi = view.bmi(rules.heightM());
        y = row(graphics, x, y, HEART, Component.translatable("messenger.backcountry.weight"),
                Component.translatable("messenger.backcountry.weight_value", Words.one(view.weightKg()), Words.one(bmi)), bmi < rules.extractionBmi() + 1 ? Look.LCD_DANGER
                        : Look.LCD_TEXT);
        gauge(graphics, x, y, BMI_LOW, BMI_HIGH, bmi, new float[] {(float) rules.extractionBmi()}, new int[] {Look.LCD_DANGER_DIM, Look.LCD_TRACK},
                (float) rules.extractionBmi());
        y += ROW - LINE;
        y = row(graphics, x, y, ENERGY, Component.translatable("messenger.backcountry.energy"), Component.literal(Words.percent(view.energy())), Look.LCD_TEXT);
        meter(graphics, x, y, view.energy(), Look.LCD_ENERGY);
        y += ROW - LINE;
        y = row(graphics, x, y, WET, Component.translatable("messenger.backcountry.water"),
                Component.translatable("messenger.backcountry.litres", Words.one(view.waterL()), Words.one(view.needL())), view.water() < 0.25 ? Look.LCD_WARN : Look.LCD_TEXT);
        meter(graphics, x, y, view.water(), Look.LCD_WATER);
        y += ROW - LINE;
        boolean over = view.loadKg() > view.capacityKg();
        y = row(graphics, x, y, LOAD, Component.translatable("messenger.backcountry.load"),
                Component.translatable("messenger.backcountry.kilograms", Words.one(view.loadKg()), Words.one(view.capacityKg())), over ? Look.LCD_DANGER : Look.LCD_TEXT);
        meter(graphics, x, y, view.loadKg() / view.capacityKg(), over ? Look.LCD_DANGER : Look.LCD_TEXT);
        y += ROW - LINE;
        int column = (LCD_WIDTH - 2 * PAD) / 2;
        for (Skill skill : Skill.values()) {
            int level = view.skills().size() == Skill.values().length ? view.skills().get(skill.ordinal()) : 1;
            int cx = x + PAD + (skill.ordinal() % 2) * column;
            int cy = y + (skill.ordinal() / 2) * LINE;
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
