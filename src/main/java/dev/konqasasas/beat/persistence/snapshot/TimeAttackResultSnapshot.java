package dev.konqasasas.beat.persistence.snapshot;

public record TimeAttackResultSnapshot(
        Integer rank,
        Long pbTicks,
        Long pbRecordedTick,
        Long pbRecordSequence,
        Long pbSplit1Ticks,
        Long pbSplit2Ticks) {
    public TimeAttackResultSnapshot {
        if (rank != null && rank <= 0) {
            throw new IllegalArgumentException("rank must be positive when present");
        }
        requireNonNegative(pbTicks);
        requireNonNegative(pbRecordedTick);
        requireNonNegative(pbSplit1Ticks);
        requireNonNegative(pbSplit2Ticks);
        if (pbRecordSequence != null && pbRecordSequence <= 0) {
            throw new IllegalArgumentException("recordSequence must be positive when present");
        }
    }

    private static void requireNonNegative(Long value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("TA result values must not be negative");
        }
    }
}
