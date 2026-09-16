package dev.konqasasas.beat.domain.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TickTimeTest {
    @Test
    void convertsConfiguredTimesToServerTicks() {
        assertEquals(12_000, TickTime.ticks(10, 0));
        assertEquals(24_000, TickTime.ticks(20, 0));
        assertEquals(30_000, TickTime.ticks(25, 0));
        assertEquals(36_000, TickTime.ticks(30, 0));
        assertEquals(247, TickTime.ticks(0, 12) + 7);
    }

    @Test
    void deadlineTickIsAcceptedButTheNextTickIsRejected() {
        long deadline = TickTime.ticks(30, 0);

        assertTrue(TickTime.isAcceptedAtInclusiveDeadline(deadline, deadline));
        assertFalse(TickTime.isAcceptedAtInclusiveDeadline(deadline + 1, deadline));
    }

    @Test
    void formattingUsesTicksRatherThanWallClockTime() {
        assertEquals("12:20", TickTime.formatMinutesSeconds(14_800));
        assertThrows(IllegalArgumentException.class, () -> TickTime.ticks(1, 60));
    }
}
