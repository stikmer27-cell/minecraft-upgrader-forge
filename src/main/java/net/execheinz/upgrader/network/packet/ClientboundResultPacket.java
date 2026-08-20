package net.execheinz.upgrader.network.packet;

import java.util.UUID;
import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundResultPacket(UUID attemptId, boolean success, float landingAngle, int durationTicks, float chance) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeUUID(attemptId); buffer.writeBoolean(success); buffer.writeFloat(landingAngle); buffer.writeVarInt(durationTicks); buffer.writeFloat(chance); }
    public static ClientboundResultPacket decode(FriendlyByteBuf buffer) { return new ClientboundResultPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readFloat(), buffer.readVarInt(), buffer.readFloat()); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.result(this));
        context.get().setPacketHandled(true);
    }
}
