package dev.konqasasas.beat.domain.high;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record HighDifficultyRules(
        Map<Integer, Integer> courseSpotCounts,
        int spotPoints,
        int goalPoints) {

    public HighDifficultyRules {
        Objects.requireNonNull(courseSpotCounts, "courseSpotCounts");
        if (spotPoints <= 0 || goalPoints <= 0) {
            throw new IllegalArgumentException("Point values must be positive");
        }

        TreeMap<Integer, Integer> normalized = new TreeMap<>(courseSpotCounts);
        if (normalized.size() != 5) {
            throw new IllegalArgumentException("High Difficulty must define exactly five courses");
        }
        for (int course = 1; course <= 5; course++) {
            Integer spotCount = normalized.get(course);
            if (spotCount == null || spotCount < 0) {
                throw new IllegalArgumentException(
                        "Course " + course + " must have a non-negative spot count");
            }
        }
        courseSpotCounts = Map.copyOf(normalized);
    }

    public int spotCount(int course) {
        Integer count = courseSpotCounts.get(course);
        if (count == null) {
            throw new IllegalArgumentException("Unknown course: " + course);
        }
        return count;
    }
}
