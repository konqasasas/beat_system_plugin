package dev.konqasasas.beat.persistence.snapshot;

public record EnduranceResultSnapshot(
        Integer rank,
        int maxProgress,
        Long progressReachedTick,
        boolean zone2Reached,
        boolean zone3Reached,
        boolean goalReached) {
    public EnduranceResultSnapshot {
        if (rank != null && rank <= 0) {
            throw new IllegalArgumentException("rank must be positive when present");
        }
        if (maxProgress < 0 || (progressReachedTick != null && progressReachedTick < 0)) {
            throw new IllegalArgumentException("Endurance result values must not be negative");
        }
        if (goalReached && (!zone2Reached || !zone3Reached)) {
            throw new IllegalArgumentException("Goal requires both zone flags");
        }
        if (zone3Reached && !zone2Reached) {
            throw new IllegalArgumentException("Zone 3 requires Zone 2");
        }
    }
}
