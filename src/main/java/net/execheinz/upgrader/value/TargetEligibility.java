package net.execheinz.upgrader.value;

import java.util.Locale;
import net.execheinz.upgrader.UpgraderConfig;
import net.execheinz.upgrader.UpgraderMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-authoritative rules for items that may be selected as upgrade rewards. */
public final class TargetEligibility {
    private TargetEligibility() {}

    public static boolean isAllowed(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        try {
            Item item = stack.getItem();
            if (ValuationEngine.INSTANCE.isBlacklisted(item)) return false;
            if (!item.isEnabled(level.enabledFeatures())) return false;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) return false;
            if (UpgraderConfig.TARGET_ALLOWLIST.get().contains(id.toString())) return true;
            if (item instanceof SpawnEggItem) return false;
            if (item instanceof EnchantedBookItem && EnchantmentHelper.getEnchantments(stack).isEmpty()) return false;
            if (stack.is(Tags.UNOBTAINABLE)) return false;
            if (item instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) < 0.0F) return false;
            return !isTechnicalPath(id.getPath());
        } catch (RuntimeException | LinkageError ignored) {
            // A malformed third-party item is safer to hide than to let it crash a menu.
            return false;
        }
    }

    /** Filled portable inventories are rejected as wagers: their nested
     * contents cannot be priced safely without each mod's capability API. */
    public static boolean isAllowedWager(Level level, ItemStack stack) {
        boolean allowed = isAllowed(level, stack);
        if (!allowed || !stack.hasTag()) return allowed;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (!(path.contains("backpack") || path.contains("shulker") || path.contains("bundle")
            || path.contains("satchel") || path.contains("pouch") || path.endsWith("_bag") || path.startsWith("bag_"))) return true;
        return !hasNestedInventory(stack.getTag(), 0);
    }

    private static boolean hasNestedInventory(Tag tag, int depth) {
        if (tag == null || depth > 4) return false;
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                String normalized = key.toLowerCase(Locale.ROOT);
                if (normalized.equals("items") || normalized.equals("contents") || normalized.equals("storageuuid")
                    || normalized.contains("inventory")) return true;
                if (hasNestedInventory(compound.get(key), depth + 1)) return true;
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < Math.min(64, list.size()); i++) {
                if (hasNestedInventory(list.get(i), depth + 1)) return true;
            }
        }
        return false;
    }

    /** Empty enchanted books are invalid rewards; generated books with a real enchantment remain allowed. */
    public static boolean isMeaningfulDefault(Item item) {
        return !(item instanceof EnchantedBookItem);
    }

    static boolean isTechnicalPath(String path) {
        String value = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return technicalToken(value, "unobtainable")
            || technicalToken(value, "technical")
            || technicalToken(value, "debug")
            || technicalToken(value, "developer")
            || technicalToken(value, "placeholder")
            || technicalToken(value, "dummy")
            || technicalToken(value, "admin")
            || technicalToken(value, "operator")
            || value.startsWith("dev_")
            || value.startsWith("creative_")
            || value.endsWith("_creative_only");
    }

    private static boolean technicalToken(String path, String token) {
        return path.equals(token) || path.startsWith(token + "_") || path.endsWith("_" + token);
    }

    private static final class Tags {
        private static final TagKey<Item> UNOBTAINABLE = TagKey.create(Registries.ITEM,
            new ResourceLocation(UpgraderMod.MOD_ID, "unobtainable"));
    }
}
