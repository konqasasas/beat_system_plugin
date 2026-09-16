package dev.konqasasas.beat.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompetitorTest {
    @Test
    void overallEligibilityRequiresAllParticipationsAndNoExclusion() {
        Competitor competitor = competitor("PlayerA");

        competitor.markParticipated(CompetitionKind.HIGH_DIFFICULTY);
        competitor.markParticipated(CompetitionKind.TIME_ATTACK);
        assertFalse(competitor.eligibleForOverall());

        competitor.markParticipated(CompetitionKind.ENDURANCE);
        assertTrue(competitor.eligibleForOverall());

        competitor.setOverallExcluded(true);
        assertFalse(competitor.eligibleForOverall());
        competitor.setOverallExcluded(false);
        competitor.setDisqualified(true);
        assertFalse(competitor.eligibleForOverall());
    }

    @Test
    void tournamentNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Competitor(UUID.randomUUID(), " "));
    }

    private static Competitor competitor(String name) {
        return new Competitor(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }
}
