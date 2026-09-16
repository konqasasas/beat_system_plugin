package dev.konqasasas.beat.domain.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.ta.TimeAttackRecord;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SurvivorSelectorTest {
    @Test
    void disqualifiedSeatsDoNotConsumeTheConfiguredSurvivorCount() {
        List<RankingEntry<TimeAttackRecord>> entries = new ArrayList<>();
        for (int position = 1; position <= 21; position++) {
            Competitor competitor = competitor("Player" + position);
            if (position == 5) {
                competitor.setDisqualified(true);
            }
            TimeAttackRecord record = new TimeAttackRecord();
            record.recordGoal(500 + position, 1_000 + position, position, null, null);
            entries.add(new RankingEntry<>(competitor, record));
        }
        List<RankedEntry<TimeAttackRecord>> ranking = CompetitionRankings.timeAttack(entries);

        Set<UUID> survivors = SurvivorSelector.topEligible(
                ranking,
                20,
                TimeAttackRecord::hasPersonalBest);

        assertEquals(20, survivors.size());
        assertFalse(survivors.contains(competitor("Player5").uuid()));
        assertTrue(survivors.contains(competitor("Player21").uuid()));
    }

    @Test
    void noRecordCompetitorsAreEliminatedEvenWhenSeatsRemain() {
        Competitor recorded = competitor("Recorded");
        TimeAttackRecord record = new TimeAttackRecord();
        record.recordGoal(600, 1_000, 1, null, null);
        Competitor noRecord = competitor("NoRecord");
        List<RankedEntry<TimeAttackRecord>> ranking = CompetitionRankings.timeAttack(List.of(
                new RankingEntry<>(recorded, record),
                new RankingEntry<>(noRecord, new TimeAttackRecord())));

        Set<UUID> survivors = SurvivorSelector.topEligible(
                ranking,
                20,
                TimeAttackRecord::hasPersonalBest);

        assertEquals(Set.of(recorded.uuid()), survivors);
    }

    private static Competitor competitor(String name) {
        return new Competitor(
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),
                name);
    }
}
