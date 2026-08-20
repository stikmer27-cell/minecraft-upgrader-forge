package net.execheinz.upgrader.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TargetCatalogTest {
    @Test
    void presetsUseOriginalOnePointZeroAbsoluteDistance() {
        assertEquals(40.0D, TargetCatalog.presetDistance(240.0D, 200.0D), 1.0E-9);
        assertTrue(TargetCatalog.presetDistance(240.0D, 200.0D)
            < TargetCatalog.presetDistance(150.0D, 200.0D));
    }

    @Test
    void technicalModItemsAreRejectedConservatively() {
        assertTrue(TargetEligibility.isTechnicalPath("debug_wand"));
        assertTrue(TargetEligibility.isTechnicalPath("machine_placeholder"));
        assertTrue(TargetEligibility.isTechnicalPath("creative_energy_cell"));
        assertFalse(TargetEligibility.isTechnicalPath("diamond_sword"));
        assertFalse(TargetEligibility.isTechnicalPath("artifact_of_power"));
    }
}
