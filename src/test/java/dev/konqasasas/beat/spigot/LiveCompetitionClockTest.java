package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LiveCompetitionClockTest {
    @Test
    void nextTargetIsTenSecondsBeforeNextEvent() {
        var snapshot = new LiveCompetitionClock.Snapshot("TA_RUNNING", 1_000, 36_000, List.of(12_000L, 18_000L));
        assertEquals(11_800, snapshot.nextTarget());
    }

    @Test
    void nextTargetUsesFinishAfterLastElimination() {
        var snapshot = new LiveCompetitionClock.Snapshot("TA_RUNNING", 20_000, 36_000, List.of(12_000L, 18_000L));
        assertEquals(35_800, snapshot.nextTarget());
    }
}
