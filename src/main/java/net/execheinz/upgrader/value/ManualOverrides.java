package net.execheinz.upgrader.value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.execheinz.upgrader.UpgraderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class ManualOverrides {
    private static volatile int cachedHash;
    private static volatile int cachedSize = -1;
    private static volatile Map<ResourceLocation, Double> cached = Map.of();

    private ManualOverrides() {}

    public static Double lookup(Item item) {
        return configured().get(ForgeRegistries.ITEMS.getKey(item));
    }

    private static Map<ResourceLocation, Double> configured() {
        List<? extends String> source = UpgraderConfig.VALUE_OVERRIDES.get();
        int hash = source.hashCode();
        Map<ResourceLocation, Double> local = cached;
        if (cachedSize == source.size() && cachedHash == hash) return local;
        synchronized (ManualOverrides.class) {
            if (cachedSize == source.size() && cachedHash == hash) return cached;
        Map<ResourceLocation, Double> result = new HashMap<>();
        for (String entry : source) {
            int split = entry.lastIndexOf('=');
            if (split <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, split).trim());
            try {
                if (id != null) result.put(id, Math.max(1.0D, Double.parseDouble(entry.substring(split + 1).trim())));
            } catch (NumberFormatException ignored) {}
        }
            cached = Map.copyOf(result);
            cachedSize = source.size();
            cachedHash = hash;
            return cached;
        }
    }
}
