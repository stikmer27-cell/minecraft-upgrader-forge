package net.execheinz.upgrader.menu;

import net.execheinz.upgrader.UpgraderConfig;
import net.execheinz.upgrader.logic.ResultAmountPolicy;
import net.execheinz.upgrader.logic.OddsPolicy;
import net.execheinz.upgrader.logic.TargetSpec;
import net.execheinz.upgrader.logic.UpgradeMode;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.packet.ClientboundSyncPacket;
import net.execheinz.upgrader.registry.ModMenus;
import net.execheinz.upgrader.server.AttemptManager;
import net.execheinz.upgrader.value.TargetCatalog;
import net.execheinz.upgrader.value.TargetEligibility;
import net.execheinz.upgrader.value.ValuationEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class UpgraderMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 49;
    public static final int INPUT_Y = 47;
    public static final int TARGET_X = 271;
    public static final int TARGET_Y = 47;
    public static final int INVENTORY_X = 87;
    public static final int INVENTORY_Y = 161;
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override public void setChanged() { super.setChanged(); slotsChanged(this); }
    };
    private final Player player;
    private ItemStack target = ItemStack.EMPTY;
    private int targetAmount = 1;
    private long inputValue;
    private long unitValue;
    private long totalValue;
    private float chance;
    private boolean processing;
    private int lossStreak;
    private double playerFactor = 1.03D;
    private double minChance = 0.005D;
    private double maxChance = 0.98D;
    private boolean clientSlotsVisible = true;

    public UpgraderMenu(int id, Inventory inventory) {
        super(ModMenus.UPGRADER.get(), id);
        this.player = inventory.player;
        addSlot(new Slot(input, 0, INPUT_X, INPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !processing && TargetEligibility.isAllowedWager(player.level(), stack);
            }
            @Override public boolean mayPickup(Player player) { return !processing; }
            @Override public boolean isActive() { return clientSlotsVisible; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(visibleInventorySlot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(visibleInventorySlot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
    }

    private Slot visibleInventorySlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override public boolean isActive() { return clientSlotsVisible; }
        };
    }

    public ItemStack inputStack() { return input.getItem(0); }
    public ItemStack targetStack() { return target; }
    public int targetAmount() { return targetAmount; }
    public long inputValue() { return inputValue; }
    public long unitValue() { return unitValue; }
    public long totalValue() { return totalValue; }
    public float chance() { return chance; }
    public boolean processing() { return processing; }
    public double previewChance(double targetValue) {
        return OddsPolicy.chance(inputValue, targetValue, playerFactor, minChance, maxChance, lossStreak);
    }
    public void setClientSlotsVisible(boolean visible) { clientSlotsVisible = visible; }

    public void setClientState(ClientboundSyncPacket packet) {
        target = packet.target().copy();
        targetAmount = packet.amount();
        inputValue = packet.inputValue();
        unitValue = packet.unitValue();
        totalValue = packet.totalValue();
        chance = packet.chance();
        processing = packet.processing();
        lossStreak = packet.lossStreak();
        playerFactor = packet.playerFactor();
        minChance = packet.minChance();
        maxChance = packet.maxChance();
    }

    public void selectTarget(ServerPlayer serverPlayer, TargetSpec spec) {
        if (AttemptManager.INSTANCE.isProcessing(serverPlayer)) return;
        ItemStack candidate = spec.createValidatedStack();
        if (!TargetEligibility.isAllowed(serverPlayer.level(), candidate)) return;
        target = candidate;
        targetAmount = ResultAmountPolicy.normalize(candidate, spec.amount(), UpgraderConfig.MAX_RESULT_AMOUNT.get());
        sync(serverPlayer);
    }

    public void selectMode(ServerPlayer serverPlayer, UpgradeMode mode) {
        if (AttemptManager.INSTANCE.isProcessing(serverPlayer) || inputStack().isEmpty()) return;
        double inputWorth = ValuationEngine.INSTANCE.stackValue(serverPlayer.level(), inputStack());
        double desired = mode.desiredTargetValue(inputWorth, UpgraderConfig.PLAYER_FACTOR.get());
        ItemStack found = TargetCatalog.INSTANCE.closest(serverPlayer.level(), desired);
        if (found.isEmpty()) return;
        target = found;
        targetAmount = 1;
        sync(serverPlayer);
    }

    public void start(ServerPlayer player, UUID attemptId) { AttemptManager.INSTANCE.start(player, this, attemptId); }

    public ItemStack consumeInput() {
        ItemStack consumed = input.removeItemNoUpdate(0);
        input.setChanged();
        return consumed;
    }

    public void sync(ServerPlayer serverPlayer) { AttemptManager.INSTANCE.syncMenu(serverPlayer, this); }

    @Override public void slotsChanged(Container container) {
        if (player instanceof ServerPlayer serverPlayer) sync(serverPlayer);
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!processing) super.clicked(slotId, button, clickType, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (processing || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == 0 ? !moveItemStackTo(source, 1, 37, true) : !moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, input);
    }

    @Override public boolean stillValid(Player player) { return player.isAlive(); }
}
