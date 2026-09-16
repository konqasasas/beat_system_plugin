package dev.konqasasas.beat.domain.ta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.ranking.CompetitionRankings;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ranking.RankingEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeAttackRankingTest {
    @Test
    void rankingUsesTimeThenRecordedTickThenRecordSequence() {
        RankingEntry<TimeAttackRecord> slower = entry("Slower", record(610, 900, 1));
        RankingEntry<TimeAttackRecord> laterTick = entry("LaterTick", record(600, 1_100, 1));
        RankingEntry<TimeAttackRecord> sequenceTwo = entry("SequenceTwo", record(600, 1_000, 2));
        RankingEntry<TimeAttackRecord> sequenceOne = entry("SequenceOne", record(600, 1_000, 1));

        List<RankedEntry<TimeAttackRecord>> ranking = CompetitionRankings.timeAttack(
                List.of(slower, laterTick, sequenceTwo, sequenceOne));

        assertEquals(
                List.of("SequenceOne", "SequenceTwo", "LaterTick", "Slower"),
                ranking.stream().map(entry -> entry.competitor().tournamentName()).toList());
        assertEquals(List.of(1, 2, 3, 4), ranking.stream().map(RankedEntry::rank).toList());
    }

    @Test
    void competitorsWithoutRecordsShareLastPlace() {
        List<RankedEntry<TimeAttackRecord>> ranking = CompetitionRankings.timeAttack(List.of(
                entry("Recorded", record(600, 1_000, 1)),
                entry("NoRecordA", new TimeAttackRecord()),
                entry("NoRecordB", new TimeAttackRecord())));

        assertEquals(List.of(1, 2, 2), ranking.stream().map(RankedEntry::rank).toList());
        assertEquals("Recorded", ranking.getFirst().competitor().tournamentName());
    }

    private static TimeAttackRecord record(long time, long reachedTick, long sequence) {
        TimeAttackRecord record = new TimeAttackRecord();
        record.recordGoal(time, reachedTick, sequence, null, null);
        return record;
    }

    private static RankingEntry<TimeAttackRecord> entry(String name, TimeAttackRecord record) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new RankingEntry<>(new Competitor(uuid, name), record);
    }
}
