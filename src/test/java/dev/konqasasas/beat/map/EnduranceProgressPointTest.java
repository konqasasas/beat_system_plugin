package dev.konqasasas.beat.map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnduranceProgressPointTest {
    private final EnduranceProgressPoint point = new EnduranceProgressPoint("beat", 10D, 20D, 30D);

    @Test
    void usesCircularHorizontalRadiusAndIndependentVerticalTolerance() {
        assertTrue(point.contains(location(10.25D, 20.5D, 30D), 0.25D, 0.5D));
        assertFalse(point.contains(location(10.25D, 20.5001D, 30D), 0.25D, 0.5D));
        assertFalse(point.contains(location(10.2D, 20D, 30.2D), 0.25D, 0.5D));
        assertFalse(point.contains(new MapLocation("other", 10D, 20D, 30D, 0F, 0F), 0.25D, 0.5D));
    }

    private static MapLocation location(double x, double y, double z) {
        return new MapLocation("beat", x, y, z, 0F, 0F);
    }
}
