package dev.konqasasas.beat.map.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.map.BlockRegion;
import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.HighCourseMap;
import dev.konqasasas.beat.map.HighDifficultyMapConfig;
import dev.konqasasas.beat.map.MapFixtures;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.TimeAttackMapConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapValidationServiceTest {
    private final MapValidationService validator = new MapValidationService("beat"::equals);

    @Test
    void completeMapsPassWithoutWarnings() {
        ValidationReport report = validator.validateAll(
                MapFixtures.validHigh(),
                MapFixtures.validTimeAttack(),
                MapFixtures.validEndurance(),
                true);

        assertTrue(report.passed());
        assertEquals(0, report.warningCount());
    }

    @Test
    void highValidationFindsMissingCoursesAndSpotSequenceGaps() {
        HighDifficultyMapConfig valid = MapFixtures.validHigh();
        Map<Integer, HighCourseMap> courses = new HashMap<>(valid.courses());
        HighCourseMap first = courses.get(1);
        courses.put(1, new HighCourseMap(
                Map.of(1, MapFixtures.region(100), 3, MapFixtures.region(110)),
                first.goal(), first.start()));
        courses.remove(5);

        ValidationReport report = validator.validateHigh(new HighDifficultyMapConfig(
                HighDifficultyMapConfig.CURRENT_SCHEMA_VERSION,
                courses,
                valid.prepare(),
                valid.end()));

        assertFalse(report.passed());
        assertTrue(hasCode(report, "high.spot.sequence"));
        assertTrue(hasCode(report, "high.course.missing"));
    }

    @Test
    void timeAttackRejectsRestartInsideStartAndMissingSplitNumber() {
        TimeAttackMapConfig valid = MapFixtures.validTimeAttack();
        TimeAttackMapConfig invalid = new TimeAttackMapConfig(
                TimeAttackMapConfig.CURRENT_SCHEMA_VERSION,
                valid.start(),
                Map.of(1, MapFixtures.region(10), 3, MapFixtures.region(20)),
                valid.goal(),
                new MapLocation("beat", 0.5, 65, 0.5, 0, 0),
                valid.end());

        ValidationReport report = validator.validateTimeAttack(invalid);

        assertTrue(hasCode(report, "ta.restart.inside-start"));
        assertTrue(hasCode(report, "ta.split.sequence"));
    }

    @Test
    void enduranceRejectsMissingProgressReferencesAndWrongZoneOrder() {
        EnduranceMapConfig valid = MapFixtures.validEndurance();
        EnduranceMapConfig invalid = new EnduranceMapConfig(
                EnduranceMapConfig.CURRENT_SCHEMA_VERSION,
                Map.of(
                        1, java.util.List.of(MapFixtures.progressPoint(1000)),
                        3, java.util.List.of(MapFixtures.progressPoint(1020)),
                        4, java.util.List.of(MapFixtures.progressPoint(1030))),
                3,
                4,
                2,
                valid.zone2Restart(),
                valid.zone3Restart(),
                valid.start(),
                valid.end(),
                valid.fallY());

        ValidationReport report = validator.validateEndurance(invalid);

        assertTrue(hasCode(report, "endurance.progress.sequence"));
        assertTrue(hasCode(report, "endurance.zone.order"));
        assertTrue(hasCode(report, "endurance.progress.reference"));
        assertTrue(hasCode(report, "endurance.goal.not-last"));
    }

    @Test
    void missingWorldAndOfflineModeAreErrors() {
        TimeAttackMapConfig valid = MapFixtures.validTimeAttack();
        BlockRegion missingWorld = new BlockRegion("missing", 64, 10, 11, 0, 1);
        TimeAttackMapConfig invalidWorld = new TimeAttackMapConfig(
                valid.schemaVersion(),
                valid.start(),
                Map.of(1, missingWorld, 2, MapFixtures.region(20)),
                valid.goal(),
                valid.restart(),
                valid.end());

        ValidationReport worldReport = validator.validateTimeAttack(invalidWorld);
        ValidationReport allReport = validator.validateAll(
                MapFixtures.validHigh(), valid, MapFixtures.validEndurance(), false);

        assertTrue(hasCode(worldReport, "world.missing"));
        assertTrue(hasCode(allReport, "server.offline-mode"));
    }

    @Test
    void overlappingDetectionRegionsAreWarningsAndDoNotBlockStart() {
        TimeAttackMapConfig valid = MapFixtures.validTimeAttack();
        TimeAttackMapConfig overlapping = new TimeAttackMapConfig(
                valid.schemaVersion(),
                valid.start(),
                Map.of(1, valid.start(), 2, MapFixtures.region(20)),
                valid.goal(),
                valid.restart(),
                valid.end());

        ValidationReport report = validator.validateTimeAttack(overlapping);

        assertTrue(report.passed());
        assertTrue(hasCode(report, "region.overlap"));
        assertEquals(1, report.warningCount());
    }

    private static boolean hasCode(ValidationReport report, String code) {
        return report.issues().stream().anyMatch(issue -> issue.code().equals(code));
    }
}
