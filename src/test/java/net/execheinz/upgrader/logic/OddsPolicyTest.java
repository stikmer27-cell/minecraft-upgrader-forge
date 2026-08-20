package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class OddsPolicyTest {
    @Test void fairRatioUsesPlayerFactor() { assertEquals(0.515D, OddsPolicy.chance(500, 1000, 1.03, 0.005, 0.98, 0), 1.0E-9); }
    @Test void capsAreEnforced() {
        assertEquals(0.98D, OddsPolicy.chance(10_000, 1, 1.03, 0.005, 0.98, 0));
        assertEquals(0.005D, OddsPolicy.chance(1, 10_000, 1.03, 0.005, 0.98, 0));
    }
    @Test void lossProtectionStartsAfterTwoLossesAndCapsAtTwelvePoints() {
        assertEquals(0.50D, OddsPolicy.chance(50, 100, 1.0, 0.0, 1.0, 2), 1.0E-9);
        assertEquals(0.52D, OddsPolicy.chance(50, 100, 1.0, 0.0, 1.0, 3), 1.0E-9);
        assertEquals(0.62D, OddsPolicy.chance(50, 100, 1.0, 0.0, 1.0, 99), 1.0E-9);
    }
    @Test void invalidValuesCannotStartAnAttempt() { assertEquals(0.0D, OddsPolicy.chance(0, 100, 1.03, 0.005, 0.98, 0)); }
}
