package io.github.roman16.backcountry.client;

import com.mojang.logging.LogUtils;
import io.github.roman16.backcountry.Backcountry;
import java.util.Optional;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class HeldLight {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD = "lambdynlights";
    private static String mode = "";

    private HeldLight() {}

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (mode.isEmpty()) {
            follow();
        }
    }

    @SubscribeEvent
    static void closing(ScreenEvent.Closing event) {
        follow();
    }

    static void follow() {
        if (!ModList.get().isLoaded(MOD)) {
            return;
        }
        String wanted = Graphics.heldLight();
        if (!wanted.equals(mode)) {
            set(wanted);
            mode = wanted;
        }
    }

    private static void set(String wanted) {
        try {
            Class<?> lights = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            Class<?> modes = Class.forName("dev.lambdaurora.lambdynlights.DynamicLightsMode");
            Object config = lights.getField("config").get(lights.getMethod("get").invoke(null));
            Object value = ((Optional<?>) modes.getMethod("byId", String.class).invoke(null, wanted)).orElseThrow();
            config.getClass().getMethod("setDynamicLightsMode", modes).invoke(config, value);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("held light failed: LambDynamicLights refused the mode {}", wanted, exception);
        }
    }
}
