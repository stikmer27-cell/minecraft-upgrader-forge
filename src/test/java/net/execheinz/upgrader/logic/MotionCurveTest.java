package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MotionCurveTest {
    @Test void curveIsMonotonicAndEndsWithoutSnap() {
        assertEquals(0.0F, MotionCurve.progress(0.0F), 1.0E-5F);
        assertEquals(1.0F, MotionCurve.progress(1.0F), 1.0E-5F);
        float previous = 0.0F;
        for (int i = 1; i <= 1000; i++) {
            float current = MotionCurve.progress(i / 1000.0F);
            assertTrue(current >= previous);
            previous = current;
        }
        assertTrue(MotionCurve.progress(0.01F) < 0.001F);
        assertTrue(1.0F - MotionCurve.progress(0.99F) < 0.001F);
    }
}
