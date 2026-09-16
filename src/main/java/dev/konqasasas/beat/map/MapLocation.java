package dev.konqasasas.beat.map;

import java.util.Objects;

public record MapLocation(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch) {
    public MapLocation {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("world must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Location values must be finite");
        }
    }
}
