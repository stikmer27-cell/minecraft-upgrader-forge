package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CelebrationCurveTest {
    @Test
    void celebrationHasStrictBoundedLifetime() {
        assertFalse(CelebrationCurve.isActive(-1L));
        assertTrue(CelebrationCurve.isActive(0L));
        assertTrue(CelebrationCurve.isActive(CelebrationCurve.DURATION_MS - 1L));
        assertFalse(CelebrationCurve.isActive(CelebrationCurve.DURATION_MS));
    }

    @Test
    void particlesDoNotRenderOutsideTheirOwnWindow() {
        assertEquals(-1.0F, CelebrationCurve.particleAge(99L, 100, 500));
        assertEquals(0.0F, CelebrationCurve.particleAge(100L, 100, 500));
        assertEquals(0.5F, CelebrationCurve.particleAge(350L, 100, 500));
        assertEquals(-1.0F, CelebrationCurve.particleAge(600L, 100, 500));
    }

    @Test
    void alphaFallsSmoothlyToZero() {
        float start = CelebrationCurve.particleAlpha(0.0F);
        float middle = CelebrationCurve.particleAlpha(0.5F);
        float end = CelebrationCurve.particleAlpha(0.99F);
        assertTrue(start > middle);
        assertTrue(middle > end);
        assertEquals(0.0F, CelebrationCurve.particleAlpha(1.0F));
    }
}
