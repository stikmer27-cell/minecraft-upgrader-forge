package net.execheinz.upgrader.value;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class GenericModAdapter implements ValuationAdapter {
    @Override public double floor(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || "minecraft".equals(key.getNamespace())) return 0.0D;
        String namespace = key.getNamespace().toLowerCase(Locale.ROOT);
        String path = key.getPath().toLowerCase(Locale.ROOT);
        String type = stack.getItem().getClass().getName().toLowerCase(Locale.ROOT);
        int rarity = ValuationEngine.rarityTier(stack);
        if (path.endsWith("_ore") || path.contains("_ore_")) return oreFloor(path);
        boolean wearableAbility = contains(namespace, "artifact", "relic", "curio")
                || hasToken(path, "artifact", "relic", "curio", "ring", "amulet", "charm", "talisman", "belt", "pendant", "totem")
                || contains(type, "artifact", "relic", "curio");
        boolean magic = hasToken(path, "staff", "wand", "spellbook", "grimoire", "tome", "focus") || contains(type, "spell", "magic");
        double rarityBonus = rarity == 3 ? 850.0D : rarity == 2 ? 500.0D : rarity == 1 ? 220.0D : 0.0D;
        if (wearableAbility) return (stack.getMaxStackSize() == 1 ? 700.0D : 260.0D) + rarityBonus;
        if (magic) return (stack.getMaxStackSize() == 1 ? 450.0D : 160.0D) + rarityBonus;
        if (contains(path, "legendary", "mythic", "ancient", "boss", "trophy", "unique")) return 650.0D + rarityBonus;
        if (stack.getMaxStackSize() == 1 && stack.getMaxDamage() == 0 && overridesBehavior(stack)) return 320.0D + rarityBonus;
        return 0.0D;
    }

    private static double oreFloor(String path) {
        if (path.contains("diamond")) return 880.0D;
        if (path.contains("emerald")) return 400.0D;
        if (path.contains("netherite") || path.contains("ancient")) return 1500.0D;
        if (path.contains("lapis")) return 650.0D;
        if (path.contains("copper")) return 225.0D;
        if (path.contains("redstone")) return 180.0D;
        if (path.contains("gold")) return 135.0D;
        if (path.contains("iron")) return 100.0D;
        if (path.contains("quartz")) return 80.0D;
        if (path.contains("coal")) return 35.0D;
        return 120.0D;
    }

    private static boolean overridesBehavior(ItemStack stack) {
        Class<?> type = stack.getItem().getClass();
        while (type != null && !type.getName().equals("net.minecraft.world.item.Item")) {
            if (type.getDeclaredMethods().length > 2) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static boolean hasToken(String path, String... tokens) {
        String normalized = "_" + path.replace('-', '_').replace('/', '_') + "_";
        for (String token : tokens) if (normalized.contains("_" + token + "_")) return true;
        return false;
    }
}
