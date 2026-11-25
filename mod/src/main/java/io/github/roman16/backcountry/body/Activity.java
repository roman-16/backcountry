package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import java.util.Locale;

public enum Activity {
    BOW_DRILL,
    BUILD,
    BUTCHER,
    CHOP,
    CLIMB,
    CRANK,
    DIG,
    FIGHT,
    MINE,
    RUN,
    SCRAPE,
    SPLIT,
    SWIM;

    public double met() {
        return Balance.get().met().get(name().toLowerCase(Locale.ROOT));
    }
}
