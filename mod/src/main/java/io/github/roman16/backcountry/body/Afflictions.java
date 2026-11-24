package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

public final class Afflictions {
    private static final double HOURS_A_DAY = 24;
    private static final float DRAIN = 1;

    private Afflictions() {}

    public static void advance(ServerPlayer player, Body body, double days, double hours) {
        Balance balance = Balance.get();
        Balance.Conditions rules = balance.conditions();
        Iterator<Map.Entry<Condition, Body.Affliction>> afflictions = body.conditions.entrySet().iterator();
        int drains = 0;
        while (afflictions.hasNext()) {
            Map.Entry<Condition, Body.Affliction> entry = afflictions.next();
            Body.Affliction affliction = entry.getValue();
            double before = affliction.days;
            boolean ends = switch (entry.getKey()) {
                case INFECTED_WOUND -> {
                    affliction.days += days;
                    if (!affliction.bandaged && affliction.days > rules.infectionDays()) {
                        drains += wholeHours(Math.max(before, rules.infectionDays()), affliction.days);
                    }
                    yield affliction.bandaged && affliction.days >= rules.infectionDays();
                }
                case BLISTERS -> {
                    affliction.days = body.wetness < rules.dryWetness() ? affliction.days + days : 0;
                    yield affliction.days >= 1;
                }
                case BURNS -> {
                    affliction.days += affliction.bandaged ? 2 * days : days;
                    yield affliction.days >= rules.burnsDays();
                }
                case GUT_INFECTION -> (affliction.days += days) >= rules.gutDays();
                case FOOD_POISONING -> (affliction.days += days) >= rules.poisoningDays();
                case SCURVY -> body.vitaminCMg >= balance.body().vitaminCMgDay();
                case EXHAUSTED -> body.awakeTicks < rules.exhaustedAwakeDays() * HOURS_A_DAY * 1000;
                case SEVERE_FROSTBITE -> {
                    affliction.days += days;
                    drains += wholeHours(before, affliction.days);
                    yield false;
                }
                default -> {
                    affliction.days += days;
                    yield false;
                }
            };
            if (ends) {
                afflictions.remove();
            }
        }
        if (body.wetBlocks >= rules.blistersWetBlocks()) {
            body.wetBlocks = 0;
            body.afflict(Condition.BLISTERS);
        }
        if (body.vitaminCMg <= 0) {
            body.afflict(Condition.SCURVY);
        }
        if (body.awakeTicks >= rules.exhaustedAwakeDays() * HOURS_A_DAY * 1000) {
            body.afflict(Condition.EXHAUSTED);
        }
        frost(player, body, hours);
        for (int i = 0; i < drains && player.isAlive(); i++) {
            boolean frozen = body.has(Condition.SEVERE_FROSTBITE);
            player.hurt(player.damageSources().source(frozen ? Metabolism.FROSTBITE : Metabolism.INFECTION), DRAIN);
        }
    }

    private static int wholeHours(double fromDays, double toDays) {
        return (int) Math.floor(toDays * HOURS_A_DAY) - (int) Math.floor(fromDays * HOURS_A_DAY);
    }

    private static void frost(ServerPlayer player, Body body, double hours) {
        Balance.Conditions rules = Balance.get().conditions();
        boolean exposed = body.coreC < rules.frostCoreC() || body.effectiveC <= rules.bareSkinC() && Warmth.bareHead(player);
        if (exposed && !body.fire) {
            body.warmHours = 0;
            body.frostHours += hours;
            while (body.frostHours >= rules.frostStageHours()) {
                body.frostHours -= rules.frostStageHours();
                stage(body, stage(body) + 1);
            }
        } else if (body.fire && stage(body) > 0) {
            body.frostHours = 0;
            body.warmHours += hours;
            while (body.warmHours >= rules.frostStageHours() && stage(body) > 0) {
                body.warmHours -= rules.frostStageHours();
                stage(body, stage(body) - 1);
            }
        }
    }

    private static int stage(Body body) {
        return body.has(Condition.SEVERE_FROSTBITE) ? 3 : body.has(Condition.FROSTBITE) ? 2 : body.has(Condition.FROSTNIP) ? 1 : 0;
    }

    private static void stage(Body body, int stage) {
        body.conditions.remove(Condition.FROSTNIP);
        body.conditions.remove(Condition.FROSTBITE);
        body.conditions.remove(Condition.SEVERE_FROSTBITE);
        switch (Math.min(3, stage)) {
            case 1 -> body.afflict(Condition.FROSTNIP);
            case 2 -> body.afflict(Condition.FROSTBITE);
            case 3 -> body.afflict(Condition.SEVERE_FROSTBITE);
            default -> {}
        }
    }

    public static boolean treat(Body body, boolean medKit) {
        boolean treated = false;
        Iterator<Map.Entry<Condition, Body.Affliction>> afflictions = body.conditions.entrySet().iterator();
        while (afflictions.hasNext()) {
            Map.Entry<Condition, Body.Affliction> entry = afflictions.next();
            Condition condition = entry.getKey();
            if (medKit && condition.curedByMedKit() || !medKit && condition == Condition.BLISTERS) {
                afflictions.remove();
                treated = true;
            } else if (!medKit && condition.takesBandage() && !entry.getValue().bandaged) {
                entry.getValue().bandaged = true;
                if (condition == Condition.INFECTED_WOUND) {
                    entry.getValue().days = 0;
                }
                treated = true;
            }
        }
        return treated;
    }
}
