package dev.konqasasas.beat.domain.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TournamentStateMachineTest {
    @Test
    void normalOperationMustFollowTheConfiguredCompetitionOrder() {
        TournamentStateMachine machine = new TournamentStateMachine();
        TournamentState[] states = TournamentState.values();

        assertEquals(TournamentState.WAITING, machine.currentState());
        for (int index = 1; index < states.length; index++) {
            assertTrue(machine.canTransitionTo(states[index]));
            machine.transitionTo(states[index]);
            assertEquals(states[index], machine.currentState());
        }
        assertFalse(machine.canTransitionTo(TournamentState.WAITING));
    }

    @Test
    void normalOperationCannotSkipAState() {
        TournamentStateMachine machine = new TournamentStateMachine();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> machine.transitionTo(TournamentState.HIGH_PRACTICE));

        assertTrue(exception.getMessage().contains("WAITING -> HIGH_PRACTICE"));
        assertEquals(TournamentState.WAITING, machine.currentState());
    }

    @Test
    void persistedStateCanBeRestoredWithoutReplayingTransitions() {
        TournamentStateMachine machine = new TournamentStateMachine(TournamentState.TA_READY);

        assertEquals(TournamentState.TA_READY, machine.currentState());
        assertTrue(machine.canTransitionTo(TournamentState.TA_COUNTDOWN));
    }
}
