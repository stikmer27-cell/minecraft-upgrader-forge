package net.execheinz.upgrader.value;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class TargetCatalog {
    public static final TargetCatalog INSTANCE = new TargetCatalog();
    private volatile Snapshot clientCache = Snapshot.EMPTY;
    private volatile Snapshot serverCache = Snapshot.EMPTY;
    private TargetCatalog() {}

    public List<ItemStack> all(Level level) {
        return snapshot(level).stacks;
    }

    public List<CatalogEntry> entries(Level level) {
        return snapshot(level).entries;
    }

    private Snapshot snapshot(Level level) {
        int identity = System.identityHashCode(level.getRecipeManager());
        Snapshot local = level.isClientSide ? clientCache : serverCache;
        if (!local.entries.isEmpty() && local.recipeIdentity == identity) return local;
        synchronized (this) {
            local = level.isClientSide ? clientCache : serverCache;
            if (!local.entries.isEmpty() && local.recipeIdentity == identity) return local;
            List<CatalogEntry> built = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                try {
                    if (!TargetEligibility.isMeaningfulDefault(item)) continue;
                    ItemStack stack = item.getDefaultInstance();
                    if (!TargetEligibility.isAllowed(level, stack)) continue;
                    built.add(entry(level, stack, null));
                } catch (RuntimeException | LinkageError ignored) {
                    // One broken third-party item must not make the whole catalogue unusable.
                }
            }
            for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
                if (!enchantment.isAllowedOnBooks()) continue;
                for (int levelValue = Math.max(1, enchantment.getMinLevel()); levelValue <= enchantment.getMaxLevel(); levelValue++) {
                    try {
                        ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, levelValue));
                        if (TargetEligibility.isAllowed(level, book)) {
                            built.add(entry(level, book, enchantment.getFullname(levelValue).getString()));
                        }
                    } catch (RuntimeException | LinkageError ignored) {}
                }
            }
            built.sort(Comparator.comparingDouble(CatalogEntry::unitValue).thenComparing(CatalogEntry::searchKey));
            List<CatalogEntry> entries = List.copyOf(built);
            List<ItemStack> stacks = entries.stream().map(CatalogEntry::stack).toList();
            Snapshot created = new Snapshot(identity, entries, stacks);
            if (level.isClientSide) clientCache = created; else serverCache = created;
            return created;
        }
    }

    public ItemStack closest(Level level, double desiredValue) {
        if (desiredValue <= 0.0D) return ItemStack.EMPTY;
        ItemStack best = ItemStack.EMPTY;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (CatalogEntry entry : entries(level)) {
            double value = entry.unitValue;
            if (value <= 0.0D) continue;
            // Upgrader 1.0 presets selected the candidate with the smallest
            // absolute value difference. Keep that exact mode behavior while
            // performing the selection authoritatively on the server.
            double distance = presetDistance(value, desiredValue);
            if (distance < bestDistance) { bestDistance = distance; best = entry.stack; }
        }
        return best.copy();
    }

    static double presetDistance(double candidateValue, double desiredValue) {
        return Math.abs(candidateValue - desiredValue);
    }

    public synchronized void invalidate() {
        clientCache = Snapshot.EMPTY;
        serverCache = Snapshot.EMPTY;
    }

    private static CatalogEntry entry(Level level, ItemStack stack, String customName) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String displayName = customName == null ? stack.getHoverName().getString() : customName;
        String searchKey = (displayName + " " + (id == null ? "" : id.toString())).toLowerCase(Locale.ROOT);
        if (stack.getItem() instanceof EnchantedBookItem) {
            for (Enchantment enchantment : net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack).keySet()) {
                ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                if (enchantmentId != null) searchKey += " " + enchantmentId;
            }
        }
        return new CatalogEntry(stack, displayName, searchKey, ValuationEngine.INSTANCE.unitValue(level, stack));
    }

    public record CatalogEntry(ItemStack stack, String displayName, String searchKey, double unitValue) {}

    private record Snapshot(int recipeIdentity, List<CatalogEntry> entries, List<ItemStack> stacks) {
        private static final Snapshot EMPTY = new Snapshot(0, List.of(), List.of());
    }
}
