package io.github.roman16.backcountry.weather;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.net.Messages;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Sky {
    private static final int TICKS_A_DAY = 24000;
    private static final int TICKS_AN_HOUR = 1000;
    private static final String WEATHER_COMMAND = "weather";
    private static int sharedDay = Integer.MIN_VALUE;

    private Sky() {}

    @SubscribeEvent
    static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        int day = Calendar.day(level);
        if (day != sharedDay) {
            sharedDay = day;
            share(level);
        }
        drive(level, day);
    }

    public static void drive(ServerLevel level, int day) {
        Day today = Weather.day(level, day);
        boolean falling = today.falling(Climate.hour(level));
        boolean thunder = falling && today.thunder();
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        int timer = falling ? data.getRainTime() : data.getClearWeatherTime();
        if (data.isRaining() != falling || data.isThundering() != thunder || timer < TICKS_AN_HOUR) {
            level.setWeatherParameters(falling ? 0 : TICKS_A_DAY, falling ? TICKS_A_DAY : 0, falling, thunder);
        }
    }

    public static void share(ServerLevel level) {
        Messages.sky(level.getServer().getPlayerList().getPlayers(), Weather.around(level, Calendar.day(level)));
    }

    @SubscribeEvent
    static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.server.overworld();
            Messages.sky(List.of(player), Weather.around(level, Calendar.day(level)));
        }
    }

    @SubscribeEvent
    static void stopped(ServerStoppedEvent event) {
        sharedDay = Integer.MIN_VALUE;
        Weather.forget();
    }

    @SubscribeEvent
    static void command(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
        if (!context.getNodes().isEmpty() && context.getNodes().getFirst().getNode() instanceof LiteralCommandNode<CommandSourceStack> literal
                && literal.getLiteral().equals(WEATHER_COMMAND)) {
            event.setCanceled(true);
            context.getSource().sendFailure(Component.translatable("message.backcountry.weather_follows_forecast"));
        }
    }

    @SubscribeEvent
    static void joining(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof SkeletonHorse horse && horse.isTrap()) {
            event.setCanceled(true);
        }
    }
}
