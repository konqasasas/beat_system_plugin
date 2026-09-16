package dev.konqasasas.beat.map;

import java.util.Map;
import java.util.Objects;

public record TimeAttackMapConfig(
        int schemaVersion,
        BlockRegion start,
        Map<Integer, BlockRegion> splits,
        BlockRegion goal,
        MapLocation restart,
        MapLocation end) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public TimeAttackMapConfig {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported Time Attack map schema: " + schemaVersion);
        }
        Objects.requireNonNull(splits, "splits");
        splits = Map.copyOf(splits);
    }

    public static TimeAttackMapConfig empty() {
        return new TimeAttackMapConfig(CURRENT_SCHEMA_VERSION, null, Map.of(), null, null, null);
    }
}
