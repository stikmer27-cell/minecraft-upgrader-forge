package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValueMathTest {
    @Test
    void oneToManyConversionCannotCreateCheapStorageInputs() {
        assertEquals(60.0D / 9.0D, ValueMath.recipeUnitCost(60.0D, 9), 1.0E-9);
        assertEquals(3600.0D, ValueMath.reverseConversionFloor(400.0D, 9), 1.0E-9);
        assertEquals(400.0D, ValueMath.reverseConversionFloor(400.0D, 1), 1.0E-9);
    }

    @Test
    void repeatableRecipesBoundCombatAndRarityPremiums() {
        assertEquals(108.0D, ValueMath.boundedCraftableFloor(100.0D, 1800.0D), 1.0E-9);
        assertEquals(70.0D, ValueMath.boundedCraftableFloor(100.0D, 70.0D), 1.0E-9);
        assertEquals(0.0D, ValueMath.boundedCraftableFloor(0.0D, 700.0D), 1.0E-9);
    }

    @Test
    void ordinaryWeaponsStayBelowExceptionalModdedWeapons() {
        double vanillaLike = ValueMath.weaponFloor(9.6D, 250);
        double modded = ValueMath.weaponFloor(25.0D, 1800);
        assertTrue(vanillaLike < 400.0D);
        assertTrue(modded > vanillaLike * 3.0D);
        assertTrue(modded < 3000.0D);
    }

    @Test
    void armorCurveIsMonotonicAndBounded() {
        double modest = ValueMath.armorFloor(4.0D, 0.0D, 0.0D, 150);
        double strong = ValueMath.armorFloor(8.0D, 3.0D, 0.1D, 600);
        assertTrue(modest >= 300.0D && modest < 500.0D);
        assertTrue(strong > modest);
        assertTrue(strong < 2000.0D);
    }

    @Test
    void extremeRecipeValuesAreCompressedInsteadOfFlattened() {
        double common = ValueMath.softCap(1_000_000.0D, 0, false);
        double epic = ValueMath.softCap(1_000_000.0D, 3, true);
        assertTrue(common > 25_000.0D && common < 100_000.0D);
        assertTrue(epic > common && epic <= 200_000.0D);
    }
}
