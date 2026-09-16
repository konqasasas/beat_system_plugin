package dev.konqasasas.beat.ui;

import dev.konqasasas.beat.map.EnduranceProgressPoint;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public final class DustSphereRenderer {
    public static final double RADIUS = 0.25D;
    public static final int PARTICLES_PER_SPHERE = 200;
    private static final double GOLDEN_ANGLE = Math.PI * (3D - Math.sqrt(5D));

    private DustSphereRenderer() {
    }

    public static void spawn(Player player, EnduranceProgressPoint point, Particle.DustOptions dust) {
        for (int index = 0; index < PARTICLES_PER_SPHERE; index++) {
            double y = 1D - 2D * (index + 0.5D) / PARTICLES_PER_SPHERE;
            double horizontal = Math.sqrt(1D - y * y);
            double angle = index * GOLDEN_ANGLE;
            player.spawnParticle(
                    Particle.DUST,
                    point.x() + RADIUS * Math.cos(angle) * horizontal,
                    displayY(point.y(), y),
                    point.z() + RADIUS * Math.sin(angle) * horizontal,
                    1,
                    0D,
                    0D,
                    0D,
                    0D,
                    dust);
        }
    }

    static double displayY(double pointY, double normalizedSphereY) {
        return pointY + RADIUS + RADIUS * normalizedSphereY;
    }
}
