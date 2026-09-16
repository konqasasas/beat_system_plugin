package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ta.TimeAttackRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeAttackBorderDisplayTest {
    @Test
    void followsTheNextEliminationBoundaryAndHidesItAfterTheLastCutoff() {
        var rank20 = entry(20, 645);
        var rank10 = entry(10, 590);
        var ranking = List.of(rank10, rank20);

        assertEquals("#20 32.25", TimeAttackBorderDisplay.format(
                0, List.of(24_000, 30_000), List.of(20, 10), ranking));
        assertEquals("#10 29.50", TimeAttackBorderDisplay.format(
                24_000, List.of(24_000, 30_000), List.of(20, 10), ranking));
        assertEquals("--.--", TimeAttackBorderDisplay.format(
                30_000, List.of(24_000, 30_000), List.of(20, 10), ranking));
    }

    @Test
    void showsMissingTimeWhenTheBoundaryRankHasNoRecord() {
        assertEquals("#20 --.--", TimeAttackBorderDisplay.format(
                0, List.of(24_000, 30_000), List.of(20, 10), List.of()));
    }

    private static RankedEntry<TimeAttackRecord> entry(int rank, long ticks) {
        var record = new TimeAttackRecord();
        record.recordGoal(ticks, ticks, rank, null, null);
        return new RankedEntry<>(new Competitor(UUID.randomUUID(), "Player" + rank), record, rank);
    }
}
