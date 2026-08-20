package net.execheinz.upgrader.item;

import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class UpgraderItem extends Item implements MenuProvider {
    public UpgraderItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, this);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override public Component getDisplayName() { return Component.translatable("container.upgrader"); }

    @Nullable
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new UpgraderMenu(id, inventory); }
}
