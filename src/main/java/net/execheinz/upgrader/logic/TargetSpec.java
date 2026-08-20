package net.execheinz.upgrader.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

public record TargetSpec(ResourceLocation itemId, ResourceLocation enchantmentId, int enchantmentLevel, int amount) {
    public static TargetSpec normal(ItemStack stack, int amount) {
        return new TargetSpec(ForgeRegistries.ITEMS.getKey(stack.getItem()), null, 0, amount);
    }

    public ItemStack createValidatedStack() {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return ItemStack.EMPTY;
        if (enchantmentId == null) return new ItemStack(item);
        if (!(item instanceof EnchantedBookItem)) return ItemStack.EMPTY;
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        if (enchantment == null || !enchantment.isAllowedOnBooks()
            || enchantmentLevel < Math.max(1, enchantment.getMinLevel()) || enchantmentLevel > enchantment.getMaxLevel()) return ItemStack.EMPTY;
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, enchantmentLevel));
    }
}
