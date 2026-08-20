package net.execheinz.upgrader.client;

import net.execheinz.upgrader.menu.UpgraderMenu;
import net.execheinz.upgrader.network.packet.ClientboundResultPacket;
import net.execheinz.upgrader.network.packet.ClientboundSyncPacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void sync(ClientboundSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof UpgraderMenu menu) menu.setClientState(packet);
        if (minecraft.screen instanceof UpgraderScreen screen) screen.onSync(packet);
    }

    public static void result(ClientboundResultPacket packet) {
        if (Minecraft.getInstance().screen instanceof UpgraderScreen screen) screen.onResult(packet);
    }
}
