package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Condition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation CLOTHING = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation FOOD = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final String[] PAGES = {"warmth", "body", "nutrition", "vitals"};
    private static final int WIDTH = 232;
    private static final int HEIGHT = 174;
    private static final int LCD_X = 12;
    private static final int LCD_Y = 24;
    private static final int LCD_WIDTH = 208;
    private static final int LCD_HEIGHT = 124;
    private static final int PAD = 6;
    private static final int ICON = 9;
    private static final int METER = 3;
    private static final int ROW = 20;
    private static final int LINE = 11;
    private static final int KEY_WIDTH = 22;
    private static final int KEY_HEIGHT = 12;
    private static final int ANTENNA_WIDTH = 8;
    private static final int ANTENNA_HEIGHT = 16;
    private static final int TABS_Y = 15;
    private static final int CONTENT_Y = 32;
    private static final float CORE_LOW = 33;
    private static final float CORE_HIGH = 39;
    private static final float BMI_LOW = 15;
    private static final float BMI_HIGH = 25;
    private static final float CLOTHING_FULL = 6;
    private static final float AWAKE_FULL_HOURS = 48;
    private static final int SHOWN_CONDITIONS = 6;
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
        int tabWidth = LCD_WIDTH / PAGES.length;
        for (int i = 0; i < PAGES.length; i++) {
            int tabLeft = left + LCD_X + i * tabWidth;
            if (inside(x, y, tabLeft, top + LCD_Y + TABS_Y - 2, tabWidth, LINE + 1)) {
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
        tabs(graphics, x, y + TABS_Y, mouseX, mouseY);
        int row = y + CONTENT_Y;
        switch (PAGES[page]) {
            case "warmth" -> warmth(graphics, x, row, view);
            case "body" -> body(graphics, x, row, view);
            case "nutrition" -> nutrition(graphics, x, row, view);
            default -> vitals(graphics, x, row, view);
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

    private void tabs(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int tabWidth = LCD_WIDTH / PAGES.length;
        for (int i = 0; i < PAGES.length; i++) {
            Component name = Component.translatable("messenger.backcountry.page." + PAGES[i]);
            int tabLeft = x + i * tabWidth;
            boolean shown = i == page;
            boolean hovered = inside(mouseX, mouseY, tabLeft, y - 2, tabWidth, LINE + 1);
            if (shown) {
                graphics.fill(tabLeft + 1, y - 2, tabLeft + tabWidth - 1, y + LINE - 1, Look.LCD_TEXT);
            }
            graphics.drawString(font, name, tabLeft + (tabWidth - font.width(name)) / 2, y, shown ? Look.LCD : hovered ? Look.LCD_TEXT : Look.LCD_DIM, false);
        }
        graphics.fill(x + PAD, y + LINE + 1, x + LCD_WIDTH - PAD, y + LINE + 2, Look.LCD_TRACK);
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
            int at = position(from, to, low, high, mark);
            graphics.fill(at, y - 2, at + 1, y + METER + 2, Look.LCD_DANGER);
        }
        int at = position(from, to, low, high, Mth.clamp(value, low, high));
        graphics.fill(at - 1, y - 2, at + 2, y + METER + 2, Look.LCD_TEXT);
    }

    private static int position(int from, int to, float low, float high, float value) {
        return from + Math.round((to - from) * (value - low) / (high - low));
    }

    private void warmth(GuiGraphics graphics, int x, int y, BodyView view) {
        y = row(graphics, x, y, FIRE, Component.translatable("messenger.backcountry.fire"),
                Component.translatable(view.fire() ? "messenger.backcountry.fire.near" : "messenger.backcountry.fire.none"), view.fire() ? Look.LCD_TEXT : Look.LCD_DIM);
        y += ROW - LINE;
        y = row(graphics, x, y, SHELTER, Component.translatable("messenger.backcountry.shelter"),
                Component.translatable(view.sheltered() ? "messenger.backcountry.shelter.roofed" : "messenger.backcountry.shelter.open"),
                view.sheltered() ? Look.LCD_TEXT : Look.LCD_DIM);
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
