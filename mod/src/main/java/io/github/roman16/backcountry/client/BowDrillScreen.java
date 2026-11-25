package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.net.Messages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class BowDrillScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("popup/background");
    private static final int WIDTH = 256;
    private static final int HEIGHT = 64;
    private static final int PAD = 10;
    private static final int LINE = 12;
    private static final int BAR_HEIGHT = 6;
    private static final int LABEL_WIDTH = 44;
    private static final double TICKS_A_SECOND = 20;
    private final BlockPos fire;
    private final double cooling;
    private Stage stage = Stage.DRILL;
    private Side last;
    private double friction;
    private double glow;
    private double flame;
    private int strokes;
    private Component setback;
    private boolean lit;

    private enum Stage {
        DRILL,
        BLOW
    }

    private enum Side {
        LEFT,
        RIGHT
    }

    public BowDrillScreen(BlockPos fire, boolean wet) {
        super(Component.translatable("bow_drill.backcountry.title"));
        this.fire = fire;
        this.cooling = wet ? Balance.get().bowDrill().wetCooling() : 1;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (stage == Stage.DRILL && (key == GLFW.GLFW_KEY_LEFT || minecraft.options.keyLeft.matches(key, scanCode))) {
            stroke(Side.LEFT);
            return true;
        }
        if (stage == Stage.DRILL && (key == GLFW.GLFW_KEY_RIGHT || minecraft.options.keyRight.matches(key, scanCode))) {
            stroke(Side.RIGHT);
            return true;
        }
        if (stage == Stage.BLOW && (key == GLFW.GLFW_KEY_SPACE || minecraft.options.keyJump.matches(key, scanCode))) {
            blow();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void stroke(Side side) {
        if (side == last) {
            return;
        }
        last = side;
        strokes++;
        friction = Math.min(1, friction + Balance.get().bowDrill().strokeHeat() / Skill.effort(ClientState.view.skills().get(Skill.FIRE_MAKING.ordinal())));
        if (friction >= 1) {
            stage = Stage.BLOW;
            glow = Balance.get().bowDrill().glowBand();
            flame = 0;
            setback = null;
        }
    }

    private void blow() {
        glow += Balance.get().bowDrill().blowGlow();
        if (glow > 1) {
            restart(Component.translatable("bow_drill.backcountry.scattered"));
        }
    }

    private void restart(Component why) {
        stage = Stage.DRILL;
        friction = 0;
        last = null;
        setback = why;
    }

    @Override
    public void tick() {
        Balance.BowDrill drill = Balance.get().bowDrill();
        if (stage == Stage.DRILL) {
            friction = Math.max(0, friction - drill.coolingPerSecond() * cooling / TICKS_A_SECOND);
            return;
        }
        glow -= drill.glowLossPerSecond() * cooling / TICKS_A_SECOND;
        if (glow <= 0) {
            restart(Component.translatable("bow_drill.backcountry.died"));
            return;
        }
        if (glow >= drill.glowBand()) {
            flame += drill.flamePerSecond() / TICKS_A_SECOND;
        }
        if (flame >= 1) {
            lit = true;
            onClose();
        }
    }

    @Override
    public void removed() {
        if (minecraft.getConnection() != null) {
            Messages.drilled(fire, strokes, lit);
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        graphics.blitSprite(PANEL, left, top, WIDTH, HEIGHT);
        int x = left + PAD;
        int y = top + PAD;
        graphics.drawString(font, title, x, y, Look.TEXT, false);
        y += LINE + 4;
        int barLeft = x + LABEL_WIDTH;
        int barRight = left + WIDTH - PAD;
        if (stage == Stage.DRILL) {
            graphics.drawString(font, Component.translatable("bow_drill.backcountry.friction"), x, y, Look.TEXT, false);
            bar(graphics, barLeft, barRight, y + 1, friction, Look.HOT);
            y += LINE;
            graphics.drawString(font, Component.translatable("bow_drill.backcountry.saw"), x, y, Look.TEXT_QUIET, false);
        } else {
            int middle = (barLeft + barRight) / 2;
            graphics.drawString(font, Component.translatable("bow_drill.backcountry.glow"), x, y, Look.TEXT, false);
            int bandLeft = barLeft + (int) Math.round((middle - 4 - barLeft) * Balance.get().bowDrill().glowBand());
            graphics.fill(barLeft, y + 1, middle - 4, y + 1 + BAR_HEIGHT, Look.TRACK);
            graphics.fill(bandLeft, y + 1, middle - 4, y + 1 + BAR_HEIGHT, Look.BAND);
            graphics.fill(barLeft, y + 2, barLeft + (int) Math.round((middle - 4 - barLeft) * Math.min(1, glow)), y + BAR_HEIGHT, Look.HOT);
            graphics.drawString(font, Component.translatable("bow_drill.backcountry.flame"), middle + 4, y, Look.TEXT, false);
            int flameLeft = middle + 4 + font.width(Component.translatable("bow_drill.backcountry.flame")) + 4;
            bar(graphics, flameLeft, barRight, y + 1, flame, Look.LIGHT);
            y += LINE;
            graphics.drawString(font, Component.translatable("bow_drill.backcountry.blow"), x, y, Look.TEXT_QUIET, false);
        }
        if (setback != null) {
            graphics.drawString(font, setback, x, y + LINE, Look.DANGER, false);
        }
    }

    private static void bar(GuiGraphics graphics, int left, int right, int top, double share, int colour) {
        graphics.fill(left, top, right, top + BAR_HEIGHT, Look.TRACK);
        graphics.fill(left, top, left + (int) Math.round((right - left) * Math.min(1, share)), top + BAR_HEIGHT, colour);
    }
}
