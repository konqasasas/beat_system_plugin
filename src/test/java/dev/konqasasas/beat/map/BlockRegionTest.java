package dev.konqasasas.beat.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockRegionTest {
    @Test
    void coordinatesAreNormalizedRegardlessOfSelectionDirection() {
        BlockRegion region = new BlockRegion("beat", 64, 10, -2, 8, -4);

        assertEquals(-2, region.minX());
        assertEquals(10, region.maxX());
        assertEquals(-4, region.minZ());
        assertEquals(8, region.maxZ());
    }

    @Test
    void containsUsesMinecraftBlockFloorCoordinates() {
        BlockRegion region = new BlockRegion("beat", 64, -2, 2, -2, 2);

        assertTrue(region.isSteppedOnBy(new MapLocation("beat", -1.2, 65.0, 0.5, 0, 0)));
        assertFalse(region.isSteppedOnBy(new MapLocation("beat", 3.1, 65.0, 0.5, 0, 0)));
        assertFalse(region.isSteppedOnBy(new MapLocation("other", 0, 65, 0, 0, 0)));
    }
}
