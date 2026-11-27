package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.gear.MapSheets;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.material.MapColor;

public final class MapScreen extends Screen {
    private static final ResourceLocation SHEET = Backcountry.id("map_sheet");
    private static final int BARK = 0xFFE8DCC0;
    private static final int BARK_EDGE = 0xFF9A8A6A;
    private static final int CHARCOAL = 0xFF2A2622;
    private static final int MARGIN = 32;
    private static final int BORDER = 4;
    private static final float INK = 0.625F;
    private final int centreX;
    private final int centreZ;
    private final byte[] colours;
    private final List<Long> crosses;
    private DynamicTexture texture;

    public MapScreen(int centreX, int centreZ, byte[] colours, List<Long> crosses) {
        super(Component.translatable("item.backcountry.bark_map"));
        this.centreX = centreX;
        this.centreZ = centreZ;
        this.colours = colours;
        this.crosses = crosses;
    }

    @Override
    protected void init() {
        if (texture == null && colours.length == MapSheets.SIZE * MapSheets.SIZE) {
            texture = new DynamicTexture(MapSheets.SIZE, MapSheets.SIZE, false);
            NativeImage pixels = texture.getPixels();
            for (int i = 0; i < colours.length; i++) {
                pixels.setPixelRGBA(i % MapSheets.SIZE, i / MapSheets.SIZE, colours[i] == 0 ? abgr(BARK) : inked(MapColor.getColorFromPackedId(colours[i] & 0xFF)));
            }
            texture.upload();
            minecraft.getTextureManager().register(SHEET, texture);
        }
    }

    private static int abgr(int argb) {
        return argb & 0xFF00FF00 | (argb & 0xFF) << 16 | argb >> 16 & 0xFF;
    }

    private static int inked(int abgr) {
        int bark = abgr(BARK);
        int mixed = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int paper = bark >> shift & 0xFF;
            int ink = abgr >> shift & 0xFF;
            mixed |= Math.round(paper + (ink - paper) * INK) << shift;
        }
        return mixed;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int scale = Math.max(1, (Math.min(width, height) - 2 * MARGIN) / MapSheets.SIZE);
        int size = MapSheets.SIZE * scale;
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        graphics.fill(left - BORDER, top - BORDER, left + size + BORDER, top + size + BORDER, BARK_EDGE);
        graphics.fill(left, top, left + size, top + size, BARK);
        if (texture == null) {
            int y = top + size / 2 - font.lineHeight * 2;
            for (FormattedCharSequence line : font.split(Component.translatable("map.backcountry.blank"), size - 2 * BORDER)) {
                graphics.drawString(font, line, left + (size - font.width(line)) / 2, y, CHARCOAL, false);
                y += font.lineHeight + 1;
            }
            return;
        }
        graphics.blit(SHEET, left, top, 0, 0, size, size, size, size);
        int step = Balance.get().survey().blocksAPixel();
        for (long cross : crosses) {
            BlockPos at = BlockPos.of(cross);
            int x = left + Math.round((at.getX() - centreX + MapSheets.SIZE / 2F * step) / step * scale);
            int y = top + Math.round((at.getZ() - centreZ + MapSheets.SIZE / 2F * step) / step * scale);
            graphics.drawString(font, "×", x - font.width("×") / 2, y - font.lineHeight / 2, CHARCOAL, false);
        }
        graphics.drawString(font, "N", left + size - font.width("N") - BORDER, top + BORDER, CHARCOAL, false);
        Component caption = crosses.size() == 1 ? Component.translatable("map.backcountry.caption.one", step)
                : Component.translatable("map.backcountry.caption", crosses.size(), step);
        graphics.drawString(font, caption, (width - font.width(caption)) / 2, top - BORDER - font.lineHeight - 2, 0xFFFFFFFF, true);
    }

    @Override
    public void removed() {
        if (texture != null) {
            minecraft.getTextureManager().release(SHEET);
            texture = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
