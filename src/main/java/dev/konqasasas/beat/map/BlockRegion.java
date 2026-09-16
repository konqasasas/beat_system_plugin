package dev.konqasasas.beat.map;

import java.util.Objects;

public record BlockRegion(
        String world,
        int y,
        int minX,
        int maxX,
        int minZ,
        int maxZ) {
    public BlockRegion {
        world = requireWorld(world);
        int normalizedMinX = Math.min(minX, maxX);
        int normalizedMaxX = Math.max(minX, maxX);
        int normalizedMinZ = Math.min(minZ, maxZ);
        int normalizedMaxZ = Math.max(minZ, maxZ);
        minX = normalizedMinX;
        maxX = normalizedMaxX;
        minZ = normalizedMinZ;
        maxZ = normalizedMaxZ;
    }

    /** Tests the block immediately below a player's feet against this region. */
    public boolean isSteppedOnBy(MapLocation location) {
        Objects.requireNonNull(location, "location");
        int blockX = floorToBlock(location.x());
        int blockY = floorToBlock(location.y() - 0.01D);
        int blockZ = floorToBlock(location.z());
        return world.equals(location.world())
                && y == blockY
                && blockX >= minX
                && blockX <= maxX
                && blockZ >= minZ
                && blockZ <= maxZ;
    }

    public double centerX() { return (minX + maxX + 1D) / 2D; }
    public double triggerY() { return y + 1.1D; }
    public double centerZ() { return (minZ + maxZ + 1D) / 2D; }

    public boolean overlaps(BlockRegion other) {
        Objects.requireNonNull(other, "other");
        return world.equals(other.world)
                && y == other.y
                && minX <= other.maxX
                && maxX >= other.minX
                && minZ <= other.maxZ
                && maxZ >= other.minZ;
    }

    private static int floorToBlock(double coordinate) {
        return (int) Math.floor(coordinate);
    }

    private static String requireWorld(String world) {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("world must not be blank");
        }
        return world;
    }
}
