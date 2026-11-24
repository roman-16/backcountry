package io.github.roman16.backcountry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class DevServer {
    private DevServer() {}

    @SubscribeEvent
    static void starting(ServerStartingEvent event) {
        if (!event.getServer().isDedicatedServer()) {
            return;
        }
        try {
            Path folder = Path.of("driver");
            Files.createDirectories(folder);
            Files.writeString(folder.resolve("server.pid"), Long.toString(ProcessHandle.current().pid()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
