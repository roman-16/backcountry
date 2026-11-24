package io.github.roman16.backcountry.mixin;

import io.github.roman16.backcountry.body.Calendar;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.riverfishing.engine.Calendar")
abstract class RiverCalendarMixin {
    private static final int SPRING_MONTH = 3;
    private static final double DAYS_A_YEAR = 365;

    @Shadow(remap = false)
    public static int YEAR_DAYS;

    @Inject(method = "dayOfYear", at = @At("HEAD"), cancellable = true, remap = false)
    private static void backcountry$followsTheIslandsYear(Level level, CallbackInfoReturnable<Integer> result) {
        LocalDate date = Calendar.date(Calendar.day(level));
        LocalDate spring = LocalDate.of(date.getYear(), SPRING_MONTH, 1);
        long sinceSpring = Math.floorMod(ChronoUnit.DAYS.between(spring, date), (long) DAYS_A_YEAR);
        result.setReturnValue((int) Math.min(YEAR_DAYS - 1, Math.floor(sinceSpring / DAYS_A_YEAR * YEAR_DAYS)));
    }
}
