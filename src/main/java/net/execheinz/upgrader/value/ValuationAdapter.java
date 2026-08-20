package net.execheinz.upgrader.value;

import net.minecraft.world.item.ItemStack;

public interface ValuationAdapter {
    double floor(ItemStack stack);
}
