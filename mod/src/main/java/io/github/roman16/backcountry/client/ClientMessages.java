package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.survivor.Locator;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Weather;
import io.github.roman16.backcountry.weather.WeatherView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.DeathScreen;

public final class ClientMessages {
    private ClientMessages() {}

    public static void view(BodyView view, WeatherView weather, GuideView guide) {
        ClientState.received(view);
        if (ClientState.viewed) {
            Tasks.compare(ClientState.guide, guide);
        }
        ClientState.guide = guide;
        ClientState.weather = weather;
        ClientState.viewed = true;
        ClientState.blackout = view.blackout();
    }

    public static void bowDrill(BlockPos fire, boolean wet) {
        Minecraft.getInstance().setScreen(new BowDrillScreen(fire, wet));
    }

    public static void marks(boolean shown, List<Locator.Mark> marks) {
        LocatorBar.received(shown, marks);
    }

    public static void sky(List<Day> days) {
        Weather.share(days);
    }

    public static void summary(Summary summary) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (summary.kind()) {
            case YEAR -> ClientState.year = summary;
            case EXTRACTED, TAPPED_OUT -> {
                ClientState.ended = summary;
                minecraft.setScreen(new SummaryScreen(summary));
            }
            default -> {
                ClientState.ended = summary;
                if (minecraft.screen instanceof DeathScreen || minecraft.player != null && minecraft.player.isDeadOrDying()) {
                    minecraft.setScreen(new SummaryScreen(summary));
                }
            }
        }
    }
}
