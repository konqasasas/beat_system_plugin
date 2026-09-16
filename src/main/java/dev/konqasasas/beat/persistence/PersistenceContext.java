package dev.konqasasas.beat.persistence;

import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.util.Objects;

public record PersistenceContext(
        EventStateRepository eventStates,
        ResultsRepository results,
        FileBackupService backups,
        EventStateSnapshot eventState,
        ResultsSnapshot resultState) {
    public PersistenceContext {
        Objects.requireNonNull(eventStates, "eventStates");
        Objects.requireNonNull(results, "results");
        Objects.requireNonNull(backups, "backups");
        Objects.requireNonNull(eventState, "eventState");
        Objects.requireNonNull(resultState, "resultState");
    }
}
