package dev.konqasasas.beat.application;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.domain.state.TournamentStateMachine;
import dev.konqasasas.beat.persistence.EventStateRepository;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class EventStateService {
    private final EventStateRepository repository;
    private EventStateSnapshot current;

    public EventStateService(EventStateRepository repository, EventStateSnapshot initialState) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.current = Objects.requireNonNull(initialState, "initialState");
    }

    public synchronized EventStateSnapshot current() {
        return current;
    }

    public synchronized Optional<String> tournamentName(UUID uuid) {
        return Optional.ofNullable(current.tournamentNames().get(uuid));
    }

    public synchronized String recordTournamentNameIfAbsent(UUID uuid, String currentName)
            throws PersistenceException {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(currentName, "currentName");
        if (currentName.isBlank()) {
            throw new IllegalArgumentException("currentName must not be blank");
        }
        String existing = current.tournamentNames().get(uuid);
        if (existing != null) {
            return existing;
        }

        Map<UUID, String> names = new LinkedHashMap<>(current.tournamentNames());
        names.put(uuid, currentName);
        replace(new EventStateSnapshot(
                current.schemaVersion(),
                current.tournamentState(),
                current.whitelistMode(),
                names));
        return currentName;
    }

    public synchronized void setWhitelistMode(WhitelistMode mode) throws PersistenceException {
        replace(new EventStateSnapshot(
                current.schemaVersion(),
                current.tournamentState(),
                Objects.requireNonNull(mode, "mode"),
                current.tournamentNames()));
    }

    public synchronized void transitionTo(TournamentState target) throws PersistenceException {
        TournamentStateMachine machine = new TournamentStateMachine(current.tournamentState());
        machine.transitionTo(Objects.requireNonNull(target, "target"));
        replace(new EventStateSnapshot(
                current.schemaVersion(), target, current.whitelistMode(), current.tournamentNames()));
    }

    public synchronized TournamentState recoverAfterRestart() throws PersistenceException {
        TournamentState recovered = switch (current.tournamentState()) {
            case HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE -> TournamentState.WAITING;
            case HIGH_RUNNING -> TournamentState.HIGH_PREPARE;
            case TA_COUNTDOWN, TA_RUNNING -> TournamentState.TA_READY;
            case ENDURANCE_COUNTDOWN, ENDURANCE_RUNNING -> TournamentState.ENDURANCE_READY;
            default -> current.tournamentState();
        };
        if (recovered != current.tournamentState()) {
            replace(new EventStateSnapshot(
                    current.schemaVersion(), recovered, current.whitelistMode(), current.tournamentNames()));
        }
        return recovered;
    }

    public synchronized void resetHighForRestart(boolean repeatPractice) throws PersistenceException {
        TournamentState target = repeatPractice ? TournamentState.WAITING : TournamentState.HIGH_PREPARE;
        replace(new EventStateSnapshot(
                current.schemaVersion(), target, current.whitelistMode(), current.tournamentNames()));
    }

    public synchronized void emergencyResetCurrentPhase() throws PersistenceException {
        TournamentState target = switch (current.tournamentState()) {
            case HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE -> TournamentState.WAITING;
            case HIGH_PREPARE, HIGH_RUNNING -> TournamentState.HIGH_PREPARE;
            case TA_COUNTDOWN, TA_RUNNING -> TournamentState.TA_READY;
            case ENDURANCE_COUNTDOWN, ENDURANCE_RUNNING -> TournamentState.ENDURANCE_READY;
            default -> current.tournamentState();
        };
        if (target != current.tournamentState()) replace(new EventStateSnapshot(
                current.schemaVersion(), target, current.whitelistMode(), current.tournamentNames()));
    }

    /** Resets event-specific state while preserving the configured whitelist mode. */
    public synchronized void resetEvent() throws PersistenceException {
        replace(new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION,
                TournamentState.WAITING,
                current.whitelistMode(),
                Map.of()));
    }

    /** Restores a pre-reset snapshot when a later persistence write fails. */
    synchronized void restore(EventStateSnapshot snapshot) throws PersistenceException {
        replace(Objects.requireNonNull(snapshot, "snapshot"));
    }

    private void replace(EventStateSnapshot candidate) throws PersistenceException {
        repository.save(candidate);
        current = candidate;
    }
}
