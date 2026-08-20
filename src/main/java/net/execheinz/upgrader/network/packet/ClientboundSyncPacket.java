package net.execheinz.upgrader.network.packet;

import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundSyncPacket(ItemStack target, int amount, long inputValue, long unitValue, long totalValue, float chance, boolean processing,
                                    int lossStreak, double playerFactor, double minChance, double maxChance) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(target);
        buffer.writeVarInt(amount);
        buffer.writeLong(inputValue);
        buffer.writeLong(unitValue);
        buffer.writeLong(totalValue);
        buffer.writeFloat(chance);
        buffer.writeBoolean(processing);
        buffer.writeVarInt(lossStreak);
        buffer.writeDouble(playerFactor);
        buffer.writeDouble(minChance);
        buffer.writeDouble(maxChance);
    }
    public static ClientboundSyncPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundSyncPacket(buffer.readItem(), buffer.readVarInt(), buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readFloat(),
            buffer.readBoolean(), buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.sync(this));
        context.get().setPacketHandled(true);
    }
}
