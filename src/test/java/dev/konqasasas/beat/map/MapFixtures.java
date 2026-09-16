package dev.konqasasas.beat.map;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapFixtures {
    private MapFixtures() {
    }

    public static BlockRegion region(int x) {
        return new BlockRegion("beat", 64, x, x + 1, 0, 1);
    }

    public static MapLocation location(int x) {
        return new MapLocation("beat", x + 0.5, 65, 0.5, 90, 0);
    }

    public static EnduranceProgressPoint progressPoint(int x) {
        return new EnduranceProgressPoint("beat", x + 0.5, 65, 0.5);
    }

    public static HighDifficultyMapConfig validHigh() {
        Map<Integer, HighCourseMap> courses = new LinkedHashMap<>();
        for (int course = 1; course <= 5; course++) {
            int base = course * 100;
            courses.put(course, new HighCourseMap(
                    Map.of(1, region(base), 2, region(base + 10)),
                    region(base + 20),
                    location(base + 40)));
        }
        return new HighDifficultyMapConfig(
                HighDifficultyMapConfig.CURRENT_SCHEMA_VERSION,
                courses,
                location(700),
                location(710));
    }

    public static TimeAttackMapConfig validTimeAttack() {
        return new TimeAttackMapConfig(
                TimeAttackMapConfig.CURRENT_SCHEMA_VERSION,
                region(0),
                Map.of(1, region(10), 2, region(20)),
                region(30),
                location(40),
                location(50));
    }

    public static EnduranceMapConfig validEndurance() {
        return new EnduranceMapConfig(
                EnduranceMapConfig.CURRENT_SCHEMA_VERSION,
                Map.of(
                        1, List.of(progressPoint(1000)),
                        2, List.of(progressPoint(1010)),
                        3, List.of(progressPoint(1020)),
                        4, List.of(progressPoint(1030))),
                4,
                2,
                3,
                location(1040),
                location(1050),
                location(1060),
                location(1070),
                20.0);
    }
}
