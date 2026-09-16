package dev.konqasasas.beat.domain.endurance;

import java.util.Objects;
import java.util.OptionalLong;

public final class EnduranceRecord {
    private final EnduranceRules rules;
    private int maxProgress;
    private Long maxProgressReachedTick;
    private boolean zone2Reached;
    private boolean zone3Reached;
    private boolean goalReached;
    private boolean frozen;

    public EnduranceRecord(EnduranceRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public ProgressUpdate reachProgress(int progress, long competitionTick) {
        if (progress < 1 || progress > rules.goalProgress()) {
            throw new IllegalArgumentException("Unknown progress: " + progress);
        }
        if (competitionTick < 0) {
            throw new IllegalArgumentException("competitionTick must not be negative");
        }
        if (frozen || progress <= maxProgress) {
            return ProgressUpdate.unchanged(maxProgress);
        }

        int previousProgress = maxProgress;
        maxProgress = progress;
        maxProgressReachedTick = competitionTick;
        zone2Reached |= progress >= rules.zone2Progress();
        zone3Reached |= progress >= rules.zone3Progress();
        goalReached |= progress >= rules.goalProgress();
        return new ProgressUpdate(previousProgress, maxProgress);
    }

    public int maxProgress() {
        return maxProgress;
    }

    public OptionalLong maxProgressReachedTick() {
        return maxProgressReachedTick == null
                ? OptionalLong.empty()
                : OptionalLong.of(maxProgressReachedTick);
    }

    public boolean zone2Reached() {
        return zone2Reached;
    }

    public boolean zone3Reached() {
        return zone3Reached;
    }

    public boolean goalReached() {
        return goalReached;
    }

    public void freeze() {
        frozen = true;
    }

    public boolean frozen() {
        return frozen;
    }

    public record ProgressUpdate(int previousProgress, int currentProgress) {
        public static ProgressUpdate unchanged(int progress) {
            return new ProgressUpdate(progress, progress);
        }

        public boolean changed() {
            return previousProgress != currentProgress;
        }
    }
}
