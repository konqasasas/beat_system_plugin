package dev.konqasasas.beat.domain.endurance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.ranking.CompetitionRankings;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ranking.RankingEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnduranceRankingTest {
    private static final EnduranceRules RULES = new EnduranceRules(35, 62, 88);

    @Test
    void rankingUsesMaximumProgressThenFirstReachedTick() {
        List<RankedEntry<EnduranceRecord>> ranking = CompetitionRankings.endurance(List.of(
                entry("Progress40", record(40, 100)),
                entry("Late50", record(50, 200)),
                entry("Early50A", record(50, 150)),
                entry("Early50B", record(50, 150))));

        assertEquals(List.of(1, 1, 3, 4), ranking.stream().map(RankedEntry::rank).toList());
        assertEquals(
                Set.of("Early50A", "Early50B"),
                ranking.subList(0, 2).stream()
                        .map(entry -> entry.competitor().tournamentName())
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals("Late50", ranking.get(2).competitor().tournamentName());
        assertEquals("Progress40", ranking.get(3).competitor().tournamentName());
    }

    @Test
    void progressZeroCompetitorsAreTied() {
        List<RankedEntry<EnduranceRecord>> ranking = CompetitionRankings.endurance(List.of(
                entry("A", new EnduranceRecord(RULES)),
                entry("B", new EnduranceRecord(RULES))));

        assertEquals(List.of(1, 1), ranking.stream().map(RankedEntry::rank).toList());
    }

    private static EnduranceRecord record(int progress, long tick) {
        EnduranceRecord record = new EnduranceRecord(RULES);
        record.reachProgress(progress, tick);
        return record;
    }

    private static RankingEntry<EnduranceRecord> entry(String name, EnduranceRecord record) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new RankingEntry<>(new Competitor(uuid, name), record);
    }
}
