package dev.konqasasas.beat.domain.high;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HighDifficultyRecordTest {
    private static final HighDifficultyRules RULES = new HighDifficultyRules(
            Map.of(1, 4, 2, 3, 3, 2, 4, 1, 5, 2),
            50,
            100);

    @Test
    void reachingALaterSpotCompletesEveryEarlierSpot() {
        HighDifficultyRecord record = new HighDifficultyRecord(RULES);

        HighDifficultyRecord.ScoreUpdate update = record.reachSpot(1, 3, 120);

        assertTrue(update.changed());
        assertEquals(150, update.delta());
        assertEquals(3, record.highestSpot(1));
        assertEquals(120, record.finalPointTick().orElseThrow());

        assertFalse(record.reachSpot(1, 2, 121).changed());
        assertEquals(150, record.points());
        assertEquals(120, record.finalPointTick().orElseThrow());
    }

    @Test
    void goalCompletesMissingSpotsAndAwardsGoalPoints() {
        HighDifficultyRecord record = new HighDifficultyRecord(RULES);
        record.reachSpot(1, 2, 100);

        HighDifficultyRecord.ScoreUpdate update = record.reachGoal(1, 140);

        assertEquals(200, update.delta());
        assertEquals(300, record.points());
        assertEquals(4, record.highestSpot(1));
        assertTrue(record.goalReached(1));
        assertEquals(140, record.finalPointTick().orElseThrow());
    }

    @Test
    void frozenRecordCannotGainPoints() {
        HighDifficultyRecord record = new HighDifficultyRecord(RULES);
        record.reachSpot(1, 1, 20);
        record.freeze();

        assertFalse(record.reachGoal(1, 30).changed());
        assertEquals(50, record.points());
        assertEquals(20, record.finalPointTick().orElseThrow());
    }
}
