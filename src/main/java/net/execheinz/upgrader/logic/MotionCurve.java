package net.execheinz.upgrader.logic;

public final class MotionCurve {
    private MotionCurve() {}

    public static float progress(float value) {
        double t = Math.max(0.0D, Math.min(1.0D, value));
        final int samples = 96;
        double sum = 0.0D;
        double total = 0.0D;
        for (int i = 0; i < samples; i++) {
            double x = (i + 0.5D) / samples;
            total += velocity(x) / samples;
            sum += velocity((i + 0.5D) * t / samples) * t / samples;
        }
        return total == 0.0D ? (float) t : (float) Math.min(1.0D, sum / total);
    }

    private static double velocity(double t) {
        if (t < 0.18D) return smoother(t / 0.18D);
        if (t < 0.62D) return 1.0D;
        return 1.0D - smoother((t - 0.62D) / 0.38D);
    }

    private static double smoother(double t) {
        t = Math.max(0.0D, Math.min(1.0D, t));
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }
}
