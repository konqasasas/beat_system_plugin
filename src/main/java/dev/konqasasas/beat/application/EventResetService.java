package dev.konqasasas.beat.application;

import dev.konqasasas.beat.persistence.FileBackupService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Safely backs up and clears all event-specific persistent data. */
public final class EventResetService {
    private final EventStateService states;
    private final ResultsRepository results;
    private final FileBackupService backups;
    private final Path eventStateFile;
    private final Path resultsFile;

    public EventResetService(EventStateService states, ResultsRepository results,
            FileBackupService backups, Path eventStateFile, Path resultsFile) {
        this.states = Objects.requireNonNull(states, "states");
        this.results = Objects.requireNonNull(results, "results");
        this.backups = Objects.requireNonNull(backups, "backups");
        this.eventStateFile = Objects.requireNonNull(eventStateFile, "eventStateFile");
        this.resultsFile = Objects.requireNonNull(resultsFile, "resultsFile");
    }

    public synchronized ResetResult reset(Runnable stopActiveCompetitions) throws PersistenceException {
        Objects.requireNonNull(stopActiveCompetitions, "stopActiveCompetitions");
        EventStateSnapshot previousState = states.current();
        ResultsSnapshot previousResults = results.load().orElse(emptyResults());
        List<Path> createdBackups = backups.backupExisting(
                List.of(eventStateFile, resultsFile), "event-reset");

        stopActiveCompetitions.run();
        states.resetEvent();
        try {
            results.save(emptyResults());
        } catch (PersistenceException resetFailure) {
            rollback(previousState, previousResults, resetFailure);
            throw resetFailure;
        }
        return new ResetResult(createdBackups);
    }

    private void rollback(EventStateSnapshot previousState, ResultsSnapshot previousResults,
            PersistenceException resetFailure) {
        try {
            states.restore(previousState);
        } catch (PersistenceException rollbackFailure) {
            resetFailure.addSuppressed(rollbackFailure);
        }
        try {
            results.save(previousResults);
        } catch (PersistenceException rollbackFailure) {
            resetFailure.addSuppressed(rollbackFailure);
        }
    }

    private static ResultsSnapshot emptyResults() {
        return new ResultsSnapshot(
                ResultsSnapshot.CURRENT_SCHEMA_VERSION, false, false, false, false, List.of());
    }

    public record ResetResult(List<Path> backups) {
        public ResetResult {
            backups = List.copyOf(backups);
        }
    }
}
