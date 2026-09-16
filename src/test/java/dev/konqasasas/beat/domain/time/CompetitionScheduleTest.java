package dev.konqasasas.beat.domain.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionScheduleTest {
    @Test
    void taAndEnduranceBoundariesRemainExactServerTicks() {
        long twentyMinutes = TickTime.ticks(20, 0);
        long twentyFiveMinutes = TickTime.ticks(25, 0);
        long thirtyMinutes = TickTime.ticks(30, 0);
        CompetitionSchedule schedule = new CompetitionSchedule(
                List.of(twentyMinutes, twentyFiveMinutes),
                thirtyMinutes);

        assertTrue(schedule.isEliminationTick(twentyMinutes));
        assertTrue(schedule.isEliminationTick(twentyFiveMinutes));
        assertFalse(schedule.isEliminationTick(twentyMinutes + 1));
        assertTrue(schedule.isFinishTick(thirtyMinutes));

        assertTrue(schedule.acceptsEvents(thirtyMinutes));
        assertFalse(schedule.acceptsEvents(thirtyMinutes + 1));
        assertEquals(twentyFiveMinutes, schedule.nextBoundaryAfter(twentyMinutes).orElseThrow());
        assertEquals(thirtyMinutes, schedule.nextBoundaryAfter(twentyFiveMinutes).orElseThrow());
        assertTrue(schedule.nextBoundaryAfter(thirtyMinutes).isEmpty());
    }

    @Test
    void invalidOrUnorderedBoundariesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CompetitionSchedule(
                List.of(TickTime.ticks(25, 0), TickTime.ticks(20, 0)),
                TickTime.ticks(30, 0)));
        assertThrows(IllegalArgumentException.class, () -> new CompetitionSchedule(
                List.of(TickTime.ticks(30, 0)),
                TickTime.ticks(30, 0)));
    }
}
