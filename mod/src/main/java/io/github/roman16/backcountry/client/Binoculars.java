package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.BinocularsItem;
import io.github.roman16.backcountry.gear.Hotbar;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class Binoculars {
    public static final KeyMapping KEY = new KeyMapping(BinocularsItem.KEY, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C,
            "key.categories." + Backcountry.MODID);
    private static final ResourceLocation VIEW = Backcountry.id("textures/misc/binoculars.png");
    private static final int VIEW_WIDTH = 112;
    private static final int VIEW_HEIGHT = 64;
    private static final int VIEW_DEPTH = -90;
    private static final float ITEM_IN_USE_SPEED = 0.2F;
    private static boolean looking;
    private static CameraType before;

    private Binoculars() {}

    public static boolean looking() {
        return looking;
    }

    public static float fovModifier() {
        return (float) (1 / Balance.get().binoculars().magnification());
    }

    public static void view(GuiGraphics graphics, float scale) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int height = Mth.floor(Math.min(screenHeight, (float) screenWidth * VIEW_HEIGHT / VIEW_WIDTH) * scale);
        int width = height * VIEW_WIDTH / VIEW_HEIGHT;
        int left = (screenWidth - width) / 2;
        int top = (screenHeight - height) / 2;
        int right = left + width;
        int bottom = top + height;
        RenderSystem.enableBlend();
        graphics.blit(VIEW, left, top, VIEW_DEPTH, 0, 0, width, height, width, height);
        RenderSystem.disableBlend();
        graphics.fill(RenderType.guiOverlay(), 0, 0, screenWidth, top, VIEW_DEPTH, Look.VIEW_DARK);
        graphics.fill(RenderType.guiOverlay(), 0, bottom, screenWidth, screenHeight, VIEW_DEPTH, Look.VIEW_DARK);
        graphics.fill(RenderType.guiOverlay(), 0, top, left, bottom, VIEW_DEPTH, Look.VIEW_DARK);
        graphics.fill(RenderType.guiOverlay(), right, top, screenWidth, bottom, VIEW_DEPTH, Look.VIEW_DARK);
    }

    private static boolean carried(Player player) {
        if (Metabolism.body(player).binoculars.getItem() instanceof BinocularsItem || player.getOffhandItem().getItem() instanceof BinocularsItem) {
            return true;
        }
        for (int slot = 0; slot < Hotbar.ROW; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BinocularsItem) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        looking = player != null && minecraft.screen == null && KEY.isDown() && !player.isUsingItem() && carried(player);
        boolean scoping = player != null && player.isScoping();
        if (scoping && before == null) {
            before = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (!scoping && before != null) {
            minecraft.options.setCameraType(before);
            before = null;
        }
    }

    @SubscribeEvent
    static void slow(MovementInputUpdateEvent event) {
        if (looking && !event.getEntity().isPassenger()) {
            Input input = event.getInput();
            input.leftImpulse *= ITEM_IN_USE_SPEED;
            input.forwardImpulse *= ITEM_IN_USE_SPEED;
            event.getEntity().setSprinting(false);
        }
    }

    @SubscribeEvent
    static void crosshair(RenderGuiLayerEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && player != null && player.isScoping()) {
            event.setCanceled(true);
        }
    }
}
