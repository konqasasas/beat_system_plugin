package dev.konqasasas.beat.setup;

import java.util.Objects;

public record SelectionPoint(String world, int x, int y, int z) {
    public SelectionPoint {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("world must not be blank");
        }
    }
}
