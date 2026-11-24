package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.survivor.Summary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

public final class ClientMessages {
    private ClientMessages() {}

    public static void view(BodyView view) {
        ClientState.view = view;
        ClientState.viewed = true;
        ClientState.blackout = view.blackout();
    }

    public static void summary(Summary summary) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (summary.kind()) {
            case YEAR -> ClientState.year = summary;
            case EXTRACTED -> {
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
