package net.execheinz.upgrader.network.packet;

import java.util.UUID;
import java.util.function.Supplier;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundStartPacket(UUID attemptId) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeUUID(attemptId); }
    public static ServerboundStartPacket decode(FriendlyByteBuf buffer) { return new ServerboundStartPacket(buffer.readUUID()); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.containerMenu instanceof UpgraderMenu menu) menu.start(player, attemptId);
        context.get().setPacketHandled(true);
    }
}
