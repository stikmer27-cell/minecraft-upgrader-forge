package net.execheinz.upgrader.logic;

/** Pure, testable balance curves used by the valuation engine. */
public final class ValueMath {
    private ValueMath() {}

    public static double recipeUnitCost(double ingredientTotal, int resultCount) {
        if (!Double.isFinite(ingredientTotal) || ingredientTotal <= 0.0D || resultCount <= 0) return 0.0D;
        return ingredientTotal / resultCount;
    }

    public static double reverseConversionFloor(double resultUnitValue, int resultCount) {
        if (!Double.isFinite(resultUnitValue) || resultUnitValue <= 0.0D || resultCount <= 0) return 0.0D;
        return resultUnitValue * resultCount;
    }

    public static double boundedCraftableFloor(double recipeValue, double featureFloor) {
        if (!Double.isFinite(recipeValue) || recipeValue <= 0.0D) return 0.0D;
        if (!Double.isFinite(featureFloor) || featureFloor <= 0.0D) return 0.0D;
        return Math.min(featureFloor, recipeValue * 1.08D);
    }

    public static double weaponFloor(double dps, int durability) {
        double safeDps = Math.max(0.0D, Math.min(80.0D, dps));
        double useful = Math.max(0.0D, safeDps - 5.0D);
        double exceptional = Math.pow(Math.max(0.0D, safeDps - 12.0D), 1.25D) * 25.0D;
        return useful <= 0.0D ? 0.0D : 20.0D + useful * 32.0D + exceptional
            + Math.sqrt(Math.max(0, durability)) * 2.0D;
    }

    public static double armorFloor(double defense, double toughness, double knockbackResistance, int durability) {
        double safeDefense = Math.max(0.0D, Math.min(40.0D, defense));
        double safeToughness = Math.max(0.0D, Math.min(30.0D, toughness));
        double safeKnockback = Math.max(0.0D, Math.min(1.0D, knockbackResistance));
        if (safeDefense <= 0.0D && safeToughness <= 0.0D && safeKnockback <= 0.0D) return 0.0D;
        return Math.pow(safeDefense, 1.5D) * 40.0D + safeToughness * 90.0D
            + safeKnockback * 700.0D + Math.sqrt(Math.max(0, durability)) * 2.5D;
    }

    public static double softCap(double value, int rarityTier, boolean unique) {
        double threshold;
        if (rarityTier >= 3) threshold = 140_000.0D;
        else if (rarityTier == 2) threshold = 85_000.0D;
        else if (rarityTier == 1) threshold = 50_000.0D;
        else threshold = unique ? 40_000.0D : 25_000.0D;
        if (value <= threshold) return Math.max(0.0D, value);
        return Math.min(200_000.0D, threshold + Math.sqrt(value - threshold) * 45.0D);
    }
}
