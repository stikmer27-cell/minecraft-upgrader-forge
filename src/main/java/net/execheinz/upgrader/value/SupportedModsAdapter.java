package net.execheinz.upgrader.value;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class SupportedModsAdapter implements ValuationAdapter {
    @Override public double floor(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return 0.0D;
        String namespace = key.getNamespace().toLowerCase(Locale.ROOT);
        String path = key.getPath().toLowerCase(Locale.ROOT);
        boolean unique = stack.getMaxStackSize() == 1;
        int rarity = ValuationEngine.rarityTier(stack);
        double rarityBonus = rarity == 3 ? 900.0D : rarity == 2 ? 520.0D : rarity == 1 ? 240.0D : 0.0D;
        if (namespace.equals("artifacts")) return (unique ? 800.0D : 260.0D) + rarityBonus;
        if (namespace.equals("relics")) return (unique ? 1050.0D : 320.0D) + rarityBonus;
        if ((namespace.contains("apotheosis") || namespace.contains("apothic")) && contains(path, "gem", "affix", "socket", "sigil")) return 450.0D + rarityBonus;
        if (namespace.contains("irons_spellbooks")) return (unique ? 480.0D : 140.0D) + rarityBonus;
        if (namespace.contains("simplyswords")) return contains(path, "runic", "unique", "legendary") ? 650.0D + rarityBonus : 0.0D;
        if (namespace.contains("mowzie") || namespace.contains("alexscaves") || namespace.contains("born_in_chaos") || namespace.contains("mutant") || namespace.contains("passive_skill")) {
            double scarcity = contains(path, "boss", "trophy", "heart", "core", "essence", "relic") ? 350.0D : 0.0D;
            return (unique ? 380.0D : 70.0D) + rarityBonus + scarcity;
        }
        return 0.0D;
    }

    private static boolean contains(String value, String... parts) {
        for (String part : parts) if (value.contains(part)) return true;
        return false;
    }
}
