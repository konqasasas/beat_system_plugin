package dev.konqasasas.beat.domain.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TournamentClockTest {
    @Test
    void clockAdvancesOnlyByExplicitServerTicks() {
        TournamentClock clock = new TournamentClock();
        assertThrows(IllegalStateException.class, clock::advanceOneTick);

        clock.start();
        for (int tick = 0; tick < 247; tick++) {
            clock.advanceOneTick();
        }

        assertEquals(247, clock.elapsedTicks());
        assertEquals("00:12", TickTime.formatMinutesSeconds(clock.elapsedTicks()));
        clock.stop();
        assertFalse(clock.running());
    }

    @Test
    void clockAppliesInclusiveCompetitionDeadline() {
        TournamentClock clock = new TournamentClock();
        long deadline = TickTime.ticks(30, 0);
        clock.start();

        for (long tick = 0; tick < deadline; tick++) {
            clock.advanceOneTick();
        }
        assertTrue(clock.isAt(deadline));
        assertTrue(clock.acceptsEventThrough(deadline));

        clock.advanceOneTick();
        assertFalse(clock.acceptsEventThrough(deadline));
    }
}
