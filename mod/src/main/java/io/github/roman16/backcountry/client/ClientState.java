package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.weather.Weather;
import io.github.roman16.backcountry.weather.WeatherView;
import org.jetbrains.annotations.Nullable;

public final class ClientState {
    public static BodyView view = BodyView.NONE;
    public static WeatherView weather = WeatherView.NONE;
    static boolean viewed;
    @Nullable
    static Summary ended;
    @Nullable
    static Summary year;
    static int blackout;
    private static boolean readSent;

    private ClientState() {}

    static boolean unread() {
        return view.unread() && !readSent;
    }

    static void read() {
        readSent = true;
    }

    static void received(BodyView received) {
        view = received;
        if (!received.unread()) {
            readSent = false;
        }
    }

    static void clear() {
        view = BodyView.NONE;
        weather = WeatherView.NONE;
        Weather.unshare();
        viewed = false;
        ended = null;
        year = null;
        blackout = 0;
        readSent = false;
    }
}
