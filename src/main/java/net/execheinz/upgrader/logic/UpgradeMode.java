package net.execheinz.upgrader.logic;

public enum UpgradeMode {
    X2("x2"), X4("x4"), X8("x8"), CHANCE_30("30%"), CHANCE_50("50%"), CHANCE_70("70%");

    private final String label;

    UpgradeMode(String label) { this.label = label; }
    public String label() { return label; }

    public double desiredTargetValue(double inputValue, double playerFactor) {
        return switch (this) {
            case X2 -> inputValue * 2.0D;
            case X4 -> inputValue * 4.0D;
            case X8 -> inputValue * 8.0D;
            case CHANCE_30 -> inputValue * playerFactor / 0.30D;
            case CHANCE_50 -> inputValue * playerFactor / 0.50D;
            case CHANCE_70 -> inputValue * playerFactor / 0.70D;
        };
    }
}
