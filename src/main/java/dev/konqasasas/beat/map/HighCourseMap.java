package dev.konqasasas.beat.map;

import java.util.Map;
import java.util.Objects;

public record HighCourseMap(
        Map<Integer, BlockRegion> spots,
        BlockRegion goal,
        MapLocation start) {
    public HighCourseMap {
        Objects.requireNonNull(spots, "spots");
        spots = Map.copyOf(spots);
    }
}
