package dev.konqasasas.beat.domain.ta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeAttackRecordTest {
    @Test
    void onlyAFasterGoalReplacesThePersonalBestAndItsSplits() {
        TimeAttackRecord record = new TimeAttackRecord();

        assertTrue(record.recordGoal(640, 1_000, 1, 200L, 420L).updated());
        assertFalse(record.recordGoal(650, 1_100, 2, 190L, 410L).updated());
        assertFalse(record.recordGoal(640, 1_200, 3, 180L, 400L).updated());
        assertTrue(record.recordGoal(620, 1_300, 4, null, 390L).updated());

        assertEquals(620, record.personalBestTicks().orElseThrow());
        assertEquals(1_300, record.personalBestRecordedTick().orElseThrow());
        assertEquals(4, record.personalBestRecordSequence().orElseThrow());
        assertTrue(record.personalBestSplit1Ticks().isEmpty());
        assertEquals(390, record.personalBestSplit2Ticks().orElseThrow());
    }

    @Test
    void frozenRecordKeepsItsPersonalBest() {
        TimeAttackRecord record = new TimeAttackRecord();
        record.recordGoal(640, 1_000, 1, null, null);
        record.freeze();

        assertFalse(record.recordGoal(600, 1_100, 2, null, null).updated());
        assertEquals(640, record.personalBestTicks().orElseThrow());
    }
}
