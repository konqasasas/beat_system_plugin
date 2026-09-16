package dev.konqasasas.beat.domain.high;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.ranking.CompetitionRankings;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ranking.RankingEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HighDifficultyRankingTest {
    private static final HighDifficultyRules RULES = new HighDifficultyRules(
            Map.of(1, 4, 2, 1, 3, 1, 4, 1, 5, 1),
            50,
            100);

    @Test
    void samePointsUseFirstReachedTickAndThenShareRank() {
        RankingEntry<HighDifficultyRecord> late = entry("Late", spotRecord(50));
        RankingEntry<HighDifficultyRecord> earlyA = entry("EarlyA", spotRecord(30));
        RankingEntry<HighDifficultyRecord> earlyB = entry("EarlyB", spotRecord(30));

        List<RankedEntry<HighDifficultyRecord>> ranking = CompetitionRankings.highDifficulty(
                List.of(late, earlyA, earlyB));

        assertEquals(List.of(1, 1, 3), ranking.stream().map(RankedEntry::rank).toList());
        assertEquals(30, ranking.getFirst().record().finalPointTick().orElseThrow());
        assertEquals(30, ranking.get(1).record().finalPointTick().orElseThrow());
        assertEquals("Late", ranking.get(2).competitor().tournamentName());
    }

    @Test
    void zeroPointCompetitorsAreTied() {
        List<RankedEntry<HighDifficultyRecord>> ranking = CompetitionRankings.highDifficulty(List.of(
                entry("A", new HighDifficultyRecord(RULES)),
                entry("B", new HighDifficultyRecord(RULES))));

        assertEquals(List.of(1, 1), ranking.stream().map(RankedEntry::rank).toList());
    }

    @Test
    void disqualificationKeepsTheOriginalRankingSeat() {
        RankingEntry<HighDifficultyRecord> first = entry("First", goalRecord());
        RankingEntry<HighDifficultyRecord> disqualified = entry("Disqualified", spotRecord(20, 4));
        RankingEntry<HighDifficultyRecord> third = entry("Third", spotRecord(10, 3));
        disqualified.competitor().setDisqualified(true);

        List<RankedEntry<HighDifficultyRecord>> ranking = CompetitionRankings.highDifficulty(
                List.of(first, disqualified, third));

        assertEquals(List.of(1, 2, 3), ranking.stream().map(RankedEntry::rank).toList());
        assertTrue(ranking.get(1).disqualified());
        assertEquals("Third", ranking.get(2).competitor().tournamentName());
        assertEquals(3, ranking.get(2).rank());
    }

    private static HighDifficultyRecord spotRecord(long tick) {
        return spotRecord(tick, 1);
    }

    private static HighDifficultyRecord spotRecord(long tick, int spot) {
        HighDifficultyRecord record = new HighDifficultyRecord(RULES);
        record.reachSpot(1, spot, tick);
        return record;
    }

    private static HighDifficultyRecord goalRecord() {
        HighDifficultyRecord record = new HighDifficultyRecord(RULES);
        record.reachGoal(1, 10);
        return record;
    }

    private static RankingEntry<HighDifficultyRecord> entry(
            String name, HighDifficultyRecord record) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new RankingEntry<>(new Competitor(uuid, name), record);
    }
}
