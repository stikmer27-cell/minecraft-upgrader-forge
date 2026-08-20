package net.execheinz.upgrader.logic;

public final class OddsPolicy {
    private OddsPolicy() {}

    public static double chance(double inputValue, double targetValue, double playerFactor, double min, double max, int lossStreak) {
        if (!Double.isFinite(inputValue) || !Double.isFinite(targetValue) || inputValue <= 0.0D || targetValue <= 0.0D) return 0.0D;
        double base = playerFactor * inputValue / targetValue;
        double protection = lossStreak < 3 ? 0.0D : Math.min(0.12D, (lossStreak - 2) * 0.02D);
        return Math.max(min, Math.min(max, base + protection));
    }
}
