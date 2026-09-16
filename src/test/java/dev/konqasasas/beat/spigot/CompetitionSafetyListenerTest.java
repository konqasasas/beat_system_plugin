package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.state.TournamentState;
import org.junit.jupiter.api.Test;

class CompetitionSafetyListenerTest {
    @Test void identifiesLivePhasesForGameModeMonitoring(){
        assertTrue(CompetitionSafetyListener.active(TournamentState.HIGH_PRACTICE));
        assertTrue(CompetitionSafetyListener.active(TournamentState.TA_COUNTDOWN));
        assertTrue(CompetitionSafetyListener.active(TournamentState.ENDURANCE_RUNNING));
        assertFalse(CompetitionSafetyListener.active(TournamentState.WAITING));
        assertFalse(CompetitionSafetyListener.active(TournamentState.OVERALL_CONFIRMED));
    }
}
