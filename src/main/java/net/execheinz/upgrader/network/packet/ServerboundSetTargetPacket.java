package net.execheinz.upgrader.network.packet;

import java.util.function.Supplier;
import net.execheinz.upgrader.logic.TargetSpec;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundSetTargetPacket(String itemId, String enchantmentId, int enchantmentLevel, int amount) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(itemId, 256);
        buffer.writeUtf(enchantmentId, 256);
        buffer.writeVarInt(enchantmentLevel);
        buffer.writeVarInt(amount);
    }

    public static ServerboundSetTargetPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundSetTargetPacket(buffer.readUtf(256), buffer.readUtf(256), buffer.readVarInt(), buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.containerMenu instanceof UpgraderMenu menu) {
            ResourceLocation item = ResourceLocation.tryParse(itemId);
            ResourceLocation enchantment = enchantmentId.isBlank() ? null : ResourceLocation.tryParse(enchantmentId);
            if (item != null && (enchantmentId.isBlank() || enchantment != null)) menu.selectTarget(player, new TargetSpec(item, enchantment, enchantmentLevel, amount));
        }
        context.get().setPacketHandled(true);
    }
}
