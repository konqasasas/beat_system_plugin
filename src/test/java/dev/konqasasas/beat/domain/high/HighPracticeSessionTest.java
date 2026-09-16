package dev.konqasasas.beat.domain.high;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.map.MapLocation;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HighPracticeSessionTest {
    private final UUID player = UUID.randomUUID();

    @Test
    void advancesAcrossExactPhaseBoundaries() {
        HighPracticeSession session = new HighPracticeSession(Set.of(player), 2, 3, 2);
        assertEquals(HighPracticeSession.Phase.COUNTDOWN, session.advanceOneTick());
        assertEquals(HighPracticeSession.Phase.PRACTICE, session.advanceOneTick());
        assertEquals(HighPracticeSession.Phase.PRACTICE, session.advanceOneTick());
        assertEquals(HighPracticeSession.Phase.PRACTICE, session.advanceOneTick());
        assertEquals(HighPracticeSession.Phase.PREPARE, session.advanceOneTick());
        assertEquals(HighPracticeSession.Phase.PREPARE, session.advanceOneTick());
        assertEquals(HighPracticeSession.Phase.COMPLETE, session.advanceOneTick());
    }

    @Test
    void checkpointRequiresGroundAndPracticePhase() {
        HighPracticeSession session = new HighPracticeSession(Set.of(player), 1, 5, 1);
        MapLocation location = new MapLocation("beat", 1, 65, 2, 90, 0);
        assertThrows(IllegalStateException.class, () -> session.setCheckpoint(player, location, true));
        session.advanceOneTick();
        assertFalse(session.setCheckpoint(player, location, false));
        assertTrue(session.setCheckpoint(player, location, true));
        assertEquals(location, session.checkpoint(player).orElseThrow());
    }

    @Test
    void enteringPrepareClearsFlightAndCheckpoint() {
        HighPracticeSession session = new HighPracticeSession(Set.of(player), 1, 1, 1);
        session.advanceOneTick();
        assertTrue(session.toggleFlight(player));
        session.setCheckpoint(player, new MapLocation("beat", 1, 2, 3, 0, 0), true);
        session.advanceOneTick();
        assertFalse(session.flightEnabled(player));
        assertTrue(session.checkpoint(player).isEmpty());
    }

    @Test
    void debugSkipMovesPracticeDirectlyToPrepareAndClearsTemporaryState() {
        HighPracticeSession session = new HighPracticeSession(Set.of(player), 1, 100, 20);
        session.advanceOneTick();
        session.toggleFlight(player);
        session.setCheckpoint(player, new MapLocation("beat", 1, 2, 3, 0, 0), true);

        session.skipPractice();

        assertEquals(HighPracticeSession.Phase.PREPARE, session.phase());
        assertFalse(session.flightEnabled(player));
        assertTrue(session.checkpoint(player).isEmpty());
        assertEquals(20, session.remainingTicks());
    }
}
