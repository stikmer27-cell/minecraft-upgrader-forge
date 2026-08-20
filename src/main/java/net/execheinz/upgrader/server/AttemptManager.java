package net.execheinz.upgrader.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.execheinz.upgrader.UpgraderConfig;
import net.execheinz.upgrader.logic.OddsPolicy;
import net.execheinz.upgrader.logic.AttemptIdLedger;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.packet.ClientboundResultPacket;
import net.execheinz.upgrader.network.packet.ClientboundSyncPacket;
import net.execheinz.upgrader.value.ValuationEngine;
import net.execheinz.upgrader.value.TargetEligibility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AttemptManager {
    public static final AttemptManager INSTANCE = new AttemptManager();
    private static final String LOSS_STREAK = "upgraderLossStreak";
    private static final int SEEN_LIMIT = 4096;
    private final Map<UUID, ActiveAttempt> active = new HashMap<>();
    private final AttemptIdLedger seenAttemptIds = new AttemptIdLedger(SEEN_LIMIT);

    private AttemptManager() {}

    public boolean isProcessing(ServerPlayer player) { return active.containsKey(player.getUUID()); }

    public void start(ServerPlayer player, UpgraderMenu menu, UUID attemptId) {
        if (attemptId == null || active.containsKey(player.getUUID()) || seenAttemptIds.contains(attemptId)) return;
        ItemStack wager = menu.inputStack().copy();
        ItemStack target = menu.targetStack().copy();
        if (!TargetEligibility.isAllowedWager(player.level(), wager)
            || !TargetEligibility.isAllowed(player.level(), target)) return;

        int amount = menu.targetAmount();
        double inputValue = ValuationEngine.INSTANCE.stackValue(player.level(), wager);
        double unitValue = ValuationEngine.INSTANCE.stackValue(player.level(), target);
        double targetValue = unitValue * amount;
        int losses = lossStreak(player);
        double chance = OddsPolicy.chance(inputValue, targetValue, UpgraderConfig.PLAYER_FACTOR.get(), UpgraderConfig.MIN_CHANCE.get(), UpgraderConfig.MAX_CHANCE.get(), losses);
        if (chance <= 0.0D) return;

        if (!seenAttemptIds.markIfNew(attemptId)) return;
        boolean success = player.getRandom().nextDouble() < chance;
        int duration = UpgraderConfig.SPIN_TICKS.get();
        long resolveTick = player.serverLevel().getGameTime() + duration;
        ActiveAttempt attempt = new ActiveAttempt(attemptId, player.getUUID(), target, amount, success, chance, resolveTick, AttemptState.PROCESSING);
        active.put(player.getUUID(), attempt); // lock is visible before the wager is removed
        ItemStack consumed = menu.consumeInput();
        if (consumed.isEmpty()) {
            active.remove(player.getUUID());
            return;
        }
        float landing = landingAngle(player, chance, success);
        ModNetwork.send(player, new ClientboundResultPacket(attemptId, success, landing, duration, (float) chance));
        syncMenu(player, menu);
    }

    public void syncMenu(ServerPlayer player, UpgraderMenu menu) {
        ItemStack input = menu.inputStack();
        ItemStack target = menu.targetStack();
        long inputValue = Math.round(ValuationEngine.INSTANCE.stackValue(player.level(), input));
        long unitValue = Math.round(ValuationEngine.INSTANCE.stackValue(player.level(), target));
        long totalValue = saturatingMultiply(unitValue, menu.targetAmount());
        float chance = (float) OddsPolicy.chance(inputValue, totalValue, UpgraderConfig.PLAYER_FACTOR.get(), UpgraderConfig.MIN_CHANCE.get(), UpgraderConfig.MAX_CHANCE.get(), lossStreak(player));
        ModNetwork.send(player, new ClientboundSyncPacket(target, menu.targetAmount(), inputValue, unitValue, totalValue, chance, isProcessing(player),
            lossStreak(player), UpgraderConfig.PLAYER_FACTOR.get(), UpgraderConfig.MIN_CHANCE.get(), UpgraderConfig.MAX_CHANCE.get()));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active.isEmpty()) return;
        MinecraftServer server = event.getServer();
        Iterator<ActiveAttempt> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveAttempt attempt = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(attempt.playerId);
            if (player != null && player.serverLevel().getGameTime() >= attempt.resolveTick) {
                iterator.remove();
                resolve(player, attempt);
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ActiveAttempt attempt = active.remove(player.getUUID());
        if (attempt != null) resolve(player, attempt);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putInt(LOSS_STREAK, event.getOriginal().getPersistentData().getInt(LOSS_STREAK));
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        ValuationEngine.INSTANCE.invalidate();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        for (ActiveAttempt attempt : active.values()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(attempt.playerId);
            if (player != null) resolve(player, attempt);
        }
        active.clear();
    }

    private void resolve(ServerPlayer player, ActiveAttempt attempt) {
        if (attempt.state != AttemptState.PROCESSING) return;
        attempt.state = AttemptState.RESOLVED;
        if (attempt.success) giveReward(player, attempt.target, attempt.amount);
        player.getPersistentData().putInt(LOSS_STREAK, attempt.success ? 0 : Math.min(100, lossStreak(player) + 1));
        if (player.containerMenu instanceof UpgraderMenu menu) syncMenu(player, menu);
    }

    static void giveReward(ServerPlayer player, ItemStack template, int amount) {
        int remaining = Math.max(1, amount);
        int maxStack = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int count = Math.min(maxStack, remaining);
            ItemStack part = template.copy();
            part.setCount(count);
            player.getInventory().add(part);
            if (!part.isEmpty()) player.drop(part, false);
            remaining -= count;
        }
        player.containerMenu.broadcastChanges();
    }

    private static int lossStreak(ServerPlayer player) { return Math.max(0, player.getPersistentData().getInt(LOSS_STREAK)); }

    private static float landingAngle(ServerPlayer player, double chance, boolean success) {
        float zone = (float) (Mth.clamp(chance, 0.0D, 1.0D) * 360.0D);
        float span = success ? zone : 360.0F - zone;
        float margin = Math.min(2.0F, span * 0.2F);
        float usable = Math.max(0.01F, span - margin * 2.0F);
        float offset = margin + player.getRandom().nextFloat() * usable;
        return success ? offset : (zone + offset) % 360.0F;
    }

    private static long saturatingMultiply(long value, int amount) {
        if (value <= 0 || amount <= 0) return 0L;
        return value > Long.MAX_VALUE / amount ? Long.MAX_VALUE : value * amount;
    }

    private static final class ActiveAttempt {
        private final UUID attemptId;
        private final UUID playerId;
        private final ItemStack target;
        private final int amount;
        private final boolean success;
        private final double chance;
        private final long resolveTick;
        private AttemptState state;

        private ActiveAttempt(UUID attemptId, UUID playerId, ItemStack target, int amount, boolean success, double chance, long resolveTick, AttemptState state) {
            this.attemptId = attemptId;
            this.playerId = playerId;
            this.target = target;
            this.amount = amount;
            this.success = success;
            this.chance = chance;
            this.resolveTick = resolveTick;
            this.state = state;
        }
    }
}
