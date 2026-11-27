package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.gear.TreatmentItem;
import io.github.roman16.backcountry.gear.Worn;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;

public final class Afflictions {
    private static final double KCAL_A_GRAM_OF_FAT = 9;
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
                case RABBIT_STARVATION -> body.fattyDays >= rules.rabbitCureDays();
                case NUMB_HANDS -> body.numbHours <= 0;
                case SHELLFISH_POISONING -> {
                    affliction.days += days;
                    drains += wholeHours(before, affliction.days);
                    yield affliction.days * HOURS_A_DAY >= rules.shellfishHours();
                }
                case SEVERE_FROSTBITE, SEVERE_HYPOTHERMIA -> {
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
        if (body.gutOnsetDays > 0) {
            body.gutOnsetDays -= days;
            if (body.gutOnsetDays <= 0) {
                body.gutOnsetDays = 0;
                body.afflict(Condition.GUT_INFECTION);
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
        hypothermia(body);
        numbHands(player, body, hours);
        for (int i = 0; i < drains && player.isAlive(); i++) {
            ResourceKey<DamageType> cause = body.has(Condition.SEVERE_HYPOTHERMIA) ? Metabolism.HYPOTHERMIA
                    : body.has(Condition.SEVERE_FROSTBITE) ? Metabolism.FROSTBITE : body.has(Condition.SHELLFISH_POISONING) ? Metabolism.SHELLFISH : Metabolism.INFECTION;
            player.hurt(player.damageSources().source(cause), DRAIN);
        }
        tell(player, body);
    }

    private static void tell(ServerPlayer player, Body body) {
        body.conditions.forEach((condition, affliction) -> {
            if (!affliction.told) {
                affliction.told = true;
                player.sendSystemMessage(condition.began());
            }
        });
    }

    private static int wholeHours(double fromDays, double toDays) {
        return (int) Math.floor(toDays * HOURS_A_DAY) - (int) Math.floor(fromDays * HOURS_A_DAY);
    }

    private static void frost(ServerPlayer player, Body body, double hours) {
        Balance.Conditions rules = Balance.get().conditions();
        boolean exposed = body.effectiveC <= rules.bareSkinC() && Warmth.bareHead(player);
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

    private static void hypothermia(Body body) {
        Balance.Conditions rules = Balance.get().conditions();
        Condition now = body.coreC < rules.severeHypothermiaCoreC() ? Condition.SEVERE_HYPOTHERMIA
                : body.coreC < rules.hypothermiaCoreC() ? Condition.HYPOTHERMIA : null;
        for (Condition cold : new Condition[] {Condition.HYPOTHERMIA, Condition.SEVERE_HYPOTHERMIA}) {
            if (cold != now) {
                body.conditions.remove(cold);
            }
        }
        if (now != null) {
            body.afflict(now);
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

    public static boolean incubating(Body body) {
        return body.gutOnsetDays > 0;
    }

    public static boolean salves(Condition condition) {
        return condition == Condition.INFECTED_WOUND || condition == Condition.BURNS;
    }

    public static boolean treat(Body body, TreatmentItem.Treatment treatment) {
        if (treatment == TreatmentItem.Treatment.SALVE) {
            return salve(body);
        }
        boolean medKit = treatment == TreatmentItem.Treatment.MED_KIT;
        boolean treated = medKit && incubating(body);
        if (treated) {
            body.gutOnsetDays = 0;
        }
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

    private static boolean salve(Body body) {
        Balance.Conditions rules = Balance.get().conditions();
        boolean treated = false;
        for (Map.Entry<Condition, Body.Affliction> entry : body.conditions.entrySet()) {
            Body.Affliction affliction = entry.getValue();
            if (entry.getKey() == Condition.INFECTED_WOUND) {
                affliction.bandaged = true;
                affliction.days = Math.max(affliction.days, rules.infectionDays() - 1);
                treated = true;
            } else if (entry.getKey() == Condition.BURNS) {
                affliction.bandaged = true;
                affliction.days = Math.max(affliction.days, rules.burnsDays() - 2);
                treated = true;
            }
        }
        return treated;
    }

    private static void numbHands(ServerPlayer player, Body body, double hours) {
        Balance.Conditions rules = Balance.get().conditions();
        boolean bare = Worn.HANDS.get(player).isEmpty() && !player.isCreative();
        if (bare && body.effectiveC <= rules.bareSkinC() && !body.fire) {
            body.numbHours += hours;
        } else {
            body.numbHours = Math.max(0, body.numbHours - 2 * hours);
        }
        if (body.numbHours >= rules.numbOnsetHours()) {
            body.afflict(Condition.NUMB_HANDS);
        }
    }

    public static void day(Body body) {
        Balance.Conditions rules = Balance.get().conditions();
        double kcal = Math.max(1, body.kcalEatenToday);
        double fatShare = body.fatToday * KCAL_A_GRAM_OF_FAT / kcal;
        if (body.proteinToday > rules.rabbitProteinG() && fatShare < rules.rabbitFatShare()) {
            body.leanDays++;
            body.fattyDays = 0;
        } else if (fatShare >= rules.rabbitFatShare()) {
            body.fattyDays++;
            body.leanDays = 0;
        }
        if (body.leanDays >= rules.rabbitOnsetDays()) {
            body.afflict(Condition.RABBIT_STARVATION);
        }
    }
}
