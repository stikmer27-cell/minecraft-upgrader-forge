package net.execheinz.upgrader.client;

import java.util.Random;
import net.execheinz.upgrader.logic.CelebrationCurve;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** A bounded, allocation-free-per-frame celebration rendered only inside the GUI. */
final class CasinoFireworks {
    private static final int LAUNCH_MS = 230;
    private static final int PARTICLES_PER_BURST = 24;
    private static final int[] BURST_X = {139, 199, 168, 124, 213};
    private static final int[] BURST_Y = {45, 43, 25, 78, 76};
    private static final int[] BURST_DELAY = {180, 520, 850, 1190, 1510};
    private static final int[] PALETTE = {
        0xFFFFCA35, 0xFFFFED9A, 0xFFFFFFFF, 0xFF42D889, 0xFFFF9F2F
    };
    private static final Spark[] SPARKS = createSparks();

    private CasinoFireworks() {}

    static void render(GuiGraphics graphics, int rootX, int rootY, long elapsedMs) {
        if (!CelebrationCurve.isActive(elapsedMs)) return;

        graphics.pose().pushPose();
        renderLaunchTrails(graphics, rootX, rootY, elapsedMs);

        for (int burst = 0; burst < BURST_X.length; burst++) {
            long burstAge = elapsedMs - BURST_DELAY[burst];
            if (burstAge >= 0L && burstAge < 150L) {
                float flash = 1.0F - burstAge / 150.0F;
                int alpha = Mth.clamp((int) (150.0F * flash), 0, 150);
                CasinoRenderer.glow(graphics, rootX + BURST_X[burst] - 3, rootY + BURST_Y[burst] - 3,
                    6, 6, 3, alpha << 24 | 0x00FFDC68);
                int core = Mth.clamp((int) (255.0F * flash), 0, 255) << 24 | 0x00FFFFFF;
                cross(graphics, rootX + BURST_X[burst], rootY + BURST_Y[burst], 2, core);
            }
        }

        for (Spark spark : SPARKS) {
            float age = CelebrationCurve.particleAge(elapsedMs, spark.delayMs, spark.lifetimeMs);
            if (age < 0.0F) continue;

            float seconds = (elapsedMs - spark.delayMs) / 1000.0F;
            int x = Math.round(rootX + spark.originX + spark.velocityX * seconds);
            int y = Math.round(rootY + spark.originY + spark.velocityY * seconds
                + 0.5F * spark.gravity * seconds * seconds);
            float twinkle = 0.72F + 0.28F * Math.abs(Mth.sin(age * 20.0F + spark.twinkle));
            int alpha = Mth.clamp((int) (255.0F * CelebrationCurve.particleAlpha(age) * twinkle), 0, 255);
            int color = alpha << 24 | spark.rgb;
            int size = spark.size;

            graphics.fill(x, y, x + size, y + size, color);
            if (spark.star && age < 0.62F) cross(graphics, x, y, 1, color);

            // A single dim trail point gives motion without a particle engine or allocations.
            if (age < 0.72F) {
                int trailAlpha = alpha / 3;
                int trailX = Math.round(x - spark.velocityX * 0.045F);
                int trailY = Math.round(y - (spark.velocityY + spark.gravity * seconds) * 0.045F);
                graphics.fill(trailX, trailY, trailX + 1, trailY + 1, trailAlpha << 24 | spark.rgb);
            }
        }
        graphics.pose().popPose();
    }

    private static void renderLaunchTrails(GuiGraphics graphics, int rootX, int rootY, long elapsedMs) {
        for (int burst = 0; burst < BURST_X.length; burst++) {
            long launchStart = BURST_DELAY[burst] - LAUNCH_MS;
            if (elapsedMs < launchStart || elapsedMs >= BURST_DELAY[burst]) continue;
            float progress = CelebrationCurve.launchProgress(elapsedMs, BURST_DELAY[burst], LAUNCH_MS);
            int x = rootX + BURST_X[burst];
            int startY = rootY + 118;
            int y = Math.round(Mth.lerp(progress, startY, rootY + BURST_Y[burst]));
            int alpha = Mth.clamp((int) (210.0F * (0.55F + progress * 0.45F)), 0, 255);
            graphics.fill(x, y, x + 2, y + 5, alpha << 24 | 0x00FFCA35);
            graphics.fill(x, y + 5, x + 1, y + 9, (alpha / 3) << 24 | 0x00FFED9A);
        }
    }

    private static void cross(GuiGraphics graphics, int x, int y, int radius, int color) {
        graphics.fill(x - radius, y, x + radius + 1, y + 1, color);
        graphics.fill(x, y - radius, x + 1, y + radius + 1, color);
    }

    private static Spark[] createSparks() {
        Spark[] result = new Spark[BURST_X.length * PARTICLES_PER_BURST];
        Random random = new Random(0x5550475241444552L);
        int index = 0;
        for (int burst = 0; burst < BURST_X.length; burst++) {
            for (int particle = 0; particle < PARTICLES_PER_BURST; particle++) {
                double angle = Math.PI * 2.0D * particle / PARTICLES_PER_BURST
                    + (random.nextDouble() - 0.5D) * 0.19D;
                float speed = 24.0F + random.nextFloat() * 28.0F;
                float velocityX = (float) Math.cos(angle) * speed;
                float velocityY = (float) Math.sin(angle) * speed - 5.0F;
                float gravity = 24.0F + random.nextFloat() * 13.0F;
                int lifetime = 920 + random.nextInt(480);
                int rgb = PALETTE[(particle + burst * 2 + random.nextInt(2)) % PALETTE.length] & 0x00FFFFFF;
                int size = particle % 7 == 0 ? 2 : 1;
                boolean star = particle % 6 == 0;
                result[index++] = new Spark(BURST_X[burst], BURST_Y[burst], velocityX, velocityY,
                    gravity, BURST_DELAY[burst], lifetime, rgb, size, star, random.nextFloat() * 6.28F);
            }
        }
        return result;
    }

    private record Spark(int originX, int originY, float velocityX, float velocityY, float gravity,
                         int delayMs, int lifetimeMs, int rgb, int size, boolean star, float twinkle) {}
}
