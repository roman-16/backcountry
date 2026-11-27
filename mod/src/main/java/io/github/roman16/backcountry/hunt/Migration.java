package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import java.time.LocalDate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class Migration {
    public static final ResourceLocation CARIBOU = ResourceLocation.fromNamespaceAndPath("cnc", "caribou");
    public static final ResourceLocation GOOSE = ResourceLocation.fromNamespaceAndPath("fowlplay", "goose");
    public static final ResourceLocation HARE = ResourceLocation.withDefaultNamespace("rabbit");

    private Migration() {}

    public static boolean caribou(int day) {
        Balance.Migration rules = Balance.get().migration();
        LocalDate date = Calendar.date(day);
        return (date.getMonthValue() == rules.caribouSpringMonth() || date.getMonthValue() == rules.caribouAutumnMonth()) && date.getDayOfMonth() >= rules.fromDayOfMonth();
    }

    public static boolean caribouBegin(int day) {
        return caribou(day) && !caribou(day - 1);
    }

    public static boolean geese(int day) {
        Balance.Migration rules = Balance.get().migration();
        int month = Calendar.date(day).getMonthValue();
        return month == rules.geeseSpringMonth() || month == rules.geeseAutumnMonth();
    }

    public static double share(EntityType<?> type, int day) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Balance balance = Balance.get();
        if (CARIBOU.equals(id) && caribou(day)) {
            return balance.migration().herdShare();
        }
        if (GOOSE.equals(id) && geese(day)) {
            return balance.migration().geeseShare();
        }
        if (HARE.equals(id)) {
            return balance.director().hareCycle().get(Math.floorMod(Calendar.year(day) - 1, balance.director().hareCycle().size()));
        }
        return 1;
    }

    public static int herd(EntityType<?> type, int day, int count) {
        return CARIBOU.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)) && caribou(day) ? (int) Math.round(count * Balance.get().migration().herdShare()) : count;
    }
}
