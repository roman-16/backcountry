package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Condition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class Hud {
    private static final ResourceLocation ENERGY_FULL = Backcountry.id("hud/energy_full");
    private static final ResourceLocation ENERGY_HALF = Backcountry.id("hud/energy_half");
    private static final ResourceLocation ENERGY_EMPTY = Backcountry.id("hud/energy_empty");
    private static final ResourceLocation TEMPERATURE = Backcountry.id("hud/temperature");
    private static final ResourceLocation WET = Backcountry.id("hud/wet");
    private static final ResourceLocation WATER_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation WATER_PROGRESS = Backcountry.id("hud/water_bar_progress");
    private static final RandomSource SHAKE = RandomSource.create();
    private static final int HOTBAR_HALF = 91;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int ICON = 9;
    private static final int ICONS = 10;
    private static final int ICON_STEP = 8;
    private static final int WATER_FROM_BOTTOM = 35;
    private static final int WATER_ROOM = 6;
    private static final int LEVEL_LIFT = 8;
    private static final int ROW = 10;
    private static final int PANEL_GAP = 8;
    private static final int OFFHAND_ROOM = 29;
    private static final int PANEL_ROW = 12;
    private static final int PANEL_PAD = 2;
    private static final int LOW_BLINK_TICKS = 8;
    private static final int FADE_TICKS = 20;
    private static final int TICKS_A_SECOND = 20;
    private static final double SHOWN_WETNESS = 0.05;

    private Hud() {}

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, Backcountry.id("water"), Hud::water);
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, Hud::energy);
        event.wrapLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, level -> (graphics, delta) -> {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, shows() ? -LEVEL_LIFT : 0, 0);
            level.render(graphics, delta);
            pose.popPose();
        });
        event.registerAbove(VanillaGuiLayers.HOTBAR, Backcountry.id("body"), Hud::body);
        event.registerBelow(VanillaGuiLayers.CHAT, Backcountry.id("blackout"), Hud::blackout);
        event.registerBelowAll(Backcountry.id("whiteout"), Blizzard::whiteout);
    }

    static boolean shows() {
        Minecraft minecraft = Minecraft.getInstance();
        return ClientState.viewed && ClientState.ended == null && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer() && minecraft.player != null
                && !minecraft.options.hideGui;
    }

    private static void water(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int left = graphics.guiWidth() / 2 - HOTBAR_HALF;
        int top = graphics.guiHeight() - WATER_FROM_BOTTOM;
        float water = Mth.clamp(ClientState.view.water(), 0, 1);
        graphics.blitSprite(WATER_BACKGROUND, left, top, BAR_WIDTH, BAR_HEIGHT);
        boolean low = water < Balance.get().healing().above();
        int filled = Math.round(BAR_WIDTH * water);
        if (filled > 0 && !(low && minecraft.gui.getGuiTicks() / LOW_BLINK_TICKS % 2 == 0)) {
            graphics.blitSprite(WATER_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, left, top, filled, BAR_HEIGHT);
        }
        minecraft.gui.leftHeight += WATER_ROOM;
        minecraft.gui.rightHeight += WATER_ROOM;
    }

    private static void energy(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int right = graphics.guiWidth() / 2 + HOTBAR_HALF;
        int top = graphics.guiHeight() - minecraft.gui.rightHeight;
        float energy = Mth.clamp(ClientState.view.energy(), 0, 1);
        int halves = Math.round(energy * ICONS * 2);
        boolean tired = energy < Balance.get().energy().sprintBelow();
        SHAKE.setSeed(minecraft.gui.getGuiTicks() * 312871L);
        for (int i = 0; i < ICONS; i++) {
            int x = right - i * ICON_STEP - ICON;
            int y = top + (tired && minecraft.gui.getGuiTicks() % (halves * 3 + 1) == 0 ? SHAKE.nextInt(3) - 1 : 0);
            graphics.blitSprite(ENERGY_EMPTY, x, y, ICON, ICON);
            if (i * 2 + 1 < halves) {
                graphics.blitSprite(ENERGY_FULL, x, y, ICON, ICON);
            } else if (i * 2 + 1 == halves) {
                graphics.blitSprite(ENERGY_HALF, x, y, ICON, ICON);
            }
        }
        minecraft.gui.rightHeight += ROW;
    }

    private record Line(ResourceLocation icon, Component text, int colour) {}

    private static void body(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        BodyView view = ClientState.view;
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(TEMPERATURE, Component.translatable("hud.backcountry.core", Words.one(view.coreC()), Words.comfort(view.comfort())), coreColour(view.coreC())));
        if (view.wetness() >= SHOWN_WETNESS) {
            lines.add(new Line(WET, Component.translatable("hud.backcountry.wet", Words.percent(view.wetness())), Look.TEXT));
        }
        for (Condition condition : view.conditions()) {
            lines.add(new Line(condition.icon(), condition.title(), condition == Condition.SEVERE_FROSTBITE ? Look.DANGER : Look.TEXT));
        }
        boolean offhandRight = minecraft.player.getMainArm() == HumanoidArm.LEFT && !minecraft.player.getOffhandItem().isEmpty();
        int x = graphics.guiWidth() / 2 + HOTBAR_HALF + PANEL_GAP + (offhandRight ? OFFHAND_ROOM : 0);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int y = graphics.guiHeight() - ICON - PANEL_PAD - 1 - i * PANEL_ROW;
            int width = ICON + PANEL_PAD + font.width(line.text());
            graphics.fill(x - PANEL_PAD, y - PANEL_PAD, x + width + PANEL_PAD, y + ICON + PANEL_PAD - 1, Look.PLATE);
            graphics.blitSprite(line.icon(), x, y, ICON, ICON);
            graphics.drawString(font, line.text(), x + ICON + PANEL_PAD, y + 1, line.colour(), true);
        }
    }

    private static int coreColour(float core) {
        Balance.Warmth warmth = Balance.get().warmth();
        if (core < Balance.get().conditions().frostCoreC()) {
            return Look.FREEZING;
        }
        if (core < warmth.shiverCoreC()) {
            return Look.COLD;
        }
        return core > warmth.sweatCoreC() ? Look.HOT : Look.TEXT;
    }

    private static void blackout(GuiGraphics graphics, DeltaTracker delta) {
        if (ClientState.blackout <= 0 || !shows()) {
            return;
        }
        int total = Balance.get().energy().blackoutSeconds() * TICKS_A_SECOND;
        int passed = total - ClientState.blackout;
        float alpha = Math.min(1, Math.min(passed, ClientState.blackout) / (float) FADE_TICKS);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (Math.round(alpha * 255) << 24) | Look.BLACKOUT);
    }
}
