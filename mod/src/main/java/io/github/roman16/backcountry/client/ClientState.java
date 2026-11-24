package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.survivor.Summary;
import org.jetbrains.annotations.Nullable;

public final class ClientState {
    public static BodyView view = BodyView.NONE;
    static boolean viewed;
    @Nullable
    static Summary ended;
    @Nullable
    static Summary year;
    static int blackout;

    private ClientState() {}

    static void clear() {
        view = BodyView.NONE;
        viewed = false;
        ended = null;
        year = null;
        blackout = 0;
    }
}
