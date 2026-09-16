package dev.konqasasas.beat.persistence.snapshot;

public record HighResultSnapshot(Integer rank, int points, Long finalPointTick) {
    public HighResultSnapshot {
        requirePositiveRank(rank);
        if (points < 0 || (finalPointTick != null && finalPointTick < 0)) {
            throw new IllegalArgumentException("High Difficulty result values must not be negative");
        }
    }

    private static void requirePositiveRank(Integer rank) {
        if (rank != null && rank <= 0) {
            throw new IllegalArgumentException("rank must be positive when present");
        }
    }
}
