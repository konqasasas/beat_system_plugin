package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.high.HighCompetitionSession;
import dev.konqasasas.beat.domain.high.HighDifficultyRules;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HighResultServiceTest {
    @Test
    void finalRankingIsPersistedAndConfirmed() throws Exception {
        UUID winner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        HighCompetitionSession session = new HighCompetitionSession(
                Map.of(winner, "Winner", other, "Other"),
                new HighDifficultyRules(Map.of(1, 1, 2, 1, 3, 1, 4, 1, 5, 1), 50, 100));
        session.activate(winner);
        session.activate(other);
        session.reachGoal(winner, 1, 20);
        session.finish();
        MemoryResults repository = new MemoryResults();

        ResultsSnapshot result = new HighResultService(repository).saveFinal(session);

        assertTrue(result.highConfirmed());
        assertEquals(1, result.players().stream().filter(p -> p.uuid().equals(winner)).findFirst().orElseThrow().high().rank());
        assertEquals(150, result.players().stream().filter(p -> p.uuid().equals(winner)).findFirst().orElseThrow().high().points());
    }

    @Test
    void clearingHighPreservesOtherCompetitionFlags() throws Exception {
        MemoryResults repository = new MemoryResults();
        repository.current = new ResultsSnapshot(
                ResultsSnapshot.CURRENT_SCHEMA_VERSION, true, true, false, true, java.util.List.of());
        HighResultService service = new HighResultService(repository);
        service.clearHigh();
        assertEquals(false, repository.current.highConfirmed());
        assertTrue(repository.current.timeAttackConfirmed());
        assertEquals(false, repository.current.overallConfirmed());
    }

    private static final class MemoryResults implements ResultsRepository {
        private ResultsSnapshot current;
        @Override public Optional<ResultsSnapshot> load() { return Optional.ofNullable(current); }
        @Override public void save(ResultsSnapshot results) { current = results; }
    }
}
