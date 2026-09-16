package dev.konqasasas.beat.domain.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TournamentStateMachine {
    private static final Map<TournamentState, Set<TournamentState>> NORMAL_TRANSITIONS =
            createNormalTransitions();

    private TournamentState currentState;

    public TournamentStateMachine() {
        this(TournamentState.WAITING);
    }

    public TournamentStateMachine(TournamentState restoredState) {
        currentState = Objects.requireNonNull(restoredState, "restoredState");
    }

    public TournamentState currentState() {
        return currentState;
    }

    public boolean canTransitionTo(TournamentState target) {
        Objects.requireNonNull(target, "target");
        return NORMAL_TRANSITIONS.get(currentState).contains(target);
    }

    public void transitionTo(TournamentState target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Illegal tournament state transition: " + currentState + " -> " + target);
        }
        currentState = target;
    }

    private static Map<TournamentState, Set<TournamentState>> createNormalTransitions() {
        EnumMap<TournamentState, Set<TournamentState>> transitions =
                new EnumMap<>(TournamentState.class);
        for (TournamentState state : TournamentState.values()) {
            transitions.put(state, Set.of());
        }
        connect(transitions, TournamentState.WAITING, TournamentState.HIGH_PRACTICE_COUNTDOWN);
        connect(transitions, TournamentState.HIGH_PRACTICE_COUNTDOWN, TournamentState.HIGH_PRACTICE);
        connect(transitions, TournamentState.HIGH_PRACTICE, TournamentState.HIGH_PREPARE);
        connect(transitions, TournamentState.HIGH_PREPARE, TournamentState.HIGH_RUNNING);
        connect(transitions, TournamentState.HIGH_RUNNING, TournamentState.HIGH_FINISHED);
        connect(transitions, TournamentState.HIGH_FINISHED, TournamentState.TA_READY);
        connect(transitions, TournamentState.TA_READY, TournamentState.TA_COUNTDOWN);
        connect(transitions, TournamentState.TA_COUNTDOWN, TournamentState.TA_RUNNING);
        connect(transitions, TournamentState.TA_RUNNING, TournamentState.TA_FINISHED);
        connect(transitions, TournamentState.TA_FINISHED, TournamentState.ENDURANCE_READY);
        connect(transitions, TournamentState.ENDURANCE_READY, TournamentState.ENDURANCE_COUNTDOWN);
        connect(transitions, TournamentState.ENDURANCE_COUNTDOWN, TournamentState.ENDURANCE_RUNNING);
        connect(transitions, TournamentState.ENDURANCE_RUNNING, TournamentState.ENDURANCE_FINISHED);
        connect(transitions, TournamentState.ENDURANCE_FINISHED, TournamentState.OVERALL_READY);
        connect(transitions, TournamentState.OVERALL_READY, TournamentState.OVERALL_CONFIRMED);
        return Map.copyOf(transitions);
    }

    private static void connect(
            Map<TournamentState, Set<TournamentState>> transitions,
            TournamentState from,
            TournamentState to) {
        transitions.put(from, Set.of(to));
    }
}
