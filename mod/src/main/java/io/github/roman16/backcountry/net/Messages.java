package io.github.roman16.backcountry.net;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Alerts;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Warmth;
import io.github.roman16.backcountry.client.ClientMessages;
import io.github.roman16.backcountry.survivor.Arrival;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Forecast;
import io.github.roman16.backcountry.weather.WeatherView;
import java.util.Collection;
import java.util.List;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Messages {
    private Messages() {}

    public record View(BodyView body, WeatherView weather) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(Backcountry.id("view"));
        static final StreamCodec<RegistryFriendlyByteBuf, View> STREAM_CODEC = StreamCodec.composite(BodyView.STREAM_CODEC, View::body, WeatherView.STREAM_CODEC,
                View::weather, View::new);

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }

    public record Shown(Summary summary) implements CustomPacketPayload {
        public static final Type<Shown> TYPE = new Type<>(Backcountry.id("summary"));
        static final StreamCodec<ByteBuf, Shown> STREAM_CODEC = Summary.STREAM_CODEC.map(Shown::new, Shown::summary);

        @Override
        public Type<Shown> type() {
            return TYPE;
        }
    }

    public record Sky(List<Day> days) implements CustomPacketPayload {
        public static final Type<Sky> TYPE = new Type<>(Backcountry.id("sky"));
        static final StreamCodec<ByteBuf, Sky> STREAM_CODEC = Day.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Sky::new, Sky::days);

        @Override
        public Type<Sky> type() {
            return TYPE;
        }
    }

    public record Read() implements CustomPacketPayload {
        public static final Type<Read> TYPE = new Type<>(Backcountry.id("read"));
        static final StreamCodec<ByteBuf, Read> STREAM_CODEC = StreamCodec.unit(new Read());

        @Override
        public Type<Read> type() {
            return TYPE;
        }
    }

    public record Arrive() implements CustomPacketPayload {
        public static final Type<Arrive> TYPE = new Type<>(Backcountry.id("arrive"));
        static final StreamCodec<ByteBuf, Arrive> STREAM_CODEC = StreamCodec.unit(new Arrive());

        @Override
        public Type<Arrive> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(View.TYPE, View.STREAM_CODEC, (payload, context) -> ClientMessages.view(payload.body(), payload.weather()));
        registrar.playToClient(Sky.TYPE, Sky.STREAM_CODEC, (payload, context) -> ClientMessages.sky(payload.days()));
        registrar.playToClient(Shown.TYPE, Shown.STREAM_CODEC, (payload, context) -> ClientMessages.summary(payload.summary()));
        registrar.playToServer(Arrive.TYPE, Arrive.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                arrive(player);
            }
        });
        registrar.playToServer(Read.TYPE, Read.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                Metabolism.body(player).unread = false;
            }
        });
    }

    private static void arrive(ServerPlayer player) {
        Body body = Metabolism.body(player);
        if (player.isDeadOrDying()) {
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        } else if (body.ended != null) {
            Arrival.arrive(player);
        }
    }

    public static void view(ServerPlayer player, Body body) {
        Balance balance = Balance.get();
        BodyView view = new BodyView((float) body.energy, (float) body.waterShare(), (float) body.waterL, (float) balance.water().needLDay(), Metabolism.shown(body),
                (float) body.weightKg, (float) body.coreC, Warmth.comfort(body), Double.compare(Math.round(body.coreC * 100), Math.round(body.lastCoreC * 100)),
                (float) body.wetness, body.awakeTicks / 1000F, (float) body.kcalEatenToday, (float) body.kcalBurnedToday, (float) body.proteinToday, (float) body.carbsToday,
                (float) body.fatToday, (float) body.stomachShare(), (float) body.loadKg, (float) balance.load().capacityKg(), Calendar.day(player.level()), body.blackoutTicks,
                body.fire, body.sheltered, body.windy, (float) body.clo, Alerts.shown(body), body.unread);
        if (!(player instanceof FakePlayer) && player.connection.hasChannel(View.TYPE)) {
            PacketDistributor.sendToPlayer(player, new View(view, Forecast.view(player, body)));
        }
    }

    public static void summary(ServerPlayer player, Summary summary) {
        if (!(player instanceof FakePlayer) && player.connection.hasChannel(Shown.TYPE)) {
            PacketDistributor.sendToPlayer(player, new Shown(summary));
        }
    }

    public static void sky(Collection<ServerPlayer> players, List<Day> days) {
        for (ServerPlayer player : players) {
            if (!(player instanceof FakePlayer) && player.connection.hasChannel(Sky.TYPE)) {
                PacketDistributor.sendToPlayer(player, new Sky(days));
            }
        }
    }

    public static void read() {
        PacketDistributor.sendToServer(new Read());
    }

    public static void arrive() {
        PacketDistributor.sendToServer(new Arrive());
    }
}
