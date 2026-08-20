package net.execheinz.upgrader.logic;

/** Pure animation timing helpers kept independent from the Minecraft client. */
public final class CelebrationCurve {
    public static final long DURATION_MS = 3000L;

    private CelebrationCurve() {}

    public static boolean isActive(long elapsedMs) {
        return elapsedMs >= 0L && elapsedMs < DURATION_MS;
    }

    /** Returns -1 outside a particle's lifetime, otherwise a normalized 0..1 age. */
    public static float particleAge(long elapsedMs, int delayMs, int lifetimeMs) {
        long local = elapsedMs - delayMs;
        if (local < 0L || local >= lifetimeMs || lifetimeMs <= 0) return -1.0F;
        return local / (float) lifetimeMs;
    }

    public static float particleAlpha(float age) {
        if (age < 0.0F || age >= 1.0F) return 0.0F;
        float remaining = 1.0F - age;
        return remaining * remaining * (0.82F + 0.18F * remaining);
    }

    public static float launchProgress(long elapsedMs, int burstDelayMs, int launchDurationMs) {
        if (launchDurationMs <= 0) return 1.0F;
        float progress = (elapsedMs - (burstDelayMs - launchDurationMs)) / (float) launchDurationMs;
        if (progress <= 0.0F) return 0.0F;
        if (progress >= 1.0F) return 1.0F;
        return 1.0F - (1.0F - progress) * (1.0F - progress);
    }
}
