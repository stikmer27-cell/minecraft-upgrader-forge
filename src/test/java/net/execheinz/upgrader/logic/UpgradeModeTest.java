package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class UpgradeModeTest {
    @Test void multipliersRemainUpgradeModes() {
        assertEquals(200, UpgradeMode.X2.desiredTargetValue(100, 1.03), 1.0E-9);
        assertEquals(400, UpgradeMode.X4.desiredTargetValue(100, 1.03), 1.0E-9);
        assertEquals(800, UpgradeMode.X8.desiredTargetValue(100, 1.03), 1.0E-9);
    }
    @Test void percentageModesDescribeDesiredOddsNotRewardAmount() {
        assertEquals(100 * 1.03 / 0.30, UpgradeMode.CHANCE_30.desiredTargetValue(100, 1.03), 1.0E-9);
        assertEquals(100 * 1.03 / 0.50, UpgradeMode.CHANCE_50.desiredTargetValue(100, 1.03), 1.0E-9);
        assertEquals(100 * 1.03 / 0.70, UpgradeMode.CHANCE_70.desiredTargetValue(100, 1.03), 1.0E-9);
    }
}
