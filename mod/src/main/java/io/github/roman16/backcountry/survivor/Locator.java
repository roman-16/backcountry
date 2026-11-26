package io.github.roman16.backcountry.survivor;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.net.Messages;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Locator {
    private Locator() {}

    public record Mark(UUID id, String name, double x, double y, double z) {
        public static final StreamCodec<ByteBuf, Mark> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Mark::id, ByteBufCodecs.STRING_UTF8, Mark::name,
                ByteBufCodecs.DOUBLE, Mark::x, ByteBufCodecs.DOUBLE, Mark::y, ByteBufCodecs.DOUBLE, Mark::z, Mark::new);

        static Mark of(ServerPlayer player) {
            return new Mark(player.getUUID(), player.getGameProfile().getName(), player.getX(), player.getY(), player.getZ());
        }
    }

    public static boolean carriesMessenger(ServerPlayer player) {
        return Hotbar.carried(player).stream().anyMatch(stack -> stack.is(Things.SATELLITE_MESSENGER.get()));
    }

    public static Optional<List<Mark>> seen(ServerPlayer viewer, Collection<ServerPlayer> players) {
        if (!Metabolism.lives(viewer) || !carriesMessenger(viewer)) {
            return Optional.empty();
        }
        List<ServerPlayer> teammates = players.stream().filter(player -> player != viewer && player.level() == viewer.level() && Metabolism.lives(player)).toList();
        if (teammates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(teammates.stream().filter(Locator::carriesMessenger).map(Mark::of).toList());
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % Balance.get().locator().sendTicks() != 0) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer viewer : players) {
            Messages.marks(viewer, seen(viewer, players));
        }
    }
}
