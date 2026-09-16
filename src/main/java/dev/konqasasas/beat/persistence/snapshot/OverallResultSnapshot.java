package dev.konqasasas.beat.persistence.snapshot;

public record OverallResultSnapshot(
        Integer rank,
        long scoreProduct,
        int highRank,
        int taRank,
        int enduranceRank,
        boolean confirmed) {
    public OverallResultSnapshot {
        if (rank != null && rank <= 0) {
            throw new IllegalArgumentException("rank must be positive when present");
        }
        if (scoreProduct <= 0 || highRank <= 0 || taRank <= 0 || enduranceRank <= 0) {
            throw new IllegalArgumentException("Overall ranks and score product must be positive");
        }
    }
}
