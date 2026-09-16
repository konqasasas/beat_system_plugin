package dev.konqasasas.beat.domain.high;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HighCompetitionSessionTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final HighCompetitionSession session = new HighCompetitionSession(
            Map.of(first, "First", second, "Second"),
            new HighDifficultyRules(Map.of(1, 2, 2, 1, 3, 1, 4, 1, 5, 1), 50, 100));

    @Test
    void skippedSpotAndGoalAreCompletedFromHighestProgress() {
        session.activate(first);
        assertEquals(100, session.reachSpot(first, 1, 2, 10).score().delta());
        assertEquals(100, session.reachGoal(first, 1, 20).score().delta());
        assertEquals(200, session.record(first).points());
    }

    @Test
    void courseGoalUpdatesCheckpointAndKeepsPlayerAliveAtElimination() {
        session.activate(first);
        session.activate(second);
        session.reachGoal(first, 1, 20);
        assertEquals(2, session.currentCourse(first));
        var eliminated = session.eliminateBelowCourse(2);
        assertEquals(second, eliminated.getFirst().playerId());
        assertTrue(session.active(first));
        assertTrue(session.eliminated(second));
    }

    @Test
    void reachingEarlierGoalNeverMovesCheckpointBackwards() {
        session.activate(first);
        session.reachGoal(first, 3, 10);
        assertEquals(4, session.currentCourse(first));
        session.reachGoal(first, 1, 20);
        assertEquals(4, session.currentCourse(first));
    }

    @Test
    void frozenPlayerCannotScoreAndAllFiveGoalsAreRequiredForAllClear() {
        session.activate(first);
        session.eliminateBelowCourse(2);
        assertFalse(session.active(first));
        session.activate(second);
        assertFalse(session.reachGoal(second, 5, 10).allClear());
        session.reachGoal(second, 1, 20);
        session.reachGoal(second, 2, 30);
        session.reachGoal(second, 3, 40);
        assertTrue(session.reachGoal(second, 4, 50).allClear());
    }
}
