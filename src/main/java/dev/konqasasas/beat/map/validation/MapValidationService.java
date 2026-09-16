package dev.konqasasas.beat.map.validation;

import dev.konqasasas.beat.map.BlockRegion;
import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import dev.konqasasas.beat.map.HighCourseMap;
import dev.konqasasas.beat.map.HighDifficultyMapConfig;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.TimeAttackMapConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MapValidationService {
    private final WorldRegistry worlds;

    public MapValidationService(WorldRegistry worlds) {
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    public ValidationReport validateAll(
            HighDifficultyMapConfig high,
            TimeAttackMapConfig timeAttack,
            EnduranceMapConfig endurance,
            boolean onlineMode) {
        ValidationReport report = new ValidationReport();
        if (!onlineMode) {
            report.error("server.offline-mode", "online-mode=false のため大会を開始できません");
        }
        report.include(validateHigh(high));
        report.include(validateTimeAttack(timeAttack));
        report.include(validateEndurance(endurance));
        return report;
    }

    public ValidationReport validateHigh(HighDifficultyMapConfig config) {
        Objects.requireNonNull(config, "config");
        ValidationReport report = new ValidationReport();
        List<NamedRegion> regions = new ArrayList<>();
        config.courses().keySet().stream()
                .filter(number -> number < 1 || number > 5)
                .forEach(number -> report.error(
                        "high.course.invalid", "Course番号は1〜5です: " + number));
        for (int courseNumber = 1; courseNumber <= 5; courseNumber++) {
            HighCourseMap course = config.courses().get(courseNumber);
            if (course == null) {
                report.error("high.course.missing", "Course " + courseNumber + " がありません");
                continue;
            }
            if (course.spots().isEmpty()) {
                report.error("high.spot.missing", "Course " + courseNumber + " にSpotがありません");
            }
            validateSequence(
                    course.spots(),
                    "high.spot.sequence",
                    "Course " + courseNumber + " Spot",
                    report);
            requireRegion(course.goal(), "high.goal.missing", "Course " + courseNumber + " Goal", report);
            requireLocation(course.start(), "high.start.missing", "Course " + courseNumber + " Start", report);
            int currentCourse = courseNumber;
            course.spots().forEach((number, region) -> {
                validateWorld(region.world(), "Course " + currentCourse + " Spot " + number, report);
                regions.add(new NamedRegion("Course " + currentCourse + " Spot " + number, region));
            });
            addRegion(course.goal(), "Course " + courseNumber + " Goal", regions, report);
            validateLocationWorld(course.start(), "Course " + courseNumber + " Start", report);
        }
        requireLocation(config.prepare(), "high.prepare.missing", "High Prepare", report);
        requireLocation(config.end(), "high.end.missing", "High End", report);
        validateLocationWorld(config.prepare(), "High Prepare", report);
        validateLocationWorld(config.end(), "High End", report);
        warnOverlaps(regions, report);
        return report;
    }

    public ValidationReport validateTimeAttack(TimeAttackMapConfig config) {
        Objects.requireNonNull(config, "config");
        ValidationReport report = new ValidationReport();
        requireRegion(config.start(), "ta.start.missing", "TA Start", report);
        requireRegion(config.goal(), "ta.goal.missing", "TA Goal", report);
        requireLocation(config.restart(), "ta.restart.missing", "TA Restart", report);
        requireLocation(config.end(), "ta.end.missing", "TA End", report);
        if (config.splits().size() < 2) {
            report.error("ta.split.missing", "TA Split 1 / Split 2 が必要です");
        }
        validateSequence(config.splits(), "ta.split.sequence", "TA Split", report);

        List<NamedRegion> regions = new ArrayList<>();
        addRegion(config.start(), "TA Start", regions, report);
        config.splits().forEach((number, region) -> {
            validateWorld(region.world(), "TA Split " + number, report);
            regions.add(new NamedRegion("TA Split " + number, region));
        });
        addRegion(config.goal(), "TA Goal", regions, report);
        validateLocationWorld(config.restart(), "TA Restart", report);
        validateLocationWorld(config.end(), "TA End", report);
        if (config.start() != null
                && config.restart() != null
                && config.start().isSteppedOnBy(config.restart())) {
            report.error("ta.restart.inside-start", "restartLocation がTA Start範囲内です");
        }
        warnOverlaps(regions, report);
        return report;
    }

    public ValidationReport validateEndurance(EnduranceMapConfig config) {
        Objects.requireNonNull(config, "config");
        ValidationReport report = new ValidationReport();
        if (config.progresses().isEmpty()) {
            report.error("endurance.progress.missing", "Endurance Progressがありません");
        }
        validateSequence(
                config.progresses(),
                "endurance.progress.sequence",
                "Endurance Progress",
                report);
        requireLocation(config.start(), "endurance.start.missing", "Endurance Start", report);
        requireLocation(config.end(), "endurance.end.missing", "Endurance End", report);
        requireLocation(
                config.zone2Restart(),
                "endurance.zone2-restart.missing",
                "Endurance Zone2 Restart",
                report);
        requireLocation(
                config.zone3Restart(),
                "endurance.zone3-restart.missing",
                "Endurance Zone3 Restart",
                report);
        if (config.fallY() == null) {
            report.error("endurance.fall-y.missing", "Endurance fallYがありません");
        }

        validateEnduranceProgressReferences(config, report);
        config.progresses().forEach((number, points) -> {
            if (points.isEmpty()) {
                report.error("endurance.progress.points.missing", "Endurance Progress " + number + " に地点がありません");
            }
            for (int index = 0; index < points.size(); index++) {
                EnduranceProgressPoint point = points.get(index);
                validateWorld(point.world(), "Endurance Progress " + number + " 地点 " + (index + 1), report);
            }
        });
        validateLocationWorld(config.start(), "Endurance Start", report);
        validateLocationWorld(config.end(), "Endurance End", report);
        validateLocationWorld(config.zone2Restart(), "Endurance Zone2 Restart", report);
        validateLocationWorld(config.zone3Restart(), "Endurance Zone3 Restart", report);
        return report;
    }

    private void validateEnduranceProgressReferences(
            EnduranceMapConfig config, ValidationReport report) {
        requireProgress(config.goalProgress(), "endurance.goal.missing", "Goal", config, report);
        requireProgress(config.zone2Progress(), "endurance.zone2.missing", "Zone2", config, report);
        requireProgress(config.zone3Progress(), "endurance.zone3.missing", "Zone3", config, report);
        if (config.goalProgress() != null && !config.progresses().isEmpty()) {
            int maximum = config.progresses().keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
            if (config.goalProgress() != maximum) {
                report.error("endurance.goal.not-last", "Goalは最大Progressである必要があります");
            }
        }
        if (config.zone2Progress() != null
                && config.zone3Progress() != null
                && config.goalProgress() != null
                && !(config.zone2Progress() < config.zone3Progress()
                        && config.zone3Progress() < config.goalProgress())) {
            report.error("endurance.zone.order", "Zone2 < Zone3 < Goal である必要があります");
        }
    }

    private void requireProgress(
            Integer progress,
            String missingCode,
            String label,
            EnduranceMapConfig config,
            ValidationReport report) {
        if (progress == null) {
            report.error(missingCode, label + " Progressが設定されていません");
        } else if (!config.progresses().containsKey(progress)) {
            report.error("endurance.progress.reference", label + "のProgress " + progress + " が存在しません");
        }
    }

    private <T> void validateSequence(
            Map<Integer, T> values,
            String code,
            String label,
            ValidationReport report) {
        if (values.isEmpty()) {
            return;
        }
        values.keySet().stream()
                .filter(number -> number < 1)
                .forEach(number -> report.error(code, label + "番号が不正です: " + number));
        int maximum = values.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
        for (int number = 1; number <= maximum; number++) {
            if (!values.containsKey(number)) {
                report.error(code, label + " " + "%03d".formatted(number) + " がありません");
            }
        }
    }

    private void requireRegion(
            BlockRegion region, String code, String label, ValidationReport report) {
        if (region == null) {
            report.error(code, label + " が設定されていません");
        }
    }

    private void requireLocation(
            MapLocation location, String code, String label, ValidationReport report) {
        if (location == null) {
            report.error(code, label + " が設定されていません");
        }
    }

    private void addRegion(
            BlockRegion region,
            String label,
            List<NamedRegion> regions,
            ValidationReport report) {
        if (region != null) {
            validateWorld(region.world(), label, report);
            regions.add(new NamedRegion(label, region));
        }
    }

    private void validateLocationWorld(
            MapLocation location, String label, ValidationReport report) {
        if (location != null) {
            validateWorld(location.world(), label, report);
        }
    }

    private void validateWorld(String world, String label, ValidationReport report) {
        if (!worlds.worldExists(world)) {
            report.error("world.missing", label + " のWorldが存在しません: " + world);
        }
    }

    private void warnOverlaps(List<NamedRegion> regions, ValidationReport report) {
        for (int left = 0; left < regions.size(); left++) {
            for (int right = left + 1; right < regions.size(); right++) {
                NamedRegion first = regions.get(left);
                NamedRegion second = regions.get(right);
                if (first.region.overlaps(second.region)) {
                    report.warning(
                            "region.overlap",
                            first.name + " と " + second.name + " が重複しています");
                }
            }
        }
    }

    private record NamedRegion(String name, BlockRegion region) {
    }
}
