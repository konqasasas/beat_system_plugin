package dev.konqasasas.beat.map;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Objects;

public record EnduranceMapConfig(
        int schemaVersion,
        Map<Integer, List<EnduranceProgressPoint>> progresses,
        Integer goalProgress,
        Integer zone2Progress,
        Integer zone3Progress,
        MapLocation zone2Restart,
        MapLocation zone3Restart,
        MapLocation start,
        MapLocation end,
        Double fallY) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public EnduranceMapConfig {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported Endurance map schema: " + schemaVersion);
        }
        Objects.requireNonNull(progresses, "progresses");
        Map<Integer, List<EnduranceProgressPoint>> copied = new LinkedHashMap<>();
        progresses.forEach((number, points) -> {
            Objects.requireNonNull(number, "progress number");
            Objects.requireNonNull(points, "progress points");
            copied.put(number, List.copyOf(points));
        });
        progresses = Map.copyOf(copied);
        if (fallY != null && !Double.isFinite(fallY)) {
            throw new IllegalArgumentException("fallY must be finite");
        }
    }

    public static EnduranceMapConfig empty() {
        return new EnduranceMapConfig(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
