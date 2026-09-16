package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.EventStateRepository;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventStateServiceTest {
    @Test
    void transitionIsValidatedAndPersisted() throws Exception {
        MemoryRepository repository = new MemoryRepository(state(TournamentState.WAITING));
        EventStateService service = new EventStateService(repository, repository.state);
        service.transitionTo(TournamentState.HIGH_PRACTICE_COUNTDOWN);
        assertEquals(TournamentState.HIGH_PRACTICE_COUNTDOWN, repository.state.tournamentState());
        assertThrows(IllegalStateException.class, () -> service.transitionTo(TournamentState.TA_RUNNING));
    }

    @Test
    void restartRecoveryReturnsPracticeToWaiting() throws Exception {
        MemoryRepository repository = new MemoryRepository(state(TournamentState.HIGH_PRACTICE));
        EventStateService service = new EventStateService(repository, repository.state);
        assertEquals(TournamentState.WAITING, service.recoverAfterRestart());
        assertEquals(TournamentState.WAITING, repository.state.tournamentState());
    }

    @Test
    void restartRecoveryKeepsCompletedStates() throws Exception {
        MemoryRepository repository = new MemoryRepository(state(TournamentState.HIGH_FINISHED));
        EventStateService service = new EventStateService(repository, repository.state);
        assertEquals(TournamentState.HIGH_FINISHED, service.recoverAfterRestart());
        assertEquals(0, repository.saves);
    }

    @Test
    void highRestartCanReturnDirectlyToRunningPreparation() throws Exception {
        MemoryRepository repository = new MemoryRepository(state(TournamentState.HIGH_RUNNING));
        EventStateService service = new EventStateService(repository, repository.state);
        service.resetHighForRestart(false);
        assertEquals(TournamentState.HIGH_PREPARE, repository.state.tournamentState());
    }

    @Test
    void emergencyResetReturnsRunningTimeAttackToReady() throws Exception {
        MemoryRepository repository = new MemoryRepository(state(TournamentState.TA_RUNNING));
        EventStateService service = new EventStateService(repository, repository.state);

        service.emergencyResetCurrentPhase();

        assertEquals(TournamentState.TA_READY, repository.state.tournamentState());
        assertEquals(1, repository.saves);
    }

    @Test
    void emergencyResetDoesNotPersistWhenStateIsAlreadySafe() throws Exception {
        MemoryRepository repository = new MemoryRepository(state(TournamentState.TA_READY));
        EventStateService service = new EventStateService(repository, repository.state);

        service.emergencyResetCurrentPhase();

        assertEquals(TournamentState.TA_READY, repository.state.tournamentState());
        assertEquals(0, repository.saves);
    }

    private static EventStateSnapshot state(TournamentState state) {
        return new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION, state, WhitelistMode.ADMIN_ONLY, Map.of());
    }

    private static final class MemoryRepository implements EventStateRepository {
        private EventStateSnapshot state;
        private int saves;
        private MemoryRepository(EventStateSnapshot state) { this.state = state; }
        @Override public Optional<EventStateSnapshot> load() { return Optional.of(state); }
        @Override public void save(EventStateSnapshot state) { this.state = state; saves++; }
    }
}
