package io.github.roman16.backcountry;

import com.mojang.authlib.GameProfile;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.survivor.Arrival;
import io.github.roman16.backcountry.survivor.Kit;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

final class Pretend {
    private Pretend() {}

    static int join(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        if (server.getPlayerList().getPlayerByName(name) != null) {
            source.sendFailure(Component.literal("pretend failed: " + name + " is already online"));
            return 0;
        }
        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        Silent connection = new Silent();
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false, ConnectionType.NEOFORGE));
        if (!Metabolism.body(player).arrived) {
            Arrival.arrive(player, Kit.CHOSEN);
        }
        source.sendSuccess(() -> Component.literal("%s is online at %.1f %.1f %.1f".formatted(name, player.getX(), player.getY(), player.getZ())), false);
        return 1;
    }

    static int leave(CommandSourceStack source, String name) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
        if (player == null || !(player.connection.getConnection() instanceof Silent)) {
            source.sendFailure(Component.literal("pretend failed: " + name + " is no pretend player online"));
            return 0;
        }
        player.connection.disconnect(Component.literal("left"));
        source.sendSuccess(() -> Component.literal(name + " left"), false);
        return 1;
    }

    private static final class Silent extends Connection {
        Silent() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
            if (packet instanceof ClientboundKeepAlivePacket alive && getPacketListener() instanceof ServerCommonPacketListenerImpl common) {
                common.handleKeepAlive(new ServerboundKeepAlivePacket(alive.getId()));
            }
            if (listener != null) {
                listener.onSuccess();
            }
        }
    }
}
