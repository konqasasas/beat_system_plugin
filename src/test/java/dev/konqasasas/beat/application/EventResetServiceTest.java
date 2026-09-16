package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.FileBackupService;
import dev.konqasasas.beat.persistence.JsonEventStateRepository;
import dev.konqasasas.beat.persistence.JsonResultsRepository;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EventResetServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void backsUpAndResetsEventDataWhilePreservingWhitelistMode() throws Exception {
        Path data = temporaryDirectory.resolve("data");
        Path eventFile = data.resolve("event-state.json");
        Path resultsFile = data.resolve("results.json");
        var eventRepository = new JsonEventStateRepository(eventFile);
        var resultsRepository = new JsonResultsRepository(resultsFile);
        UUID player = UUID.randomUUID();
        EventStateSnapshot event = new EventStateSnapshot(1, TournamentState.ENDURANCE_FINISHED,
                WhitelistMode.ALL, Map.of(player, "TournamentName"));
        ResultsSnapshot results = new ResultsSnapshot(1, true, true, true, true, List.of());
        eventRepository.save(event);
        resultsRepository.save(results);
        EventStateService states = new EventStateService(eventRepository, event);
        AtomicBoolean stopped = new AtomicBoolean();
        EventResetService service = new EventResetService(states, resultsRepository,
                new FileBackupService(temporaryDirectory.resolve("backups")), eventFile, resultsFile);

        EventResetService.ResetResult outcome = service.reset(() -> stopped.set(true));

        assertTrue(stopped.get());
        assertEquals(2, outcome.backups().size());
        assertTrue(outcome.backups().stream().allMatch(Files::isRegularFile));
        assertEquals(TournamentState.WAITING, states.current().tournamentState());
        assertEquals(WhitelistMode.ALL, states.current().whitelistMode());
        assertTrue(states.current().tournamentNames().isEmpty());
        ResultsSnapshot resetResults = resultsRepository.load().orElseThrow();
        assertFalse(resetResults.highConfirmed());
        assertFalse(resetResults.timeAttackConfirmed());
        assertFalse(resetResults.enduranceConfirmed());
        assertFalse(resetResults.overallConfirmed());
        assertTrue(resetResults.players().isEmpty());
    }

    @Test
    void restoresEventStateIfResultsCannotBeReset() throws Exception {
        UUID player = UUID.randomUUID();
        EventStateSnapshot event = new EventStateSnapshot(1, TournamentState.HIGH_FINISHED,
                WhitelistMode.ADMIN_ONLY, Map.of(player, "Player"));
        var eventRepository = new MemoryEventRepository(event);
        EventStateService states = new EventStateService(eventRepository, event);
        ResultsSnapshot results = new ResultsSnapshot(1, true, false, false, false, List.of());
        ResultsRepository failingResults = new ResultsRepository() {
            @Override public Optional<ResultsSnapshot> load() { return Optional.of(results); }
            @Override public void save(ResultsSnapshot ignored) throws PersistenceException {
                throw new PersistenceException("simulated failure");
            }
        };
        EventResetService service = new EventResetService(states, failingResults,
                new FileBackupService(temporaryDirectory.resolve("backups")),
                temporaryDirectory.resolve("event-state.json"), temporaryDirectory.resolve("results.json"));

        assertThrows(PersistenceException.class, () -> service.reset(() -> {}));

        assertEquals(event, states.current());
    }

    private static final class MemoryEventRepository
            implements dev.konqasasas.beat.persistence.EventStateRepository {
        private EventStateSnapshot state;
        private MemoryEventRepository(EventStateSnapshot state) { this.state = state; }
        @Override public Optional<EventStateSnapshot> load() { return Optional.of(state); }
        @Override public void save(EventStateSnapshot state) { this.state = state; }
    }
}
