package dev.konqasasas.beat.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnduranceProgressIndexTest {
    @Test
    void findsPointsAcrossAChunkBoundaryWithoutScanningTheWholeMap() {
        var first = new EnduranceProgressPoint("world", 15.9, 20, 0);
        var second = new EnduranceProgressPoint("world", 16.1, 20, 0);
        var far = new EnduranceProgressPoint("world", 80, 20, 0);
        var index = new EnduranceProgressIndex(Map.of(1, List.of(first), 2, List.of(second), 3, List.of(far)));

        assertEquals(2, index.highestContaining(new MapLocation("world", 16, 20, 0, 0, 0), 0.25, 0.5));
        assertEquals(0, index.highestContaining(new MapLocation("world", 16, 21, 0, 0, 0), 0.25, 0.5));
    }

    @Test
    void keepsParallelLocationsAndFindsTheNextConfiguredNumber() {
        var index = new EnduranceProgressIndex(Map.of(
                1, List.of(new EnduranceProgressPoint("world", 0, 0, 0)),
                4, List.of(
                        new EnduranceProgressPoint("world", 1, 0, 0),
                        new EnduranceProgressPoint("world", 2, 0, 0))));

        assertEquals(2, index.inChunk("world", 0, 0).stream().filter(entry -> entry.progress() == 4).count());
        assertEquals(4, index.nextProgress(1));
        assertEquals(null, index.nextProgress(4));
    }
}
