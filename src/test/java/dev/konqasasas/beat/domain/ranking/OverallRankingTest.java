package dev.konqasasas.beat.domain.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import dev.konqasasas.beat.persistence.snapshot.*;

class OverallRankingTest {
    @Test void usesProductThenSortedRanksAndAllowsTrueTies() {
        var a = player("A", 1, 3, 4, false, false, true);
        var b = player("B", 2, 2, 3, false, false, true);
        var c = player("C", 3, 2, 2, false, false, true);
        var ranking = OverallRanking.calculate(List.of(c, b, a));
        assertEquals("A", ranking.getFirst().player().tournamentName());
        assertEquals(java.util.Set.of("B", "C"), ranking.subList(1, 3).stream()
                .map(s -> s.player().tournamentName()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of(1, 2, 2), ranking.stream().map(OverallRanking.Standing::rank).toList());
    }

    @Test void excludesAbsentExcludedAndDisqualifiedPlayers() {
        var valid = player("Valid", 1, 1, 1, false, false, true);
        var absent = player("Absent", 1, 1, 1, false, false, false);
        var excluded = player("Excluded", 1, 1, 1, true, false, true);
        var dq = player("DQ", 1, 1, 1, false, true, true);
        assertEquals(List.of("Valid"), OverallRanking.calculate(List.of(valid, absent, excluded, dq)).stream()
                .map(s -> s.player().tournamentName()).toList());
    }

    private static PlayerResultSnapshot player(String name, int high, int ta, int endurance,
            boolean excluded, boolean dq, boolean participated) {
        return new PlayerResultSnapshot(UUID.nameUUIDFromBytes(name.getBytes()), name,
                participated, participated, participated, excluded, dq,
                new HighResultSnapshot(high, 0, null),
                new TimeAttackResultSnapshot(ta, null, null, null, null, null),
                new EnduranceResultSnapshot(endurance, 0, null, false, false, false), null);
    }
}
