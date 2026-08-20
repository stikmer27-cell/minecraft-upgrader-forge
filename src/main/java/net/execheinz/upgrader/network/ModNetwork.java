package net.execheinz.upgrader.network;

import net.execheinz.upgrader.UpgraderMod;
import net.execheinz.upgrader.network.packet.ClientboundResultPacket;
import net.execheinz.upgrader.network.packet.ClientboundSyncPacket;
import net.execheinz.upgrader.network.packet.ServerboundModePacket;
import net.execheinz.upgrader.network.packet.ServerboundSetTargetPacket;
import net.execheinz.upgrader.network.packet.ServerboundStartPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(UpgraderMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ServerboundSetTargetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundSetTargetPacket::encode).decoder(ServerboundSetTargetPacket::decode).consumerMainThread(ServerboundSetTargetPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundModePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundModePacket::encode).decoder(ServerboundModePacket::decode).consumerMainThread(ServerboundModePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundStartPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundStartPacket::encode).decoder(ServerboundStartPacket::decode).consumerMainThread(ServerboundStartPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundSyncPacket::encode).decoder(ClientboundSyncPacket::decode).consumerMainThread(ClientboundSyncPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundResultPacket.class, id, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundResultPacket::encode).decoder(ClientboundResultPacket::decode).consumerMainThread(ClientboundResultPacket::handle).add();
    }

    public static void sendToServer(Object packet) { CHANNEL.sendToServer(packet); }
    public static void send(ServerPlayer player, Object packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
}
