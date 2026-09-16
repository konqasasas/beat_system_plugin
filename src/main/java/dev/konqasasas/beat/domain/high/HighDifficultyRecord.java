package dev.konqasasas.beat.domain.high;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

public final class HighDifficultyRecord {
    private final HighDifficultyRules rules;
    private final Map<Integer, CourseProgress> courses = new HashMap<>();
    private int points;
    private Long finalPointTick;
    private boolean frozen;

    public HighDifficultyRecord(HighDifficultyRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
        for (int course = 1; course <= 5; course++) {
            courses.put(course, new CourseProgress());
        }
    }

    public ScoreUpdate reachSpot(int course, int spot, long competitionTick) {
        validateTick(competitionTick);
        int spotCount = rules.spotCount(course);
        if (spot < 1 || spot > spotCount) {
            throw new IllegalArgumentException("Invalid spot " + spot + " for course " + course);
        }

        CourseProgress progress = courses.get(course);
        if (frozen || progress.goalReached || spot <= progress.highestSpot) {
            return ScoreUpdate.unchanged(points);
        }

        int previousPoints = points;
        int completedSpots = spot - progress.highestSpot;
        progress.highestSpot = spot;
        points += completedSpots * rules.spotPoints();
        finalPointTick = competitionTick;
        return new ScoreUpdate(previousPoints, points);
    }

    public ScoreUpdate reachGoal(int course, long competitionTick) {
        validateTick(competitionTick);
        int spotCount = rules.spotCount(course);
        CourseProgress progress = courses.get(course);
        if (frozen || progress.goalReached) {
            return ScoreUpdate.unchanged(points);
        }

        int previousPoints = points;
        int missingSpots = spotCount - progress.highestSpot;
        progress.highestSpot = spotCount;
        progress.goalReached = true;
        points += missingSpots * rules.spotPoints() + rules.goalPoints();
        finalPointTick = competitionTick;
        return new ScoreUpdate(previousPoints, points);
    }

    public int points() {
        return points;
    }

    public OptionalLong finalPointTick() {
        return finalPointTick == null ? OptionalLong.empty() : OptionalLong.of(finalPointTick);
    }

    public int highestSpot(int course) {
        rules.spotCount(course);
        return courses.get(course).highestSpot;
    }

    public boolean goalReached(int course) {
        rules.spotCount(course);
        return courses.get(course).goalReached;
    }

    public boolean allCoursesCleared() {
        return courses.values().stream().allMatch(progress -> progress.goalReached);
    }

    public void freeze() {
        frozen = true;
    }

    public boolean frozen() {
        return frozen;
    }

    private static void validateTick(long competitionTick) {
        if (competitionTick < 0) {
            throw new IllegalArgumentException("competitionTick must not be negative");
        }
    }

    public record ScoreUpdate(int previousPoints, int currentPoints) {
        public static ScoreUpdate unchanged(int points) {
            return new ScoreUpdate(points, points);
        }

        public int delta() {
            return currentPoints - previousPoints;
        }

        public boolean changed() {
            return previousPoints != currentPoints;
        }
    }

    private static final class CourseProgress {
        private int highestSpot;
        private boolean goalReached;
    }
}
