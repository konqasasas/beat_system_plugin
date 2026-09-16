package dev.konqasasas.beat.map;

import java.util.Map;
import java.util.Objects;

public record HighDifficultyMapConfig(
        int schemaVersion,
        Map<Integer, HighCourseMap> courses,
        MapLocation prepare,
        MapLocation end) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public HighDifficultyMapConfig {
        requireSchema(schemaVersion);
        Objects.requireNonNull(courses, "courses");
        courses = Map.copyOf(courses);
    }

    public static HighDifficultyMapConfig empty() {
        return new HighDifficultyMapConfig(CURRENT_SCHEMA_VERSION, Map.of(), null, null);
    }

    private static void requireSchema(int schemaVersion) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported High Difficulty map schema: " + schemaVersion);
        }
    }
}
