package net.execheinz.upgrader.logic;

import net.minecraft.world.item.ItemStack;

public final class ResultAmountPolicy {
    public static final int HARD_MAX = 64;

    private ResultAmountPolicy() {}

    public static int normalize(ItemStack target, int requested, int configuredMax) {
        if (!needsPopup(target)) return 1;
        return Math.max(1, Math.min(maximum(configuredMax), requested));
    }

    public static int maximum(int configuredMax) {
        return Math.max(1, Math.min(HARD_MAX, configuredMax));
    }

    public static boolean needsPopup(ItemStack target) {
        return !target.isEmpty() && target.isStackable() && target.getMaxStackSize() > 1;
    }
}
