package dev.konqasasas.beat.domain.endurance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnduranceRecordTest {
    private static final EnduranceRules RULES = new EnduranceRules(35, 62, 88);

    @Test
    void laterProgressCompletesSkippedProgressAndZoneState() {
        EnduranceRecord record = new EnduranceRecord(RULES);

        assertTrue(record.reachProgress(40, 500).changed());
        assertEquals(40, record.maxProgress());
        assertEquals(500, record.maxProgressReachedTick().orElseThrow());
        assertTrue(record.zone2Reached());
        assertFalse(record.zone3Reached());

        assertFalse(record.reachProgress(20, 600).changed());
        assertEquals(500, record.maxProgressReachedTick().orElseThrow());
    }

    @Test
    void goalIsTheMaximumProgressAndFrozenRecordsDoNotAdvance() {
        EnduranceRecord record = new EnduranceRecord(RULES);
        record.reachProgress(88, 1_200);

        assertTrue(record.zone2Reached());
        assertTrue(record.zone3Reached());
        assertTrue(record.goalReached());
        assertEquals(88, record.maxProgress());

        EnduranceRecord frozen = new EnduranceRecord(RULES);
        frozen.reachProgress(30, 100);
        frozen.freeze();
        assertFalse(frozen.reachProgress(40, 200).changed());
        assertEquals(30, frozen.maxProgress());
    }
}
