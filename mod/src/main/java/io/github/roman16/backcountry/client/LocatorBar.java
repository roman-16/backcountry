package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.survivor.Locator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class LocatorBar {
    private static final ResourceLocation BAR = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation DOT = Backcountry.id("hud/locator_dot");
    private static final ResourceLocation DOT_EAST = Backcountry.id("hud/locator_dot_east");
    private static final ResourceLocation DOT_WEST = Backcountry.id("hud/locator_dot_west");
    private static final ResourceLocation UP = Backcountry.id("hud/locator_arrow_up");
    private static final ResourceLocation DOWN = Backcountry.id("hud/locator_arrow_down");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    private static final int ICON = 9;
    private static final int TOP = 2;
    private static final int BAR_DROP = 6;
    private static final int DOT_RISE = 2;
    private static final int ARROW_STEP = 6;
    private static final int NAME_DROP = 18;
    private static final int LINE = 10;
    private static final int TRACKER_GAP = 4;
    private static final int METRES_A_KM = 1000;
    private static boolean shown;
    private static List<Locator.Mark> marks = List.of();
    private static int bottom;

    private LocatorBar() {}

    static void received(boolean shownNow, List<Locator.Mark> marksNow) {
        shown = shownNow;
        marks = marksNow;
    }

    public static int bottom() {
        return bottom;
    }

    public static int colour(UUID id) {
        return Look.TEAM[Math.floorMod(id.hashCode(), Look.TEAM.length)];
    }

    private record Placed(Locator.Mark mark, double relative, double dy, double distance) {}

    static void render(GuiGraphics graphics, DeltaTracker delta) {
        bottom = 0;
        Minecraft minecraft = Minecraft.getInstance();
        if (!shown || !Hud.shows() || minecraft.getDebugOverlay().showDebugScreen() || minecraft.level == null) {
            return;
        }
        Balance.Locator locator = Balance.get().locator();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        float yaw = camera.getYRot();
        float partial = delta.getGameTimeDeltaPartialTick(true);
        Vec3 from = camera.getEntity().getPosition(partial);
        int left = (graphics.guiWidth() - WIDTH) / 2;
        int top = TaskTracker.right() + TRACKER_GAP > left ? TaskTracker.bottom() + TRACKER_GAP : TOP;
        int barY = top + BAR_DROP;
        graphics.blitSprite(BAR, left, barY, WIDTH, HEIGHT);
        double half = locator.spanDegrees() / 2;
        double reach = WIDTH / 2.0 - ICON / 2.0;
        Placed named = null;
        RenderSystem.enableBlend();
        for (Locator.Mark mark : marks) {
            Player loaded = minecraft.level.getPlayerByUUID(mark.id());
            Vec3 at = loaded == null ? new Vec3(mark.x(), mark.y(), mark.z()) : loaded.getPosition(partial);
            Vec3 to = at.subtract(from);
            double relative = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-to.x, to.z)) - yaw);
            Placed placed = new Placed(mark, relative, to.y, to.length());
            if (Math.abs(relative) <= locator.nameDegrees() && (named == null || Math.abs(relative) < Math.abs(named.relative()))) {
                named = placed;
            }
            draw(graphics, placed, left, barY, half, reach, locator);
        }
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        bottom = barY + NAME_DROP - BAR_DROP;
        if (named != null) {
            Font font = minecraft.font;
            Component line = Component.translatable("hud.backcountry.locator", named.mark().name(), distance(named.distance()));
            graphics.drawCenteredString(font, line, graphics.guiWidth() / 2, barY + NAME_DROP - BAR_DROP + 2, Look.TEXT);
            bottom += LINE;
        }
    }

    private static void draw(GuiGraphics graphics, Placed placed, int left, int barY, double half, double reach, Balance.Locator locator) {
        boolean behind = Math.abs(placed.relative()) > half;
        double share = Mth.clamp(placed.relative() / half, -1, 1);
        int centre = left + WIDTH / 2 + (int) Math.round(share * reach);
        int x = centre - ICON / 2;
        int y = barY - DOT_RISE;
        int colour = colour(placed.mark().id());
        double fade = Mth.clamp((placed.distance() - locator.fullBlocks()) / (locator.faintBlocks() - locator.fullBlocks()), 0, 1);
        float alpha = (float) (1 - fade * (1 - locator.faintShare()));
        graphics.setColor((colour >> 16 & 0xFF) / 255F, (colour >> 8 & 0xFF) / 255F, (colour & 0xFF) / 255F, alpha);
        graphics.blitSprite(behind ? share < 0 ? DOT_EAST : DOT_WEST : DOT, x, y, ICON, ICON);
        if (placed.dy() >= locator.elevationBlocks()) {
            graphics.blitSprite(UP, x, y - ARROW_STEP, ICON, ICON);
        } else if (placed.dy() <= -locator.elevationBlocks()) {
            graphics.blitSprite(DOWN, x, y + ARROW_STEP, ICON, ICON);
        }
    }

    private static Component distance(double blocks) {
        return blocks >= METRES_A_KM ? Component.translatable("hud.backcountry.kilometres", Words.one(blocks / METRES_A_KM))
                : Component.translatable("hud.backcountry.metres", Math.round(blocks));
    }
}
