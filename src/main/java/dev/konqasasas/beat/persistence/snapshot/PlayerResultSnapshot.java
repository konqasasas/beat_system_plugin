package dev.konqasasas.beat.persistence.snapshot;

import java.util.Objects;
import java.util.UUID;

public record PlayerResultSnapshot(
        UUID uuid,
        String tournamentName,
        boolean participatedHigh,
        boolean participatedTa,
        boolean participatedEndurance,
        boolean overallExcluded,
        boolean disqualified,
        HighResultSnapshot high,
        TimeAttackResultSnapshot timeAttack,
        EnduranceResultSnapshot endurance,
        OverallResultSnapshot overall) {
    public PlayerResultSnapshot {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(tournamentName, "tournamentName");
        if (tournamentName.isBlank()) {
            throw new IllegalArgumentException("tournamentName must not be blank");
        }
    }
}
