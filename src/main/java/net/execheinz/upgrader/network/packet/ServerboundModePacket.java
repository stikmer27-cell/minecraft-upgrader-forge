package net.execheinz.upgrader.network.packet;

import java.util.function.Supplier;
import net.execheinz.upgrader.logic.UpgradeMode;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundModePacket(UpgradeMode mode) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeEnum(mode); }
    public static ServerboundModePacket decode(FriendlyByteBuf buffer) { return new ServerboundModePacket(buffer.readEnum(UpgradeMode.class)); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.containerMenu instanceof UpgraderMenu menu) menu.selectMode(player, mode);
        context.get().setPacketHandled(true);
    }
}
