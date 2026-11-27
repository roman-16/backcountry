package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.guide.FieldNotes;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.survivor.Locator;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Weather;
import io.github.roman16.backcountry.weather.WeatherView;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;

public final class ClientMessages {
    private ClientMessages() {}

    public static void view(BodyView view, WeatherView weather, GuideView guide) {
        ClientState.received(view);
        if (Minecraft.getInstance().player != null) {
            Metabolism.body(Minecraft.getInstance().player).spirits = view.spirits();
        }
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

    public static void choose() {
        Minecraft.getInstance().setScreen(new KitScreen());
    }

    public static void map(int centreX, int centreZ, byte[] colours, List<Long> crosses) {
        Minecraft.getInstance().setScreen(new MapScreen(centreX, centreZ, colours, crosses));
    }

    public static void notes(List<FieldNotes.Note> notes) {
        ClientState.notes = notes.stream().collect(Collectors.toMap(FieldNotes.Note::id, note -> note, (a, b) -> a, TreeMap::new));
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
