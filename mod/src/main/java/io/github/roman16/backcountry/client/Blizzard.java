package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Weather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class Blizzard {
    private static final String FAR_VIEW = "distanthorizons";
    private static final float TICKS_TO_CLOSE = 20;
    private static final float RED_SHARE = 0.299F;
    private static final float GREEN_SHARE = 0.587F;
    private static final float BLUE_SHARE = 0.114F;
    private static final float CHANNEL = 255;
    private static float strength;

    private Blizzard() {}

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded(FAR_VIEW)) {
            FarFog.bind();
        }
    }

    static float strength() {
        return strength;
    }

    private static boolean raging(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || !level.isRaining()) {
            return false;
        }
        Day today = Weather.day(level, Calendar.day(level));
        if (!today.falling(Climate.hour(level)) || today.windKmh() < Balance.get().weather().blizzardWindKmh()) {
            return false;
        }
        BlockPos camera = minecraft.gameRenderer.getMainCamera().getBlockPosition();
        return Climate.snows(level, level.getBiome(camera).value(), camera) && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, camera).getY() <= camera.getY();
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        float target = raging(Minecraft.getInstance()) ? 1 : 0;
        strength = Mth.approach(strength, target, 1 / TICKS_TO_CLOSE);
    }

    @SubscribeEvent
    static void fog(ViewportEvent.RenderFog event) {
        if (strength <= 0 || event.getType() != FogType.NONE) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(strength, event.getFarPlaneDistance(), Balance.get().weather().blizzardFogBlocks()));
        event.setNearPlaneDistance(Mth.lerp(strength, event.getNearPlaneDistance(), 0));
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void colour(ViewportEvent.ComputeFogColor event) {
        if (strength <= 0) {
            return;
        }
        float red = (Look.BLIZZARD >> 16 & 0xFF) / CHANNEL;
        float green = (Look.BLIZZARD >> 8 & 0xFF) / CHANNEL;
        float blue = (Look.BLIZZARD & 0xFF) / CHANNEL;
        float light = luminance(event.getRed(), event.getGreen(), event.getBlue()) / luminance(red, green, blue);
        event.setRed(Mth.lerp(strength, event.getRed(), Math.min(1, red * light)));
        event.setGreen(Mth.lerp(strength, event.getGreen(), Math.min(1, green * light)));
        event.setBlue(Mth.lerp(strength, event.getBlue(), Math.min(1, blue * light)));
    }

    private static float luminance(float red, float green, float blue) {
        return RED_SHARE * red + GREEN_SHARE * green + BLUE_SHARE * blue;
    }
}
