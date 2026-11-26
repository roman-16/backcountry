package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Condition;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class Hud {
    private static final ResourceLocation ENERGY_FULL = Backcountry.id("hud/energy_full");
    private static final ResourceLocation ENERGY_HALF = Backcountry.id("hud/energy_half");
    private static final ResourceLocation ENERGY_EMPTY = Backcountry.id("hud/energy_empty");
    private static final ResourceLocation WATER_FULL = Backcountry.id("hud/water_full");
    private static final ResourceLocation WATER_HALF = Backcountry.id("hud/water_half");
    private static final ResourceLocation WATER_EMPTY = Backcountry.id("hud/water_empty");
    private static final ResourceLocation TEMPERATURE = Backcountry.id("hud/temperature");
    private static final ResourceLocation WET = Backcountry.id("hud/wet");
    private static final ResourceLocation FROST = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static final RandomSource SHAKE = RandomSource.create();
    private static final int HOTBAR_HALF = 91;
    private static final int ICON = 9;
    private static final int ICONS = 10;
    private static final int ICON_STEP = 8;
    private static final int ROW = 10;
    private static final int XP_ROW = 7;
    private static final int PANEL_GAP = 8;
    private static final int OFFHAND_ROOM = 29;
    private static final int PANEL_ROW = 12;
    private static final int PANEL_PAD = 2;
    private static final int FADE_TICKS = 20;
    private static final int TICKS_A_SECOND = 20;
    private static final double SHOWN_WETNESS = 0.05;
    private static final List<Tip> TIPS = new ArrayList<>();
    private static int healthBottom;

    private Hud() {}

    private record Tip(int x, int y, int width, int height, Supplier<List<Component>> lines) {
        boolean inside(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Backcountry.id("frost"), Hud::frost);
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_BAR, Hud::closeXpRow);
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, (graphics, delta) -> {});
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Backcountry.id("health"), Hud::health);
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, Hud::energy);
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, Backcountry.id("water"), Hud::water);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Backcountry.id("body"), Hud::body);
        event.registerBelow(VanillaGuiLayers.CHAT, Backcountry.id("tasks"), TaskTracker::render);
        event.registerBelow(VanillaGuiLayers.CHAT, Backcountry.id("blackout"), Hud::blackout);
        event.registerBelowAll(Backcountry.id("whiteout"), Blizzard::whiteout);
    }

    static boolean shows() {
        Minecraft minecraft = Minecraft.getInstance();
        return ClientState.viewed && ClientState.ended == null && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer() && minecraft.player != null
                && !minecraft.options.hideGui;
    }

    static float frostShare(float coreC) {
        Balance balance = Balance.get();
        double shiver = balance.warmth().shiverCoreC();
        return (float) Mth.clamp((shiver - coreC) / (shiver - balance.conditions().frostCoreC()), 0, 1);
    }

    private static void frost(GuiGraphics graphics, DeltaTracker delta) {
        TIPS.clear();
        if (!shows()) {
            return;
        }
        float vanilla = Minecraft.getInstance().player.getPercentFrozen();
        float wanted = frostShare(ClientState.view.coreC());
        if (wanted <= vanilla) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, (wanted - vanilla) / (1 - vanilla));
        graphics.blit(FROST, 0, 0, -90, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void closeXpRow(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null && minecraft.gameMode.hasExperience()) {
            minecraft.gui.leftHeight -= XP_ROW;
            minecraft.gui.rightHeight -= XP_ROW;
        }
        healthBottom = graphics.guiHeight() - minecraft.gui.leftHeight + ICON;
    }

    private static void health(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int top = graphics.guiHeight() - minecraft.gui.leftHeight + ROW;
        tip(graphics.guiWidth() / 2 - HOTBAR_HALF, top, ICONS * ICON_STEP + 1, healthBottom - top, () -> {
            float hearts = minecraft.player.getHealth() / 2;
            String shown = hearts == Math.round(hearts) ? String.valueOf(Math.round(hearts)) : Words.one(hearts);
            return List.of(Component.translatable("hud.backcountry.hover.health", shown, Math.round(minecraft.player.getMaxHealth() / 2)),
                    Component.translatable("hud.backcountry.hover.health.help").withStyle(ChatFormatting.GRAY));
        });
    }

    private static void energy(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        float energy = Mth.clamp(ClientState.view.energy(), 0, 1);
        double sprintBelow = Balance.get().energy().sprintBelow();
        icons(graphics, energy, energy < sprintBelow, ENERGY_FULL, ENERGY_HALF, ENERGY_EMPTY, () -> List.of(
                Component.translatable("hud.backcountry.hover.energy", Words.percent(ClientState.view.energy())),
                Component.translatable("hud.backcountry.hover.energy.help").withStyle(ChatFormatting.GRAY),
                Component.translatable("hud.backcountry.hover.energy.sprint", Words.percent(sprintBelow)).withStyle(ChatFormatting.GRAY)));
    }

    private static void water(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        float water = Mth.clamp(ClientState.view.water(), 0, 1);
        icons(graphics, water, water < Balance.get().healing().above(), WATER_FULL, WATER_HALF, WATER_EMPTY, () -> List.of(
                Component.translatable("hud.backcountry.hover.water", Words.percent(ClientState.view.water())),
                Component.translatable("messenger.backcountry.help.water").withStyle(ChatFormatting.GRAY)));
    }

    private static void icons(GuiGraphics graphics, float share, boolean low, ResourceLocation full, ResourceLocation half, ResourceLocation empty, Supplier<List<Component>> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        int right = graphics.guiWidth() / 2 + HOTBAR_HALF;
        int top = graphics.guiHeight() - minecraft.gui.rightHeight;
        int halves = Math.round(share * ICONS * 2);
        SHAKE.setSeed(minecraft.gui.getGuiTicks() * 312871L);
        for (int i = 0; i < ICONS; i++) {
            int x = right - i * ICON_STEP - ICON;
            int y = top + (low && minecraft.gui.getGuiTicks() % (halves * 3 + 1) == 0 ? SHAKE.nextInt(3) - 1 : 0);
            graphics.blitSprite(empty, x, y, ICON, ICON);
            if (i * 2 + 1 < halves) {
                graphics.blitSprite(full, x, y, ICON, ICON);
            } else if (i * 2 + 1 == halves) {
                graphics.blitSprite(half, x, y, ICON, ICON);
            }
        }
        tip(right - (ICONS - 1) * ICON_STEP - ICON, top, (ICONS - 1) * ICON_STEP + ICON, ICON, lines);
        minecraft.gui.rightHeight += ROW;
    }

    private record Line(ResourceLocation icon, Component text, int colour, Supplier<List<Component>> tip) {}

    private static void body(GuiGraphics graphics, DeltaTracker delta) {
        if (!shows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        BodyView view = ClientState.view;
        List<Line> lines = new ArrayList<>();
        Component core = Component.translatable("hud.backcountry.core", Words.one(view.coreC()), Words.comfort(view.comfort()));
        lines.add(new Line(TEMPERATURE, core, coreColour(view.coreC()), () -> {
            List<Component> tip = new ArrayList<>(List.of(core, Component.translatable("hud.backcountry.hover.body").withStyle(ChatFormatting.GRAY)));
            if (ClientState.view.coreC() < Balance.get().warmth().shiverCoreC()) {
                tip.add(Component.translatable("messenger.backcountry.help.cold").withStyle(ChatFormatting.GRAY));
            }
            return tip;
        }));
        if (view.wetness() >= SHOWN_WETNESS) {
            Component wet = Component.translatable("hud.backcountry.wet", Words.percent(view.wetness()));
            lines.add(new Line(WET, wet, Look.TEXT, () -> List.of(wet, Component.translatable("messenger.backcountry.help.wet").withStyle(ChatFormatting.GRAY))));
        }
        for (Condition condition : view.conditions()) {
            lines.add(new Line(condition.icon(), condition.title(), condition == Condition.SEVERE_FROSTBITE ? Look.DANGER : Look.TEXT, () -> List.of(condition.title(),
                    Component.translatable("hud.backcountry.hover.cure", Component.translatable("condition.backcountry." + condition.id() + ".cure"))
                            .withStyle(ChatFormatting.GRAY))));
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
            tip(x - PANEL_PAD, y - PANEL_PAD, width + 2 * PANEL_PAD, ICON + 2 * PANEL_PAD - 1, line.tip());
        }
    }

    private static void tip(int x, int y, int width, int height, Supplier<List<Component>> lines) {
        TIPS.add(new Tip(x, y, width, height, lines));
    }

    @SubscribeEvent
    static void hover(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !shows() || !screen.getMenu().getCarried().isEmpty()) {
            return;
        }
        for (Tip tip : TIPS) {
            if (tip.inside(event.getMouseX(), event.getMouseY())) {
                event.getGuiGraphics().renderComponentTooltip(Minecraft.getInstance().font, tip.lines().get(), event.getMouseX(), event.getMouseY());
                return;
            }
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
