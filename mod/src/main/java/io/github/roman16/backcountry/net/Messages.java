package io.github.roman16.backcountry.net;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Alerts;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.body.Warmth;
import io.github.roman16.backcountry.client.ClientMessages;
import io.github.roman16.backcountry.fire.Lighting;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.guide.GuideView;
import io.github.roman16.backcountry.survivor.Arrival;
import io.github.roman16.backcountry.survivor.Summary;
import io.github.roman16.backcountry.survivor.Team;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Forecast;
import io.github.roman16.backcountry.weather.WeatherView;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
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

    public record View(BodyView body, WeatherView weather, GuideView guide) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(Backcountry.id("view"));
        static final StreamCodec<RegistryFriendlyByteBuf, View> STREAM_CODEC = StreamCodec.composite(BodyView.STREAM_CODEC, View::body, WeatherView.STREAM_CODEC,
                View::weather, GuideView.STREAM_CODEC, View::guide, View::new);

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

    public record BowDrill(BlockPos fire, boolean wet) implements CustomPacketPayload {
        public static final Type<BowDrill> TYPE = new Type<>(Backcountry.id("bow_drill"));
        static final StreamCodec<ByteBuf, BowDrill> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BowDrill::fire, ByteBufCodecs.BOOL, BowDrill::wet,
                BowDrill::new);

        @Override
        public Type<BowDrill> type() {
            return TYPE;
        }
    }

    public record Drilled(BlockPos fire, int strokes, boolean lit) implements CustomPacketPayload {
        public static final Type<Drilled> TYPE = new Type<>(Backcountry.id("drilled"));
        static final StreamCodec<ByteBuf, Drilled> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Drilled::fire, ByteBufCodecs.VAR_INT, Drilled::strokes,
                ByteBufCodecs.BOOL, Drilled::lit, Drilled::new);

        @Override
        public Type<Drilled> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(View.TYPE, View.STREAM_CODEC, (payload, context) -> ClientMessages.view(payload.body(), payload.weather(), payload.guide()));
        registrar.playToClient(Sky.TYPE, Sky.STREAM_CODEC, (payload, context) -> ClientMessages.sky(payload.days()));
        registrar.playToClient(BowDrill.TYPE, BowDrill.STREAM_CODEC, (payload, context) -> ClientMessages.bowDrill(payload.fire(), payload.wet()));
        registrar.playToServer(Drilled.TYPE, Drilled.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                Lighting.drilled(player, payload.fire(), payload.strokes(), payload.lit());
            }
        });
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
        Team team = Team.get(player.server);
        BodyView view = new BodyView((float) body.energy, (float) body.waterShare(), (float) body.waterL, (float) balance.water().needLDay(), Metabolism.shown(body),
                (float) body.weightKg, (float) body.coreC, Warmth.comfort(body), Double.compare(Math.round(body.coreC * 100), Math.round(body.lastCoreC * 100)),
                (float) body.wetness, body.awakeTicks / 1000F, (float) body.kcalEatenToday, (float) body.kcalBurnedToday, (float) body.proteinToday, (float) body.carbsToday,
                (float) body.fatToday, (float) body.stomachShare(), (float) body.loadKg, (float) balance.load().capacityKg(), Calendar.day(player.level()), body.blackoutTicks,
                body.fire, body.sheltered, body.windy, (float) body.clo, Alerts.shown(body), body.unread, team.ration().ordinal(),
                (float) team.storesDays(Team.living(player.server)), Arrays.stream(body.burned).mapToObj(kcal -> (float) kcal).toList(),
                Skill.levels(body));
        if (!(player instanceof FakePlayer) && player.connection.hasChannel(View.TYPE)) {
            PacketDistributor.sendToPlayer(player, new View(view, Forecast.view(player, body), Guide.get(player.server).view(player)));
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

    public static void bowDrill(ServerPlayer player, BlockPos fire, boolean wet) {
        if (!(player instanceof FakePlayer) && player.connection.hasChannel(BowDrill.TYPE)) {
            PacketDistributor.sendToPlayer(player, new BowDrill(fire, wet));
        }
    }

    public static void drilled(BlockPos fire, int strokes, boolean lit) {
        PacketDistributor.sendToServer(new Drilled(fire, strokes, lit));
    }

    public static void read() {
        PacketDistributor.sendToServer(new Read());
    }

    public static void arrive() {
        PacketDistributor.sendToServer(new Arrive());
    }
}
