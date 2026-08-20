package net.execheinz.upgrader.value;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.execheinz.upgrader.UpgraderConfig;
import net.execheinz.upgrader.logic.ValueMath;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class ValuationEngine {
    public static final ValuationEngine INSTANCE = new ValuationEngine();
    private static final double MAX_VALUE = 1_000_000.0D;
    private final List<ValuationAdapter> adapters = List.of(new GenericModAdapter(), new SupportedModsAdapter());
    private volatile ValueTable cachedTable = ValueTable.EMPTY;
    private volatile int cachedRecipeIdentity;

    private ValuationEngine() {}

    public boolean isBlacklisted(Item item) {
        if (item == Items.AIR || item == Items.BARRIER || item == Items.COMMAND_BLOCK || item == Items.CHAIN_COMMAND_BLOCK || item == Items.REPEATING_COMMAND_BLOCK || item == Items.STRUCTURE_BLOCK || item == Items.STRUCTURE_VOID || item == Items.JIGSAW || item == Items.DEBUG_STICK || item == Items.KNOWLEDGE_BOOK) return true;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null || UpgraderConfig.BLACKLIST.get().contains(key.toString());
    }

    public double stackValue(Level level, ItemStack stack) {
        if (stack.isEmpty() || isBlacklisted(stack.getItem())) return 0.0D;
        double unit = unitValue(level, stack);
        double durability = 1.0D;
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) durability = Math.max(0.08D, 1.0D - (double) stack.getDamageValue() / stack.getMaxDamage());
        return clamp(unit * durability * stack.getCount());
    }

    public double unitValue(Level level, ItemStack stack) {
        if (stack.isEmpty()) return 0.0D;
        ValueTable table = table(level);
        Item item = stack.getItem();
        double generic = genericFloor(stack);
        double recipeBase = table.values.getOrDefault(item, generic);
        boolean craftable = table.crafted.contains(item);
        // Repeatably craftable items cannot be valued far above their cheapest
        // recipe or the player could manufacture casino value forever. Combat,
        // rarity and mod ability floors remain fully active for drop-only gear;
        // for recipe outputs they receive only a small quality allowance.
        double floor = craftable ? 0.0D : generic;
        try { floor = Math.max(floor, combatFloor(stack)); } catch (RuntimeException | LinkageError ignored) {}
        for (ValuationAdapter adapter : adapters) {
            try { floor = Math.max(floor, adapter.floor(stack)); } catch (RuntimeException | LinkageError ignored) {}
        }
        if (craftable) floor = ValueMath.boundedCraftableFloor(recipeBase, floor);
        Double override = ManualOverrides.lookup(item);
        double base = override != null ? override : Math.max(recipeBase, floor);
        double enchantments;
        try { enchantments = enchantmentPremium(stack); } catch (RuntimeException | LinkageError ignored) { enchantments = 0.0D; }
        double nbt;
        try { nbt = nbtPremium(stack, base); } catch (RuntimeException | LinkageError ignored) { nbt = 0.0D; }
        double raw = base + enchantments + nbt;
        if (override == null) raw = ValueMath.softCap(raw, rarityTier(stack), stack.getMaxStackSize() == 1);
        return clamp(raw);
    }

    public double enchantmentPremium(ItemStack stack) {
        double total = 0.0D;
        int counted = 0;
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            if (counted++ >= 24) break;
            Enchantment enchantment = entry.getKey();
            int level = Math.max(1, Math.min(20, entry.getValue()));
            Enchantment.Rarity enchantmentRarity = enchantment.getRarity();
            double rarity;
            if (enchantmentRarity == Enchantment.Rarity.VERY_RARE) rarity = 3.2D;
            else if (enchantmentRarity == Enchantment.Rarity.RARE) rarity = 2.1D;
            else if (enchantmentRarity == Enchantment.Rarity.UNCOMMON) rarity = 1.45D;
            else rarity = 1.0D;
            double scarcity = enchantment.getMaxLevel() <= 1 ? 1.8D : 1.0D + 0.6D * level / enchantment.getMaxLevel();
            double special = (enchantment.isTreasureOnly() ? 2.5D : 1.0D) * (enchantment.isCurse() ? 0.2D : 1.0D);
            total += 70.0D * Math.pow(level, 1.65D) * rarity * scarcity * special;
        }
        return Math.min(50_000.0D, total);
    }

    private ValueTable table(Level level) {
        int identity = System.identityHashCode(level.getRecipeManager());
        ValueTable local = cachedTable;
        if (!local.values.isEmpty() && cachedRecipeIdentity == identity) return local;
        synchronized (this) {
            if (!cachedTable.values.isEmpty() && cachedRecipeIdentity == identity) return cachedTable;
            cachedTable = compute(level);
            cachedRecipeIdentity = identity;
            return cachedTable;
        }
    }

    public synchronized void invalidate() {
        cachedTable = ValueTable.EMPTY;
        cachedRecipeIdentity = 0;
        TargetCatalog.INSTANCE.invalidate();
    }

    private ValueTable compute(Level level) {
        Map<Item, Double> values = new HashMap<>();
        Set<Item> pinned = new HashSet<>();
        seed(values, pinned);
        for (Item item : ForgeRegistries.ITEMS) values.putIfAbsent(item, defaultFloor(item));
        List<RecipeEdge> edges = recipeEdges(level);
        Set<Item> crafted = new HashSet<>();
        for (RecipeEdge edge : edges) crafted.add(edge.result);
        for (int pass = 0; pass < 48; pass++) {
            boolean changed = false;
            Map<Item, Double> cheapestRecipes = new HashMap<>();
            for (RecipeEdge edge : edges) {
                double cost = edge.cost(values);
                if (!Double.isFinite(cost) || cost <= 0.0D) continue;
                if (!pinned.contains(edge.result)) cheapestRecipes.merge(edge.result, cost, Math::min);
                if (edge.groups.size() == 1) {
                    double reverseFloor = ValueMath.reverseConversionFloor(values.getOrDefault(edge.result, cost), edge.count);
                    for (Item source : edge.groups.get(0)) {
                        double current = values.getOrDefault(source, 0.0D);
                        if (!pinned.contains(source) && reverseFloor > current + 0.001D) {
                            values.put(source, reverseFloor);
                            changed = true;
                        }
                    }
                }
            }
            for (Map.Entry<Item, Double> candidate : cheapestRecipes.entrySet()) {
                // Rarity is not free value when the item has a repeatable
                // recipe. The recipe graph is the economic anchor; item-stack
                // enchantments and meaningful NBT are applied later.
                double next = candidate.getValue();
                double current = values.getOrDefault(candidate.getKey(), next);
                if (Math.abs(current - next) > 0.001D) { values.put(candidate.getKey(), next); changed = true; }
            }
            if (!changed) break;
        }
        return new ValueTable(Map.copyOf(values), Set.copyOf(crafted));
    }

    private List<RecipeEdge> recipeEdges(Level level) {
        RegistryAccess access = level.registryAccess();
        List<RecipeEdge> edges = new ArrayList<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            try {
                if (recipe.isSpecial()) continue;
                ItemStack result = recipe.getResultItem(access);
                if (result.isEmpty() || result.getCount() <= 0 || isBlacklisted(result.getItem())) continue;
                List<List<Item>> groups = new ArrayList<>();
                boolean valid = true;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    List<Item> alternatives = new ArrayList<>();
                    for (ItemStack option : ingredient.getItems()) if (!option.isEmpty() && !isBlacklisted(option.getItem())) alternatives.add(option.getItem());
                    if (alternatives.isEmpty()) { valid = false; break; }
                    groups.add(alternatives);
                }
                if (valid && !groups.isEmpty()) edges.add(new RecipeEdge(result.getItem(), result.getCount(), groups));
            } catch (RuntimeException | LinkageError ignored) {
                // A malformed third-party recipe must not make inserting an item crash the server.
            }
        }
        return edges;
    }

    private static double genericFloor(ItemStack stack) {
        if (stack.isEmpty()) return 0.0D;
        // Do not compile a Java enum-switch over Minecraft classes here.
        // Forge runtime enum extension (used by large modpacks/Apotheosis)
        // can make the synthetic switch map fail with
        // IncompatibleClassChangeError after class transformation.
        Item item = stack.getItem();
        try {
            double rarity = rarityFloor(stack);
            double category;
            if (item instanceof BlockItem) category = 12.0D;
            else if (stack.getFoodProperties(null) != null) category = 35.0D;
            else if (stack.getMaxStackSize() > 1) category = 30.0D;
            else if (stack.isDamageableItem()) category = 140.0D;
            else category = 220.0D;
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            if (key != null && !"minecraft".equals(key.getNamespace())) category *= 1.15D;
            return Math.max(category, rarity);
        } catch (RuntimeException | LinkageError ignored) {
            return genericFallback(item);
        }
    }

    private static double defaultFloor(Item item) {
        try { return genericFloor(item.getDefaultInstance()); }
        catch (RuntimeException | LinkageError ignored) { return genericFallback(item); }
    }

    private static double rarityFloor(ItemStack stack) {
        int tier = rarityTier(stack);
        if (tier >= 3) return 1200.0D;
        if (tier == 2) return 500.0D;
        if (tier == 1) return 180.0D;
        return 0.0D;
    }

    private static double genericFallback(Item item) {
        double base = item instanceof BlockItem ? 12.0D : 140.0D;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && !"minecraft".equals(key.getNamespace()) ? base * 1.15D : base;
    }

    private static double combatFloor(ItemStack stack) {
        double damage = attributeValue(stack, Attributes.ATTACK_DAMAGE, 1.0D);
        double speed = Math.max(0.1D, attributeValue(stack, Attributes.ATTACK_SPEED, 4.0D));
        double dps = Math.max(0.0D, damage * speed);
        double weapon = ValueMath.weaponFloor(dps, stack.getMaxDamage());
        double armor = 0.0D;
        if (stack.getItem() instanceof ArmorItem armorItem) {
            armor = ValueMath.armorFloor(armorItem.getDefense(), armorItem.getToughness(),
                armorItem.getMaterial().getKnockbackResistance(), stack.getMaxDamage());
        }
        return Math.max(weapon, armor);
    }

    private static double attributeValue(ItemStack stack, Attribute attribute, double base) {
        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        double additive = 0.0D, multiplyBase = 0.0D, multiplyTotal = 1.0D;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            AttributeModifier.Operation operation = modifier.getOperation();
            if (operation == AttributeModifier.Operation.ADDITION) additive += modifier.getAmount();
            else if (operation == AttributeModifier.Operation.MULTIPLY_BASE) multiplyBase += modifier.getAmount();
            else if (operation == AttributeModifier.Operation.MULTIPLY_TOTAL) multiplyTotal *= 1.0D + modifier.getAmount();
        }
        return Math.max(0.0D, (base + additive + base * multiplyBase) * multiplyTotal);
    }

    static int rarityTier(ItemStack stack) {
        try {
            Rarity rarity = stack.getRarity();
            if (rarity == Rarity.EPIC) return 3;
            if (rarity == Rarity.RARE) return 2;
            if (rarity == Rarity.UNCOMMON) return 1;
        } catch (RuntimeException | LinkageError ignored) {}
        return 0;
    }

    private static double nbtPremium(ItemStack original, double base) {
        if (!original.hasTag()) return 0.0D;
        CompoundTag tag = original.getTag().copy();
        tag.remove("Damage"); tag.remove("Enchantments"); tag.remove("StoredEnchantments"); tag.remove("RepairCost");
        // Player-authored/cosmetic NBT must never create casino value. Names,
        // books and map decorations are cheap to duplicate and can contain an
        // arbitrary amount of text.
        tag.remove("display"); tag.remove("HideFlags"); tag.remove("CustomModelData");
        tag.remove("pages"); tag.remove("filtered_pages"); tag.remove("author"); tag.remove("title");
        tag.remove("resolved"); tag.remove("generation"); tag.remove("map"); tag.remove("Decorations");
        tag.remove("CanDestroy"); tag.remove("CanPlaceOn");
        if (tag.isEmpty()) return 0.0D;
        String text = tag.toString().toLowerCase(Locale.ROOT);
        double premium = Math.min(1200.0D, Math.sqrt(text.length()) * 28.0D);
        if (text.contains("affix") || text.contains("apotheosis")) premium += 700.0D;
        if (text.contains("socket")) premium += 450.0D;
        if (text.contains("gem")) premium += 550.0D;
        if (text.contains("spell") || text.contains("ability")) premium += 400.0D;
        return Math.min(base * 0.75D + 2500.0D, premium);
    }

    private static void seed(Map<Item, Double> values, Set<Item> pinned) {
        put(values, pinned, 1, Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL);
        put(values, pinned, 4, Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG);
        put(values, pinned, 15, Items.COAL, Items.CHARCOAL);
        put(values, pinned, 30, Items.COPPER_INGOT, Items.REDSTONE);
        put(values, pinned, 45, Items.IRON_INGOT, Items.LAPIS_LAZULI);
        put(values, pinned, 60, Items.GOLD_INGOT);
        put(values, pinned, 180, Items.EMERALD);
        put(values, pinned, 400, Items.DIAMOND);
        put(values, pinned, 1500, Items.NETHERITE_SCRAP);
        put(values, pinned, 2500, Items.NETHERITE_INGOT);
        put(values, pinned, 1800, Items.ELYTRA);
        put(values, pinned, 1500, Items.TOTEM_OF_UNDYING);
        put(values, pinned, 1400, Items.NETHER_STAR);
        // Ore blocks are obtainable with Silk Touch and can then be mined with
        // Fortune. Price the best normal survival conversion, not the bare block.
        put(values, pinned, 35, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE);
        put(values, pinned, 225, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE);
        put(values, pinned, 100, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE);
        put(values, pinned, 135, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE);
        put(values, pinned, 180, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE);
        put(values, pinned, 650, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE);
        put(values, pinned, 880, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE);
        put(values, pinned, 400, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE);
        put(values, pinned, 80, Items.NETHER_QUARTZ_ORE);
        put(values, pinned, 55, Items.NETHER_GOLD_ORE);
        put(values, pinned, 1500, Items.ANCIENT_DEBRIS);
    }

    private static void put(Map<Item, Double> values, Set<Item> pinned, double value, Item... items) { for (Item item : items) { values.put(item, value); pinned.add(item); } }
    private static double clamp(double value) { return Math.max(0.0D, Math.min(MAX_VALUE, value)); }

    private record RecipeEdge(Item result, int count, List<List<Item>> groups) {
        double cost(Map<Item, Double> values) {
            double sum = 0.0D;
            for (List<Item> group : groups) {
                double cheapest = Double.POSITIVE_INFINITY;
                for (Item item : group) cheapest = Math.min(cheapest, values.getOrDefault(item, Double.POSITIVE_INFINITY));
                if (!Double.isFinite(cheapest)) return Double.POSITIVE_INFINITY;
                sum += cheapest;
            }
            return ValueMath.recipeUnitCost(sum, count);
        }
    }

    private record ValueTable(Map<Item, Double> values, Set<Item> crafted) {
        private static final ValueTable EMPTY = new ValueTable(Map.of(), Set.of());
    }
}
