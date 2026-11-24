package io.github.roman16.backcountry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teamtea.eclipticseasons.common.core.solar.SolarDataManager;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.net.Messages;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class DevCommands {
    public static final String MARK = "driver mark ";
    private static final int OWNER = 2;

    private DevCommands() {}

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal(Backcountry.MODID)
                .then(Commands.literal("mark").then(Commands.argument("number", IntegerArgumentType.integer()).executes(context -> {
                    int number = IntegerArgumentType.getInteger(context, "number");
                    context.getSource().sendSystemMessage(Component.literal(MARK + number));
                    return 1;
                })))
                .then(Commands.literal("body").requires(source -> source.hasPermission(OWNER))
                        .then(Commands.argument("field", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[] {"core", "energy", "stomach", "water", "weight", "wetness"}, builder))
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes(DevCommands::body))))
                .then(Commands.literal("condition").requires(source -> source.hasPermission(OWNER))
                        .then(Commands.argument("condition", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Condition.values()).map(Condition::id), builder))
                                .executes(DevCommands::condition)))
                .then(Commands.literal("day").requires(source -> source.hasPermission(OWNER))
                        .then(Commands.argument("day", IntegerArgumentType.integer(1)).executes(DevCommands::day)))
                .then(Commands.literal("pretend").requires(source -> source.hasPermission(OWNER))
                        .then(Commands.literal("join").then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> Pretend.join(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("leave").then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> Pretend.leave(context.getSource(), StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("registries").requires(source -> source.hasPermission(OWNER))
                        .then(Commands.argument("folder", StringArgumentType.greedyString())
                                .executes(context -> Reports.write(context.getSource(), StringArgumentType.getString(context, "folder"))))));
    }

    private static int body(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Body body = Metabolism.body(player);
        double value = DoubleArgumentType.getDouble(context, "value");
        String field = StringArgumentType.getString(context, "field");
        switch (field) {
            case "core" -> body.coreC = value;
            case "energy" -> body.energy = value;
            case "stomach" -> body.stomachKcal = value;
            case "water" -> body.waterL = value;
            case "weight" -> body.weightKg = value;
            case "wetness" -> body.wetness = value;
            default -> {
                context.getSource().sendFailure(Component.literal("body failed: no field " + field));
                return 0;
            }
        }
        Messages.view(player, body);
        context.getSource().sendSuccess(() -> Component.literal("body " + field + " is " + value), false);
        return 1;
    }

    private static int condition(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "condition");
        try {
            Metabolism.body(player).afflict(Condition.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException missing) {
            context.getSource().sendFailure(Component.literal("condition failed: no condition " + name));
            return 0;
        }
        Messages.view(player, Metabolism.body(player));
        context.getSource().sendSuccess(() -> Component.literal("condition " + name), false);
        return 1;
    }

    private static int day(CommandContext<CommandSourceStack> context) {
        int day = IntegerArgumentType.getInteger(context, "day");
        var level = context.getSource().getServer().overworld();
        SolarDataManager solar = SolarDataManager.get(level);
        solar.setSolarTermsDay(Calendar.firstSolarDay() + day - 1);
        solar.sendAndUpdate(level);
        context.getSource().sendSuccess(() -> Component.literal("it is " + Calendar.label(Calendar.day(level)).getString()), false);
        return 1;
    }
}
