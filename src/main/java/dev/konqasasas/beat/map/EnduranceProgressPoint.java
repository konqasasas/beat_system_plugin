package dev.konqasasas.beat.map;

import java.util.Objects;

public record EnduranceProgressPoint(String world, double x, double y, double z) {
    public EnduranceProgressPoint {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("world must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Progress point coordinates must be finite");
        }
    }

    public boolean contains(MapLocation location, double horizontalRadius, double verticalTolerance) {
        Objects.requireNonNull(location, "location");
        if (!Double.isFinite(horizontalRadius) || horizontalRadius < 0D
                || !Double.isFinite(verticalTolerance) || verticalTolerance < 0D) {
            throw new IllegalArgumentException("Progress point tolerances must be finite and non-negative");
        }
        double dx = location.x() - x;
        double dz = location.z() - z;
        return world.equals(location.world())
                && dx * dx + dz * dz <= horizontalRadius * horizontalRadius
                && Math.abs(location.y() - y) <= verticalTolerance;
    }
}
