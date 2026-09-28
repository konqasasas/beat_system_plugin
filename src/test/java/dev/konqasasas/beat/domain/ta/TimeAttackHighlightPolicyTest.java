package dev.konqasasas.beat.domain.ta;

import static org.junit.jupiter.api.Assertions.*;
import dev.konqasasas.beat.domain.notification.HighlightType;
import org.junit.jupiter.api.Test;

class TimeAttackHighlightPolicyTest {
    @Test void borderUsesOnlyTheNextScheduledCutoff() {
        var crossing = TimeAttackHighlightPolicy.personalBest(1_000L, 21, 15, 20);
        assertTrue(crossing.contains(HighlightType.BORDER));
        var alreadyInside = TimeAttackHighlightPolicy.personalBest(1_000L, 15, 8, 20);
        assertFalse(alreadyInside.contains(HighlightType.BORDER));
        var noFutureCutoff = TimeAttackHighlightPolicy.personalBest(1_000L, 21, 15, null);
        assertFalse(noFutureCutoff.contains(HighlightType.BORDER));
    }

    @Test void firstRecordInsideBorderCountsAsCrossingAndLeaderWinsPriority() {
        var first = TimeAttackHighlightPolicy.personalBest(null, 8, 1, 20);
        assertTrue(first.contains(HighlightType.BORDER));
        assertTrue(first.contains(HighlightType.LEADER_CHANGE));
    }
}
