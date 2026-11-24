package io.github.roman16.backcountry;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.teamtea.eclipticseasons.common.core.solar.SolarDataManager;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

final class Playthrough {
    private static final int HOURS = 24;
    private static final long TICKS_AN_HOUR = 1000;
    private static final long TICKS_A_DAY = 24000;
    private static final int DUSK_HOUR = 12;
    private static final int BED_HOUR = 16;
    private static final double EXTRACTION_MARGIN = 0.05;
    private static final int BERRIES_A_MEAL = 3;
    private static final UUID SURVIVOR = UUID.nameUUIDFromBytes("playthrough".getBytes(StandardCharsets.UTF_8));

    private Playthrough() {}

    static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("playthrough").requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("first_day", IntegerArgumentType.integer(1))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("kcal_a_day", DoubleArgumentType.doubleArg(0))
                                                .then(Commands.argument("work_kcal_a_day", DoubleArgumentType.doubleArg(0)).executes(Playthrough::run))))));
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        String name = StringArgumentType.getString(context, "name");
        int firstDay = IntegerArgumentType.getInteger(context, "first_day");
        int days = IntegerArgumentType.getInteger(context, "days");
        double kcal = DoubleArgumentType.getDouble(context, "kcal_a_day");
        double work = DoubleArgumentType.getDouble(context, "work_kcal_a_day");
        Vec3 at = source.getPosition();
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(SURVIVOR, "playthrough"));
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(true);
        player.moveTo(at.x, at.y, at.z, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Things.HIDE_HAT.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Things.HIDE_COAT.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Things.HIDE_PANTS.get()));
        Body body = Metabolism.body(player);
        body.reset(firstDay - 1, 1);
        SolarDataManager solar = SolarDataManager.get(level);
        int solarDay = solar.getSolarTermsDay();
        long time = level.getDayTime();
        List<String> lines = new ArrayList<>(List.of("day,date,kg,bmi,energy,core_c,eaten_kcal,burned_kcal,conditions"));
        String ended = "";
        try {
            for (int offset = 0; offset < days && ended.isEmpty(); offset++) {
                int day = firstDay - 1 + offset;
                solar.setSolarTermsDay(Calendar.firstSolarDay() + day);
                long morning = (time / TICKS_A_DAY + offset + 1) * TICKS_A_DAY;
                for (int hour = 0; hour < HOURS; hour++) {
                    level.setDayTime(morning + hour * TICKS_AN_HOUR);
                    boolean asleep = hour >= BED_HOUR;
                    if (!asleep) {
                        body.workKcal += work / BED_HOUR;
                    }
                    if (body.bmi() <= Balance.get().body().extractionBmi() + EXTRACTION_MARGIN) {
                        ended = "extracted on day " + (day + 1);
                        break;
                    }
                    Metabolism.step(player, body, TICKS_AN_HOUR, asleep);
                    if (hour == 0 || hour == DUSK_HOUR) {
                        meal(player, kcal / 2);
                    }
                }
                lines.add(String.format(Locale.ROOT, "%d,%s,%.2f,%.2f,%.2f,%.2f,%.0f,%.0f,%s", day + 1, Calendar.dateText(day), body.weightKg, body.bmi(), body.energy,
                        body.coreC, body.kcalEatenToday, body.kcalBurnedToday, String.join(" ", Metabolism.shown(body).stream().map(condition -> condition.id()).toList())));
            }
        } finally {
            solar.setSolarTermsDay(solarDay);
            level.setDayTime(time);
        }
        Path file = FMLPaths.GAMEDIR.get().resolve("../playthroughs/" + name + ".csv").normalize();
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        String summary = String.format(Locale.ROOT, "playthrough %s: %.1f kg after %d days from %.1f kg%s, written to %s", name, body.weightKg, lines.size() - 1,
                Balance.get().body().arrivalKg(), ended.isEmpty() ? "" : ", " + ended, file);
        source.sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }

    private static void meal(ServerPlayer player, double kcal) {
        for (int berry = 0; berry < BERRIES_A_MEAL; berry++) {
            Metabolism.eat(player, new ItemStack(Items.SWEET_BERRIES));
        }
        ItemStack bread = new ItemStack(Things.PILOT_BREAD.get());
        double each = Nutrition.of(bread).map(Nutrition::kcal).orElse(0.0);
        double eaten = BERRIES_A_MEAL * Nutrition.of(new ItemStack(Items.SWEET_BERRIES)).map(Nutrition::kcal).orElse(0.0);
        while (each > 0 && eaten < kcal && Metabolism.eat(player, bread.copy())) {
            eaten += each;
        }
    }
}
